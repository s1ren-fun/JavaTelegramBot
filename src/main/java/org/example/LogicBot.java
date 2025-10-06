package org.example;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogicBot {

    private final NoteDatabaseService noteService = new NoteDatabaseService();

    // Состояния пользователей
    private enum State {
        NONE,
        AWAITING_NOTE_TEXT,
        AWAITING_NOTE_ID_FOR_EDIT,
        AWAITING_NEW_TEXT_FOR_EDIT,
        AWAITING_NOTE_ID_FOR_DELETE,
        AWAITING_DELETE_CONFIRMATION
    }

    private final Map<Long, State> userStates = new HashMap<>();
    private final Map<Long, Integer> userPendingNoteId = new HashMap<>();

    // Новый метод: обработка с контекстом
    public String handleCommand(long userId, String input) {
        State state = userStates.getOrDefault(userId, State.NONE);

        try {
            switch (state) {
                case AWAITING_NOTE_TEXT:
                    noteService.addNote(userId, input);
                    userStates.remove(userId);
                    return "Заметка сохранена!";

                case AWAITING_NOTE_ID_FOR_EDIT:
                    if (isNumeric(input)) {
                        int noteId = Integer.parseInt(input);
                        if (noteService.noteExists(userId, noteId)) {
                            userPendingNoteId.put(userId, noteId);
                            userStates.put(userId, State.AWAITING_NEW_TEXT_FOR_EDIT);
                            String current = noteService.getNoteTextById(userId, noteId);
                            return "Текущий текст заметки:\n«" + current + "»\nОтправьте новый текст.";
                        } else {
                            return "Заметки с таким номером нет. Попробуйте снова.";
                        }
                    } else {
                        return "Введите корректный номер заметки.";
                    }

                case AWAITING_NEW_TEXT_FOR_EDIT:
                    int noteId = userPendingNoteId.get(userId);
                    noteService.updateNote(userId, noteId, input);
                    userStates.remove(userId);
                    userPendingNoteId.remove(userId);
                    return "Заметка №" + noteId + " обновлена!";

                case AWAITING_NOTE_ID_FOR_DELETE:
                    if (isNumeric(input)) {
                        int delNoteId = Integer.parseInt(input);
                        if (noteService.noteExists(userId, delNoteId)) {
                            String text = noteService.getNoteTextById(userId, delNoteId);
                            userPendingNoteId.put(userId, delNoteId);
                            userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                            return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
                        } else {
                            return "Заметки с таким номером не существует.";
                        }
                    } else {
                        return "Введите корректный номер заметки.";
                    }

                case AWAITING_DELETE_CONFIRMATION:
                    if ("да".equalsIgnoreCase(input.trim())) {
                        int delId = userPendingNoteId.get(userId);
                        noteService.deleteNote(userId, delId);
                        userStates.remove(userId);
                        userPendingNoteId.remove(userId);
                        return "Заметка №" + delId + " удалена.";
                    } else if ("нет".equalsIgnoreCase(input.trim())) {
                        userStates.remove(userId);
                        userPendingNoteId.remove(userId);
                        return "Удаление отменено.";
                    } else {
                        return "Ответьте «да» или «нет».";
                    }

                default:
                    return handleMainMenu(userId, input);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка базы данных. Попробуйте позже.";
        }
    }

    private String handleMainMenu(long userId, String input) throws SQLException {
        switch (input) {
            case "/start":
                return "Привет! Я помогу тебе сохранять и просматривать заметки. Используй кнопки ниже.";

            case "Новая заметка":
                userStates.put(userId, State.AWAITING_NOTE_TEXT);
                return "Отправьте текст заметки.";

            case "Список заметок":
                List<String> notes = noteService.getAllNotes(userId);
                if (notes.isEmpty()) {
                    return "У вас пока нет заметок.";
                }
                return String.join("\n", notes);

            case "Удалить заметку":
                List<String> allNotes = noteService.getAllNotes(userId);
                if (allNotes.isEmpty()) {
                    return "Нет заметок для удаления.";
                }
                userStates.put(userId, State.AWAITING_NOTE_ID_FOR_DELETE);
                return "Введите номер заметки, которую хотите удалить:\n" + String.join("\n", allNotes);

            case "/edit":
                List<String> editNotes = noteService.getAllNotes(userId);
                if (editNotes.isEmpty()) {
                    return "Нет заметок для редактирования.";
                }
                userStates.put(userId, State.AWAITING_NOTE_ID_FOR_EDIT);
                return "Введите номер заметки, которую хотите отредактировать:\n" + String.join("\n", editNotes);

            case "/delete":
                return handleMainMenu(userId, "Удалить заметку");

            default:
                return "Неизвестная команда. Используйте кнопки или команды /edit, /delete.";
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}