package org.example;

import org.example.entity.*;
import org.example.logic.BotLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * MockNoteService — in-memory реализация NoteService для тестов.
     */
    public class MockNoteService implements NoteService {
        private class Note {
            int id;
            String login;
            String text;
            List<String> tags;

            Note(int id, String login, String text, List<String> tags) {
                this.id = id;
                this.login = login;
                this.text = text;
                this.tags = new ArrayList<>(tags);
            }
        }

        private final Map<String, List<Note>> storage = new HashMap<>();
        private int nextId = 1;

        private List<String> extractTags(String text) {
            List<String> tags = new ArrayList<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[\\p{L}0-9_]+");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                tags.add(matcher.group().toLowerCase());
            }
            return new ArrayList<>(new LinkedHashSet<>(tags));
        }

        @Override
        public void addNote(String login, String text) {
            List<String> tags = extractTags(text);
            storage.computeIfAbsent(login, k -> new ArrayList<>()).add(new Note(nextId++, login, text, tags));
        }

        @Override
        public Integer getNoteIdByIndex(String login, int index) {
            List<Note> list = storage.getOrDefault(login, Collections.emptyList());
            if (index < 1 || index > list.size()) return null;
            return list.get(index - 1).id;
        }

        @Override
        public String getNoteTextById(String login, int noteId) {
            return storage.getOrDefault(login, Collections.emptyList())
                    .stream()
                    .filter(n -> n.id == noteId && n.login.equals(login))
                    .findFirst()
                    .map(n -> n.text)
                    .orElse(null);
        }

        @Override
        public List<String> getTagsForNote(int noteId) {
            for (List<Note> notes : storage.values()) {
                for (Note n : notes) {
                    if (n.id == noteId) {
                        return new ArrayList<>(n.tags);
                    }
                }
            }
            return Collections.emptyList();
        }

        @Override
        public void updateNote(String login, int noteId, String newText) {
            List<Note> list = storage.getOrDefault(login, Collections.emptyList());
            for (Note n : list) {
                if (n.id == noteId && n.login.equals(login)) {
                    n.text = newText;
                    n.tags = extractTags(newText);
                    return;
                }
            }
        }

        @Override
        public void deleteNote(String login, int noteId) {
            List<Note> list = storage.getOrDefault(login, Collections.emptyList());
            list.removeIf(n -> n.id == noteId && n.login.equals(login));
        }

        @Override
        public List<String> getNotesByTag(String login, String tag) {
            if (tag == null || tag.trim().isEmpty()) {
                return getAllNotes(login);
            }
            String normalizedTag = tag.toLowerCase();
            return storage.getOrDefault(login, Collections.emptyList())
                    .stream()
                    .filter(n -> n.tags.contains(normalizedTag))
                    .map(n -> n.text)
                    .collect(Collectors.toList());
        }

        @Override
        public List<String> getAllUserTagsWithCounts(String login) {
            Map<String, Integer> tagCount = new HashMap<>();
            for (Note n : storage.getOrDefault(login, Collections.emptyList())) {
                for (String tag : n.tags) {
                    tagCount.merge(tag, 1, Integer::sum);
                }
            }
            return tagCount.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        String tag = entry.getKey();
                        int count = entry.getValue();
                        String suffix;
                        if (count % 10 == 1 && count % 100 != 11) {
                            suffix = "заметка";
                        } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
                            suffix = "заметки";
                        } else {
                            suffix = "заметок";
                        }
                        return tag + " — " + count + " " + suffix;
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public List<String> getAllNotes(String login) {
            return storage.getOrDefault(login, Collections.emptyList())
                    .stream()
                    .map(n -> n.text)
                    .collect(Collectors.toList());
        }
    }

    /**
     * MockReminderService — in-memory реализация ReminderService для тестов.
     */
    public class MockReminderService implements ReminderService {
        private class ReminderEntry {
            int id;
            String login;
            String text;
            LocalDateTime reminderTime;

            ReminderEntry(int id, String login, String text, LocalDateTime reminderTime) {
                this.id = id;
                this.login = login;
                this.text = text;
                this.reminderTime = reminderTime;
            }
        }

        private final List<ReminderEntry> reminders = new ArrayList<>();
        private int nextId = 1;

        @Override
        public void addReminder(String login, String text, LocalDateTime time) {
            reminders.add(new ReminderEntry(nextId++, login, text, time));
        }

        @Override
        public List<org.example.entity.Reminder> getUserReminders(String login) {
            return reminders.stream()
                    .filter(r -> r.login.equals(login))
                    .map(r -> new org.example.entity.Reminder(r.id, r.login, r.text, r.reminderTime))
                    .collect(Collectors.toList());
        }

        @Override
        public void updateReminder(int id, String text, LocalDateTime time) {
            for (ReminderEntry r : reminders) {
                if (r.id == id) {
                    if (text != null) r.text = text;
                    if (time != null) r.reminderTime = time;
                    break;
                }
            }
        }

        @Override
        public void deleteReminder(int id) {
            reminders.removeIf(r -> r.id == id);
        }

        @Override
        public List<org.example.entity.Reminder> getDueReminders(LocalDateTime now) {
            return reminders.stream()
                    .filter(r -> !r.reminderTime.isAfter(now))
                    .map(r -> new org.example.entity.Reminder(r.id, r.login, r.text, r.reminderTime))
                    .collect(Collectors.toList());
        }
    }

    /**
     * MockUserService — in-memory реализация UserService для тестов.
     */
    public class MockUserService implements UserService {
        private class UserEntry {
            String login;
            Long telegramId;
            Long discordId;
            java.time.ZoneId timezone;

            UserEntry(String login, Long telegramId, Long discordId, java.time.ZoneId timezone) {
                this.login = login;
                this.telegramId = telegramId;
                this.discordId = discordId;
                this.timezone = timezone != null ? timezone : java.time.ZoneId.of("UTC");
            }
        }

        private final Map<String, UserEntry> users = new HashMap<>();

        @Override
        public void registerUser(String login, Long telegramId, Long discordId, java.time.ZoneId timezone) {
            users.put(login, new UserEntry(login, telegramId, discordId, timezone));
        }

        @Override
        public void updateTelegramId(String login, Long telegramId) {
            UserEntry u = users.get(login);
            if (u != null) u.telegramId = telegramId;
        }

        @Override
        public void updateDiscordId(String login, Long discordId) {
            UserEntry u = users.get(login);
            if (u != null) u.discordId = discordId;
        }

        @Override
        public User getUserByLogin(String login) {
            UserEntry entry = users.get(login);
            if (entry == null) return null;
            return new User(entry.login, entry.telegramId, entry.discordId, entry.timezone);
        }
    }

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
     * Тест: создание напоминания.
     */
    @Test
    public void addReminderFlow() {
        long userId = 101L;
        Platform platform = Platform.TELEGRAM;
        String login = "reminder_user";
        String reminderText = "Подготовить отчёт";
        String timeInput = "30.10.2025 14:00";

        bot.handleCommand(userId, "/start",platform);
        bot.handleCommand(userId, login,platform);

        assertEquals("Отправьте текст напоминания.", bot.handleCommand(userId, "/new_reminder",platform));
        assertEquals("Укажите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ (например: 30.10.2025 14:00)", bot.handleCommand(userId, reminderText,platform));
        assertEquals("Напоминание сохранено!\nВы получите уведомление 30 октября в 14:00.", bot.handleCommand(userId, timeInput,platform));

        List<org.example.entity.Reminder> userReminders = mockReminderService.getUserReminders(login);
        assertEquals(1, userReminders.size());
        assertEquals(reminderText, userReminders.getFirst().getText());
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
}