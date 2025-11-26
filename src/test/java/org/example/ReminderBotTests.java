package org.example;

import org.example.entity.*;
import org.example.logic.BotLogic;
import org.example.scheduler.ReminderScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReminderBotTests — набор unit-тестов для проверки логики бота и поведения
 * моковых сервисов (MockNoteService, MockReminderService, MockUserService).
 * <p>
 * Тесты покрывают следующие сценарии:
 * </p>
 * <ul>
 * <li>Регистрация по логину;</li>
 * <li>Создание, редактирование, удаление и просмотр напоминаний;</li>
 * <li>Проверка отправки напоминаний планировщиком;</li>
 * <li>Валидация ввода даты/времени;</li>
 * <li>Проверка доступа к функциям без логина;</li>
 * <li>Конвертация заметки в напоминание.</li>
 * </ul>
 *
 * <p>Тестовый класс содержит вложенные мок-классы, имитирующие поведение
 * реальных сервисов в памяти и используемые для изоляции тестируемой логики.</p>
 */
public class ReminderBotTests {

    private BotLogic bot;
    private MockNoteService mockNoteService;
    private MockReminderService mockReminderService;
    private MockUserService mockUserService;
    private DummyNotificationSender dummyNotificationSender;
    private ReminderScheduler scheduler;

    /**
     * Подготавливает окружение для тестов: создаёт экземпляры {@code BotLogic} и
     * мок-сервисов, после чего передаёт их в конструктор.
     * <p>Аннотирован как {@code @BeforeEach} — выполняется перед каждым тестом.</p>
     */
    @BeforeEach
    public void setUp() {
        mockNoteService = new MockNoteService();
        mockReminderService = new MockReminderService();
        mockUserService = new MockUserService();
        bot = new BotLogic(mockNoteService, mockReminderService, mockUserService);
        dummyNotificationSender = new DummyNotificationSender();
        scheduler = new ReminderScheduler(mockReminderService, mockUserService, dummyNotificationSender);
    }

    /**
     * Тест: успешная регистрация по логину.
     */
    @Test
    public void loginFlowSuccess() {
        long userId = 100L;
        String login = "test_user";
        Platform platform = Platform.TELEGRAM;


        assertEquals("Введите ваш логин.", bot.handleCommand(userId, "/start",platform));
        assertEquals("Добро пожаловать, test_user! Вы можете использовать бота.", bot.handleCommand(userId, login,platform));

        assertNotNull(mockUserService.getUserByLogin(login));
    }

    /**
     * Тест: создание напоминания.
     * Проверяет, что напоминание сохраняется корректно и будет отправлено на все платформы,
     * если у пользователя привязаны оба ID (Telegram и Discord).
     * Проверяет, что планировщик найдёт просроченное напоминание и получит пользователя с двумя ID.
     */
    @Test
    public void addReminderFlow_SendsToBothPlatformsViaScheduler() throws SQLException {
        long userIdTelegram = 101L;
        long userIdDiscord = 202L;
        Platform tgPlatform = Platform.TELEGRAM;
        Platform dsPlatform = Platform.DISCORD;
        String login = "reminder_user_both_platforms_dummy";
        String reminderText = "Подготовить отчёт для теста с заглушкой";
        String timeInput = "30.10.2026 14:00";

        bot.handleCommand(userIdTelegram, "/start", tgPlatform);
        bot.handleCommand(userIdTelegram, login, tgPlatform);


        bot.handleCommand(userIdDiscord, "/start", dsPlatform);
        bot.handleCommand(userIdDiscord, login, dsPlatform);

        User savedUser = mockUserService.getUserByLogin(login);
        assertNotNull(savedUser, "Пользователь должен быть создан/обновлён");
        assertEquals(userIdTelegram, savedUser.getTelegramId(), "Telegram ID должен совпадать");
        assertEquals(userIdDiscord, savedUser.getDiscordId(), "Discord ID должен совпадать");

        assertEquals("Отправьте текст напоминания.", bot.handleCommand(userIdTelegram, "/new_reminder", tgPlatform));
        assertEquals("Укажите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ (например: 30.10.2025 14:00)", bot.handleCommand(userIdTelegram, reminderText, tgPlatform));
        assertEquals("Напоминание сохранено!\nВы получите уведомление 30 октября в 14:00.", bot.handleCommand(userIdTelegram, timeInput, tgPlatform));

        List<Reminder> userReminders = mockReminderService.getUserReminders(login);
        assertEquals(1, userReminders.size(), "Должно быть одно напоминание для пользователя");
        assertEquals(reminderText, userReminders.get(0).getText(), "Текст напоминания должен совпадать");

        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        String pastReminderText = "Прошлое напоминание для проверки с заглушкой";
        mockReminderService.addReminder(login, pastReminderText, pastTime);

        List<Reminder> dueReminders = mockReminderService.getDueReminders(LocalDateTime.now());
        assertTrue(dueReminders.stream().anyMatch(r -> r.getText().equals(pastReminderText)), "Прошлое напоминание должно быть в списке просроченных");

        scheduler.checkAndSendReminders();

        User userForCheck = mockUserService.getUserByLogin(login);
        assertNotNull(userForCheck, "Пользователь должен существовать для проверки");
        assertNotNull(userForCheck.getTelegramId(), "Telegram ID должен существовать для отправки");
        assertNotNull(userForCheck.getDiscordId(), "Discord ID должен существовать для отправки");

    }
    /**
     * Тест: отображение списка напоминаний пользователя.
     */
    @Test
    public void showUserReminders() {
        String login = "show_user";
        long userId = 102L;
        Platform platform = Platform.TELEGRAM;


        bot.handleCommand(userId, "/start",platform);
        bot.handleCommand(userId, login,platform);

        mockUserService.registerUser(login, userId, null, null);
        mockReminderService.addReminder(login, "Задание", LocalDateTime.now().plusDays(1));

        String response = bot.handleCommand(userId, "/my_reminders",platform);
        assertTrue(response.contains("Задание"));
    }

    /**
     * Тест: удаление напоминания.
     */
    @Test
    public void deleteReminderFlow() {
        String login = "delete_user";
        long userId = 103L;


        mockUserService.registerUser(login, userId, null, null);
        mockReminderService.addReminder(login, "Удалить это", LocalDateTime.now().plusHours(1));

        List<org.example.entity.Reminder> before = mockReminderService.getUserReminders(login);
        assertEquals(1, before.size());

        mockReminderService.deleteReminder(before.getFirst().getId());

        List<org.example.entity.Reminder> after = mockReminderService.getUserReminders(login);
        assertEquals(0, after.size());
    }

    /**
     * Тест: планировщик находит просроченные напоминания.
     */
    @Test
    public void schedulerFindsDueReminders() {
        String login = "scheduler_user";
        long userId = 104L;


        mockUserService.registerUser(login, userId, null, null);
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        mockReminderService.addReminder(login, "Просроченное напоминание", pastTime);

        List<org.example.entity.Reminder> due = mockReminderService.getDueReminders(LocalDateTime.now());
        assertEquals(1, due.size());
        assertEquals("Просроченное напоминание", due.getFirst().getText());
    }

    /**
     * Тест: ошибка при вводе неверного формата даты.
     */
    @Test
    public void reminderWithInvalidTimeReturnsError() {
        long userId = 105L;
        String login = "invalid_time_user";
        Platform platform = Platform.TELEGRAM;


        bot.handleCommand(userId, "/start",platform);
        bot.handleCommand(userId, login,platform);

        bot.handleCommand(userId, "/new_reminder",platform);
        bot.handleCommand(userId, "Напоминание",platform);
        String response = bot.handleCommand(userId, "32.13.2025 25:70",platform);
        assertTrue(response.contains("Неверный формат"));
    }

    /**
     * Тест: пользователь без логина не может получить доступ к напоминаниям.
     */
    @Test
    public void notLoggedInUserCannotAccessReminders() {
        long userId = 106L;
        Platform platform = Platform.TELEGRAM;


        String response = bot.handleCommand(userId, "/my_reminders",platform);
        assertTrue(response.contains("Вы не авторизованы"));
    }

    /**
     * Тест: сценарий конвертации заметки в напоминание.
     */
    @Test
    public void convertNoteToReminderFlow() {
        String login = "convert_user";
        long userId = 107L;
        String noteText = "Купить молоко #покупки";
        Platform platform = Platform.TELEGRAM;


        bot.handleCommand(userId, "/start",platform);
        bot.handleCommand(userId, login,platform);

        mockNoteService.addNote(String.valueOf(userId), noteText);

        List<String> notes = mockNoteService.getAllNotes(String.valueOf(userId));
        assertEquals(1, notes.size());
        assertEquals(noteText, notes.getFirst());
    }
    /**
     * Тест: добавление заметки с тегом → тег извлекается и сохраняется.
     */
    @Test
    public void addNoteWithTagsExtractsTags() {
        long userId = 10L;
        Platform platform = Platform.TELEGRAM;
        String login = "convert_user";

        bot.handleCommand(userId, "/start",platform);
        bot.handleCommand(userId, login,platform);
        bot.handleCommand(userId, "Новая заметка",platform);
        bot.handleCommand(userId, "Купить хлеб #продукты #список",platform);

        List<String> tags = mockNoteService.getTagsForNote(mockNoteService.getNoteIdByIndex(login, 1));
        Assertions.assertEquals(2, tags.size());
        Assertions.assertTrue(tags.contains("#продукты"));
        Assertions.assertTrue(tags.contains("#список"));
    }

    /**
     * Тест: фильтрация заметок по тегу.
     */
    @Test
    public void filterNotesByTag() {
        long uid = 11L;
        Platform platform = Platform.TELEGRAM;
        String login = "convert_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        mockNoteService.addNote(String.valueOf(uid), "Подготовить отчёт #работа");
        mockNoteService.addNote(String.valueOf(uid), "Купить молоко #личное");
        mockNoteService.addNote(String.valueOf(uid), "Идея для стартапа #идея #работа");

        List<String> workNotes = mockNoteService.getNotesByTag(String.valueOf(uid), "#работа");
        Assertions.assertEquals(2, workNotes.size());
        Assertions.assertTrue(workNotes.contains("Подготовить отчёт #работа"));
        Assertions.assertTrue(workNotes.contains("Идея для стартапа #идея #работа"));
    }

    /**
     * Тест: получение списка всех тегов с количеством.
     */
    @Test
    public void getAllUserTagsWithCounts(){
        long uid = 12L;
        Platform platform = Platform.TELEGRAM;
        String login = "convert_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        mockNoteService.addNote(String.valueOf(uid), "Заметка 1 #тег");
        mockNoteService.addNote(String.valueOf(uid), "Заметка 2 #тег");
        mockNoteService.addNote(String.valueOf(uid), "Заметка 3 #другой");

        List<String> tagList = mockNoteService.getAllUserTagsWithCounts(String.valueOf(uid));
        Assertions.assertEquals(2, tagList.size());
        Assertions.assertTrue(tagList.contains("#тег — 2 заметки"));
        Assertions.assertTrue(tagList.contains("#другой — 1 заметка"));
    }

    /**
     * Тест: обновление заметки → теги перезаписываются.
     */
    @Test
    public void updateNoteReplacesTags() {
        long uid = 13L;

        mockNoteService.addNote(String.valueOf(uid), "Старый текст #старый");
        int noteId = mockNoteService.getNoteIdByIndex(String.valueOf(uid), 1);

        mockNoteService.updateNote(String.valueOf(uid), noteId, "Новый текст #новый");

        List<String> tags = mockNoteService.getTagsForNote(noteId);
        Assertions.assertEquals(1, tags.size());
        Assertions.assertEquals("#новый", tags.getFirst());
        Assertions.assertFalse(tags.contains("#старый"));
    }

    /**
     * Тест: удаление всех тегов (пустой список).
     */
    @Test
    public void updateNoteWithNoTagsClearsTags()  {
        long uid = 14L;
        mockNoteService.addNote(String.valueOf(uid), "Текст с тегом #тег");
        int noteId = mockNoteService.getNoteIdByIndex(String.valueOf(uid), 1);

        mockNoteService.updateNote(String.valueOf(uid), noteId, "Текст без тегов");

        List<String> tags = mockNoteService.getTagsForNote(noteId);
        Assertions.assertTrue(tags.isEmpty());
    }
    
    /**
     * Тест: сценарий создания заметки — запрос текста, сохранение, проверка содержимого хранилища.
     */
    @Test
    public void createNoteFlow() throws SQLException {
        long uid = 2L;
        Platform platform = Platform.TELEGRAM;
        String login = "not_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        Assertions.assertEquals("Отправьте текст заметки.", bot.handleCommand(uid, "Новая заметка",platform));
        Assertions.assertEquals("Заметка сохранена!", bot.handleCommand(uid, "Текст заметки",platform));
        List<String> notes = mockNoteService.getAllNotes(login);
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals("Текст заметки", notes.get(0));
    }
    /**
     * Тест: попытка редактировать несуществующую заметку должна вернуть сообщение об ошибке.
     */
    @Test
    public void editNonexistentNoteShowsError() throws SQLException {
        long uid = 3L;
        Platform platform = Platform.TELEGRAM;
        String login = "edit_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        bot.handleCommand(uid, "Изменить заметку",platform);
        String resp = bot.handleCommand(uid, "1",platform);
        Assertions.assertEquals("Неизвестная команда. Используйте кнопки.", resp);
    }
    /**
     * Тест: отмена удаления оставляет заметку в хранилище.
     */
    @Test
    public void deleteCancelKeepsNote() throws SQLException {
        long uid = 4L;
        Platform platform = Platform.TELEGRAM;
        String login = "delete_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        mockNoteService.addNote(login, "не удалять");
        bot.handleCommand(uid, "Изменить заметку",platform);
        bot.handleCommand(uid, "1",platform);
        bot.handleCommand(uid, "Удалить заметку",platform);
        String resp = bot.handleCommand(uid, "нет",platform);
        Assertions.assertEquals("Удаление отменено.", resp);
        List<String> notes = mockNoteService.getAllNotes(login);
        Assertions.assertEquals(1, notes.size());
    }
    /**
     * Тест: попытка удаления несуществующей заметки возвращает сообщение об ошибке.
     */
    @Test
    public void deleteNonexistentNoteShowsError() throws SQLException {
        long uid = 5L;
        Platform platform = Platform.TELEGRAM;
        String login = "errorDelete_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        bot.handleCommand(uid, "Удалить заметку",platform);
        String resp = bot.handleCommand(uid, "10",platform);
        Assertions.assertEquals("Неизвестная команда. Используйте кнопки.", resp);
    }
    /**
     * Тест: вывод списка заметок корректно содержит все заметки с номерами.
     */
    @Test
    public void multipleNotesListedCorrectly() throws SQLException {
        long uid = 6L;
        Platform platform = Platform.TELEGRAM;
        String login = "notes_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        mockNoteService.addNote(login, "a");
        mockNoteService.addNote(login, "b");
        mockNoteService.addNote(login, "c");
        String list = bot.handleCommand(uid, "Список заметок",platform);
        Assertions.assertTrue(list.contains("1. a") && list.contains("2. b") && list.contains("3. c"));
    }
}