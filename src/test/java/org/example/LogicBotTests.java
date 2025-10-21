package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogicBotTests — набор unit-тестов для проверки логики бота и поведения
 * фиктивной базы заметок (MockNoteDatabaseService).
 *
 * <p>Тесты покрывают следующие сценарии:
 * <ul>
 * <li>Создание, обновление и удаление заметок;</li>
 * <li>Обработка команд бота: добавление заметки, список, удаление, редактирование, помощь;</li>
 * <li>Проверка корректной валидации входных данных и ответов при некорректных командах.</li>
 * </ul>
 *
 * <p>Тестовый класс содержит вспомогательный вложенный класс {@link MockNoteDatabaseService},
 * который имитирует поведение реальной базы данных заметок в памяти и используется для изоляции
 * тестируемой логики от слоя хранения.
 */

public class LogicBotTests {

    /**
     * MockNoteDatabaseService — простая in-memory реализация NoteDatabaseService,
     * используемая в тестах для управления коллекцией заметок без реальной БД.
     */
    public class MockNoteDatabaseService extends NoteDatabaseService {
        /**
         * Внутреннее представление заметки в моковой базе.
         * Поля соответствуют структуре, ожидаемой тестируемой логикой:
         * {@code id} — уникальный идентификатор заметки,
         * {@code userId} — идентификатор владельца,
         * {@code text} — текст заметки.
         */
        private class Note {
            int id;
            long userId;
            String text;
            Note(int id, long userId, String text) {
                this.id = id;
                this.userId = userId;
                this.text = text;
            }
        }

        private final Map<Long, List<Note>> storage = new HashMap<>();
        private int nextId = 1;

        /**
         * Конструктор моковой БД заметок.
         */
        public MockNoteDatabaseService() {
            super();
        }
        /**
         * Добавляет новую заметку для указанного пользователя.
         * @param userId идентификатор пользователя
         * @param text текст заметки
         */
        @Override
        public void addNote(long userId, String text) throws SQLException {
            storage.computeIfAbsent(userId, k -> new ArrayList<>()).add(new Note(nextId++, userId, text));
        }


        /**
         * Возвращает список заметок для пользователя
         *
         * @param userId идентификатор пользователя
         * @return список заметок (в порядке добавления)
         */
        @Override
        public List<String> getAllNotes(long userId) throws SQLException {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            List<String> out = new ArrayList<>(list.size());
            for (Note n : list) out.add(n.text);
            return out;
        }

        /**
         * Возвращает внутренний id заметки по её порядковому номеру в списке пользователя.
         */
        @Override
        public Integer getNoteIdByIndex(long userId, int index) throws SQLException {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            if (index < 1 || index > list.size()) return null;
            return list.get(index - 1).id; // Возвращаем id
        }
        /**
         * Возвращает текст заметки по её внутреннему id для конкретного пользователя.
         * <p>Ищет заметку с указанным {@code noteId} в списке заметок пользователя {@code userId}.
         * Если заметка не найдена или принадлежит другому пользователю — возвращает {@code null}.</p>
         */
        @Override
        public String getNoteTextById(long userId, int noteId) throws SQLException {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            for (Note n : list) if (n.id == noteId && n.userId == userId) return n.text;
            return null;
        }
        /**
         * Обновляет текст заметки с указанным внутренним идентификатором для данного пользователя.
         *
         * <p>Если заметка с {@code noteId} найдена в списке пользователя {@code userId}, её текст
         * заменяется на {@code newText} и метод возвращает {@code true}. В противном случае
         * метод возвращает {@code false} и изменений не выполняется.</p>
         */
        @Override
        public boolean updateNote(long userId, int noteId, String newText) throws SQLException {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            for (Note n : list) if (n.id == noteId && n.userId == userId) { n.text = newText; return true; }
            return false;
        }
        /**
         * Удаляет заметку с указанным внутренним идентификатором для данного пользователя.
         *
         * <p>Итеративно проходит по списку заметок пользователя и удаляет первую заметку,
         * у которой совпадают {@code id} и {@code userId}. При успешном удалении возвращает {@code true},
         * если заметка не найдена — {@code false}.</p>
         */
        @Override
        public boolean deleteNote(long userId, int noteId) throws SQLException {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            Iterator<Note> it = list.iterator();
            while (it.hasNext()) {
                Note n = it.next();
                if (n.id == noteId && n.userId == userId) { it.remove(); return true; }
            }
            return false;
        }
    }

    private LogicBot bot;
    private MockNoteDatabaseService mock;
    /**
     * Подготавливает окружение для тестов: создаёт экземпляры {@code LogicBot} и
     * {@code MockNoteDatabaseService}, после чего подменяет приватное поле {@code noteService}
     * в боте на мок-реализацию.
     *
     * <p>Аннотирован как {@code @BeforeEach} — выполняется перед каждым тестом.</p>
     */
    @BeforeEach
    public void setUp() throws Exception {
        bot = new LogicBot();
        mock = new MockNoteDatabaseService();
        setPrivateField(bot, "noteService", mock);
    }
    /**
     * Устанавливает значение приватного (или защищённого) поля у целевого объекта через reflection.
     * <p>Поиск поля ведётся вверх по иерархии классов (включая суперклассы).</p>
     */
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Находит поле с именем {@code fieldName} в классе {@code cls} или в одном из его суперклассов.
     *
     * @param cls класс, в котором начинается поиск
     * @param fieldName имя искомого поля
     * @return найденное {@link Field}
     */
    private Field findField(Class<?> cls, String fieldName) throws NoSuchFieldException {
        Class<?> cur = cls;
        while (cur != null) {
            try { return cur.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { cur = cur.getSuperclass(); }
        }
        throw new NoSuchFieldException(fieldName);
    }


    /**
     * Тест: команда /start возвращает приветственное сообщение с подсказкой о дальнейших действиях.
     */
    @Test
    public void startCommandReturnsWelcome() throws SQLException {
        long uid = 1L;
        String resp = bot.handleCommand(uid, "/start");
        assertEquals("Привет! Я помогу тебе сохранять и просматривать заметки. Используй кнопки ниже.", resp);
    }
    /**
     * Тест: сценарий создания заметки — запрос текста, сохранение, проверка содержимого хранилища.
     */
    @Test
    public void createNoteFlow() throws SQLException {
        long uid = 2L;
        assertEquals("Отправьте текст заметки.", bot.handleCommand(uid, "Новая заметка"));
        assertEquals("Заметка сохранена!", bot.handleCommand(uid, "Текст заметки"));
        List<String> notes = mock.getAllNotes(uid);
        assertEquals(1, notes.size());
        assertEquals("Текст заметки", notes.get(0));
    }
    /**
     * Тест: попытка редактировать несуществующую заметку должна вернуть сообщение об ошибке.
     */
    @Test
    public void editNonexistentNoteShowsError() throws SQLException {
        long uid = 3L;
        bot.handleCommand(uid, "Изменить заметку");
        String resp = bot.handleCommand(uid, "1");
        assertEquals("Неизвестная команда. Используйте кнопки", resp);
    }
    /**
     * Тест: отмена удаления оставляет заметку в хранилище.
     */
    @Test
    public void deleteCancelKeepsNote() throws SQLException {
        long uid = 4L;
        mock.addNote(uid, "не удалять");
        bot.handleCommand(uid, "Удалить заметку");
        bot.handleCommand(uid, "1");
        String resp = bot.handleCommand(uid, "нет");
        assertEquals("Удаление отменено.", resp);
        List<String> notes = mock.getAllNotes(uid);
        assertEquals(1, notes.size());
    }
    /**
     * Тест: попытка удаления несуществующей заметки возвращает сообщение об ошибке.
     */
    @Test
    public void deleteNonexistentNoteShowsError() throws SQLException {
        long uid = 5L; // Пользователь
        bot.handleCommand(uid, "Удалить заметку");
        String resp = bot.handleCommand(uid, "10");
        assertEquals("Неизвестная команда. Используйте кнопки", resp);
    }
    /**
     * Тест: вывод списка заметок корректно содержит все заметки с номерами.
     */
    @Test
    public void multipleNotesListedCorrectly() throws SQLException {
        long uid = 6L; // Пользователь
        mock.addNote(uid, "a");
        mock.addNote(uid, "b");
        mock.addNote(uid, "c");
        String list = bot.handleCommand(uid, "Список заметок");
        assertTrue(list.contains("1. a") && list.contains("2. b") && list.contains("3. c"));
    }
}
