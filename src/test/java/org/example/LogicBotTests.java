package org.example;

import org.example.entity.NoteService;
import org.example.logic.LogicBot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;


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
    public class MockNoteDatabaseService implements NoteService {
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
            List<String> tags;
            Note(int id, long userId, String text, List<String> tags) {
                this.id = id;
                this.userId = userId;
                this.text = text;
                this.tags=new ArrayList<>(tags);
            }
        }

        private final Map<Long, List<Note>> storage = new HashMap<>();
        private int nextId = 1;

        /**
         * Конструктор моковой БД заметок.
         */
        public MockNoteDatabaseService() {
            // in-memory mock; no DB file will be created
        }

        /**
         * Получает все теги из заметки
         * @param text
         * @return
         */
        private List<String> extractTags(String text) {
            List<String> tags = new ArrayList<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[\\p{L}0-9_]+");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                tags.add(matcher.group().toLowerCase());
            }
            return new ArrayList<>(new LinkedHashSet<>(tags));
        }
        /**
         * Добавляет новую заметку для указанного пользователя.
         * @param userId идентификатор пользователя
         * @param text текст заметки
         */
        @Override
        public void addNote(long userId, String text) {
            List<String> tags = extractTags(text);
            storage.computeIfAbsent(userId, k -> new ArrayList<>()).add(new Note(nextId++, userId, text, tags));
        }


        /**
         * Возвращает список заметок для пользователя
         *
         * @param userId идентификатор пользователя
         * @return список заметок (в порядке добавления)
         */
        @Override
        public List<String> getAllNotes(long userId) {
            return storage.getOrDefault(userId, Collections.emptyList())
                    .stream()
                    .map(n -> n.text)
                    .toList();
        }

        /**
         * Возвращает внутренний id заметки по её порядковому номеру в списке пользователя.
         */
        @Override
        public Integer getNoteIdByIndex(long userId, int index) {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            if (index < 1 || index > list.size()) return null;
            return list.get(index - 1).id;
        }
        /**
         * Возвращает текст заметки по её внутреннему id для конкретного пользователя.
         * <p>Ищет заметку с указанным {@code noteId} в списке заметок пользователя {@code userId}.
         * Если заметка не найдена или принадлежит другому пользователю — возвращает {@code null}.</p>
         */
        @Override
        public String getNoteTextById(long userId, int noteId) {
            return storage.getOrDefault(userId, Collections.emptyList())
                    .stream()
                    .filter(n -> n.id == noteId && n.userId == userId)
                    .findFirst()
                    .map(n -> n.text)
                    .orElse(null);
        }
        /**
         * Обновляет текст заметки с указанным внутренним идентификатором для данного пользователя.
         *
         * <p>Если заметка с {@code noteId} найдена в списке пользователя {@code userId}, её текст
         * заменяется на {@code newText} и метод возвращает {@code true}. В противном случае
         * метод возвращает {@code false} и изменений не выполняется.</p>
         */
        @Override
        public boolean updateNote(long userId, int noteId, String newText) {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            for (Note n : list) {
                if (n.id == noteId && n.userId == userId) {
                    n.text = newText;
                    n.tags = extractTags(newText);
                    return true;
                }
            }
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
        public boolean deleteNote(long userId, int noteId) {
            List<Note> list = storage.getOrDefault(userId, Collections.emptyList());
            return list.removeIf(n -> n.id == noteId && n.userId == userId);
        }

        /**
         * Возвращает список тегов, привязанных к указанной заметке.
         * <p>
         * Теги возвращаются в порядке вставки (обычно — порядок в тексте).
         * </p>
         *
         * @param noteId идентификатор заметки
         * @return список тегов (например, {@code ["#личное", "#срочно"]}), пустой список — если тегов нет
         */
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

        /**
         * Возвращает заметки пользователя, отфильтрованные по тегу.
         * <p>
         * Если {@code tag} равен {@code null} или пуст, возвращаются все заметки.
         * Поиск по тегу нечувствителен к регистру благодаря приведению к нижнему регистру при сохранении.
         * </p>
         *
         * @param userId идентификатор пользователя
         * @param tag    тег для фильтрации (например, {@code "#личное"}), может быть {@code null}
         * @return список текстов заметок, содержащих указанный тег
         */
        @Override
        public List<String> getNotesByTag(long userId, String tag) {
            if (tag == null || tag.trim().isEmpty()) {
                return getAllNotes(userId);
            }
            String normalizedTag = tag.toLowerCase();
            return storage.getOrDefault(userId, Collections.emptyList())
                    .stream()
                    .filter(n -> n.tags.contains(normalizedTag))
                    .map(n -> n.text)
                    .toList();
        }

        /**
         * Возвращает список всех тегов пользователя с количеством заметок, в которых они используются.
         * <p>
         * Формат: {@code #тег — N заметок}
         * Теги сортируются по алфавиту.
         * </p>
         *
         * @param userId идентификатор пользователя
         * @return список строк вида {@code "#личное — 3 заметки"}
         */
        @Override
        public List<String> getAllUserTagsWithCounts(long userId) {
            Map<String, Integer> tagCount = new HashMap<>();
            for (Note n : storage.getOrDefault(userId, Collections.emptyList())) {
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
                    .toList();
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
    public void setUp() {
        mock = new MockNoteDatabaseService();
        bot = new LogicBot(mock);
    }

    /**
     * Тест: добавление заметки с тегом → тег извлекается и сохраняется.
     */
    @Test
    public void addNoteWithTagsExtractsTags() {
        long uid = 10L;
        bot.handleCommand(uid, "Новая заметка");
        bot.handleCommand(uid, "Купить хлеб #продукты #список");

        List<String> tags = mock.getTagsForNote(mock.getNoteIdByIndex(uid, 1));
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
        mock.addNote(uid, "Подготовить отчёт #работа");
        mock.addNote(uid, "Купить молоко #личное");
        mock.addNote(uid, "Идея для стартапа #идея #работа");

        List<String> workNotes = mock.getNotesByTag(uid, "#работа");
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
        mock.addNote(uid, "Заметка 1 #тег");
        mock.addNote(uid, "Заметка 2 #тег");
        mock.addNote(uid, "Заметка 3 #другой");

        List<String> tagList = mock.getAllUserTagsWithCounts(uid);
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
        mock.addNote(uid, "Старый текст #старый");
        int noteId = mock.getNoteIdByIndex(uid, 1);

        mock.updateNote(uid, noteId, "Новый текст #новый");

        List<String> tags = mock.getTagsForNote(noteId);
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
        mock.addNote(uid, "Текст с тегом #тег");
        int noteId = mock.getNoteIdByIndex(uid, 1);

        mock.updateNote(uid, noteId, "Текст без тегов");

        List<String> tags = mock.getTagsForNote(noteId);
        Assertions.assertTrue(tags.isEmpty());
    }

}
