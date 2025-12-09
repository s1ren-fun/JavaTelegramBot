package org.example;

import org.example.entity.Platform;
import org.example.logic.BotLogic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagsBotTests {
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
}
