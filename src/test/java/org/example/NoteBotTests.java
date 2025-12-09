package org.example;

import org.example.entity.Platform;
import org.example.logic.BotLogic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoteBotTests {
    private BotLogic bot;
    private MockNoteService mockNoteService;
    private MockReminderService mockReminderService;
    private MockUserService mockUserService;

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
     * Тест: сценарий создания заметки — запрос текста, сохранение, проверка содержимого хранилища.
     */
    @Test
    public void createNoteFlow(){
        long uid = 2L;
        Platform platform = Platform.TELEGRAM;
        String login = "not_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        Assertions.assertEquals("Отправьте текст заметки.", bot.handleCommand(uid, "Новая заметка",platform));
        Assertions.assertEquals("Заметка сохранена!", bot.handleCommand(uid, "Текст заметки",platform));
        List<String> notes = mockNoteService.getAllNotes(login);
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals("Текст заметки", notes.getFirst());
    }
    /**
     * Тест: попытка редактировать несуществующую заметку должна вернуть сообщение об ошибке.
     */
    @Test
    public void editNonexistentNoteShowsError(){
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
    public void deleteCancelKeepsNote(){
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
    public void deleteNonexistentNoteShowsError(){
        long uid = 5L;
        Platform platform = Platform.TELEGRAM;
        String login = "errorDelete_user";

        bot.handleCommand(uid, "/start",platform);
        bot.handleCommand(uid, login,platform);
        bot.handleCommand(uid, "Удалить заметку",platform);
        String resp = bot.handleCommand(uid, "10",platform);
        Assertions.assertEquals("Неизвестная команда. Используйте кнопки.", resp);
    }

}
