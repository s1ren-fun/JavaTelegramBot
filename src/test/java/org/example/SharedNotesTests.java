package org.example;

import org.example.entity.*;
import org.example.logic.BotLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SharedNotesTests {

    private BotLogic bot;
    private MockNoteService mockNoteService;
    private MockReminderService mockReminderService;
    private MockUserService mockUserService;

    @BeforeEach
    public void setUp() {
        mockNoteService = new MockNoteService();
        mockReminderService = new MockReminderService();
        mockUserService = new MockUserService();
        bot = new BotLogic(mockNoteService, mockReminderService, mockUserService);

    }

    /**
     * Тест: успешное создание общей заметки
     */
    @Test
    public void createSharedNoteSuccess() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);

        bot.handleCommand(userId, "Создать общую заметку", platform);

        String noteText = "Планы на корпоратив: 1. Выбрать место 2. Составить меню 3. Отправить приглашения";
        String response2 = bot.handleCommand(userId, noteText, platform);
        assertEquals("Укажите логины пользователей, которые будут иметь полный доступ к заметке (через запятую):", response2);

        String collaborators = "participant1, participant2";
        String response3 = bot.handleCommand(userId, collaborators, platform);
        assertTrue(response3.contains("Общая заметка создана!"));
        assertTrue(response3.contains("participant1"));
        assertTrue(response3.contains("participant2"));

        List<SharedNote> sharedNotes = mockNoteService.getSharedNotesForMember("creator");
        assertEquals(1, sharedNotes.size());
        assertEquals(noteText, sharedNotes.get(0).getText());

        assertTrue(sharedNotes.get(0).getMembers().contains("participant1"));
        assertTrue(sharedNotes.get(0).getMembers().contains("participant2"));
    }

    /**
     * Тест: редактирование общей заметки
     */
    @Test
    public void editSharedNoteSuccess() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);

        bot.handleCommand(userId, "Создать общую заметку", platform);
        String noteText = "Старый текст заметки";
        bot.handleCommand(userId, noteText, platform);
        bot.handleCommand(userId, "participant1", platform);

        List<SharedNote> sharedNotes = mockNoteService.getSharedNotesForMember("creator");

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "participant1", platform);

        String notesList = bot.handleCommand(101L, "Список заметок", platform);
        assertTrue(notesList.contains("Старый текст заметки"));

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);
        bot.handleCommand(userId, "Изменить заметку", platform);
        bot.handleCommand(userId, "1", platform);
        bot.handleCommand(userId, "Изменить текст", platform);

        String newText = "Обновленный текст заметки";
        bot.handleCommand(userId, newText, platform);

        String creatorNotesList = bot.handleCommand(userId, "Список заметок", platform);
        assertTrue(creatorNotesList.contains("Обновленный текст заметки"));

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "participant1", platform);
        String participantNotesList = bot.handleCommand(101L, "Список заметок", platform);
        assertTrue(participantNotesList.contains("Обновленный текст заметки"));
    }

    /**
     * Тест: удаление пользователя из общей заметки
     */
    @Test
    public void removeUserFromSharedNoteSuccess() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;
        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "participant1", platform);
        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "participant2", platform);
        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);


        bot.handleCommand(userId, "Создать общую заметку", platform);
        bot.handleCommand(userId, "Тестовая заметка", platform);
        bot.handleCommand(userId, "participant1, participant2", platform);

        List<SharedNote> sharedNotes = mockNoteService.getSharedNotesForMember("creator");
        int sharedNoteId = sharedNotes.getFirst().getId();

        assertTrue(mockNoteService.getSharedNotesForMember("participant1").stream()
                .anyMatch(note -> note.getId() == sharedNoteId));
        assertTrue(mockNoteService.getSharedNotesForMember("participant2").stream()
                .anyMatch(note -> note.getId() == sharedNoteId));

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);
        bot.handleCommand(userId, "Изменить заметку", platform);
        bot.handleCommand(userId, "1", platform);
        bot.handleCommand(userId, "Управление доступом", platform);
        bot.handleCommand(userId, "Удалить пользователя", platform);
        bot.handleCommand(userId, "2", platform);
        bot.handleCommand(userId, "да", platform);
        List<SharedNote> updatedSharedNotesForParticipant1 = mockNoteService.getSharedNotesForMember("participant1");

        assertFalse(updatedSharedNotesForParticipant1.stream()
                .anyMatch(note -> note.getId() == sharedNoteId));

        assertTrue(mockNoteService.getSharedNotesForMember("participant2").stream()
                .anyMatch(note -> note.getId() == sharedNoteId));
    }

    /**
     * Тест: добавление пользователя в общую заметку
     */
    @Test
    public void addUserToSharedNoteSuccess() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;
        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "participant2", platform);
        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);

        bot.handleCommand(userId, "Создать общую заметку", platform);
        bot.handleCommand(userId, "Тестовая заметка", platform);
        bot.handleCommand(userId, "participant1", platform);

        List<SharedNote> sharedNotes = mockNoteService.getSharedNotesForMember("creator");
        int sharedNoteId = sharedNotes.get(0).getId();

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "creator", platform);
        bot.handleCommand(userId, "Изменить заметку", platform);

        bot.handleCommand(userId, "1", platform);
        bot.handleCommand(userId, "Управление доступом", platform);
        bot.handleCommand(userId, "Добавить пользователя", platform);
        bot.handleCommand(userId, "participant2", platform);

        List<SharedNote> updatedParticipant2Notes = mockNoteService.getSharedNotesForMember("participant2");
        assertTrue(updatedParticipant2Notes.stream().anyMatch(note -> note.getId() == sharedNoteId));
    }

    /**
     * Тест: предоставление доступа на чтение для личной заметки
     */
    @Test
    public void grantReadPermissionSuccess() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "reader1", platform);

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "owner", platform);

        mockNoteService.addNote("owner", "Подготовить презентацию #работа");

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "owner", platform);
        bot.handleCommand(userId, "Изменить заметку", platform);
        bot.handleCommand(userId, "1", platform);
        bot.handleCommand(userId, "Предоставить доступ на чтение", platform);
        bot.handleCommand(userId, "reader1", platform);

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "reader1", platform);
        String notesList = bot.handleCommand(101L, "Список заметок", platform);
        assertTrue(notesList.contains("Подготовить презентацию #работа"));
        assertTrue(notesList.contains("(пользователь: owner)"));
    }

    /**
     * Тест: фильтрация заметок по тегу с учетом разных типов доступа
     */
    @Test
    public void filterNotesByTagWithDifferentAccessTypes() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "reader1", platform);

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "owner", platform);

        mockNoteService.addNote("owner", "Отправить отчёт бухгалтерии #работа");

        int sharedNoteId = mockNoteService.createSharedNote("owner", "Планы на корпоратив #работа");
        mockNoteService.addMemberToSharedNote(sharedNoteId, "owner", "owner");
        mockNoteService.addMemberToSharedNote(sharedNoteId, "reader1", "owner");

        Integer noteId = mockNoteService.getNoteIdByIndex("owner", 1);
        mockNoteService.grantReadPermission(noteId, "owner", "reader1");

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "reader1", platform);

         bot.handleCommand(101L, "Фильтр по тегу", platform);
        String response = bot.handleCommand(101L, "#работа", platform);

        assertTrue(response.contains("Общие заметки:"), "Должен быть раздел общих заметок");
        assertTrue(response.contains("заметки других пользователей:"), "Должен быть раздел заметок других пользователей");

        assertTrue(response.contains("Отправить отчёт бухгалтерии #работа"), "Должна быть личная заметка с тегом #работа");
        assertTrue(response.contains("Планы на корпоратив #работа"), "Должна быть общая заметка с тегом #работа");
    }

    /**
     * Тест: невозможность редактирования заметки с правами только на чтение
     */
    @Test
    public void cannotEditNoteWithReadOnlyPermission() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "owner", platform);

        mockNoteService.addNote("owner", "Заметка только для чтения");

        Integer noteId = mockNoteService.getNoteIdByIndex("owner", 1);
        mockNoteService.grantReadPermission(noteId, "owner", "reader1");

        bot.handleCommand(101L, "/start", platform);
        bot.handleCommand(101L, "reader1", platform);

        String response = bot.handleCommand(101L, "Изменить заметку", platform);

        assertTrue(response.contains("Нет заметок для редактирования"));
    }

    /**
     * Тест: невозможно удалить самого себя из общей заметки
     */
    @Test
    public void cannotRemoveSelfFromSharedNote() throws Exception {
        long userId = 100L;
        Platform platform = Platform.TELEGRAM;

        bot.handleCommand(userId, "/start", platform);
        bot.handleCommand(userId, "user1", platform);

        bot.handleCommand(userId, "Создать общую заметку", platform);
        bot.handleCommand(userId, "Общая заметка", platform);
        bot.handleCommand(userId, "user2", platform);

        List<SharedNote> sharedNotes = mockNoteService.getSharedNotesForMember("user1");
        int sharedNoteId = sharedNotes.getFirst().getId();

        bot.handleCommand(userId, "Изменить заметку", platform);
        bot.handleCommand(userId, "1", platform);
        bot.handleCommand(userId, "Управление доступом", platform);
        bot.handleCommand(userId, "Удалить пользователя", platform);

        String response = bot.handleCommand(userId, "1", platform);
        assertTrue(response.contains("user1"));

        response = bot.handleCommand(userId, "да", platform);
        assertTrue(response.contains("Вы не можете удалить самого себя"));

        assertTrue(mockNoteService.getSharedNotesForMember("user1").stream()
                .anyMatch(note -> note.getId() == sharedNoteId));
    }

}