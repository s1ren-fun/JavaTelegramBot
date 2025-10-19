package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Основной класс логики бота для управления заметками пользователей.
 * Обрабатывает пользовательские команды и состояния взаимодействия,
 * обеспечивая создание, просмотр, редактирование и удаление заметок
 * с использованием сервиса {@link NoteDatabaseService}.
 * <p>
 * Класс поддерживает контекстное взаимодействие: после выбора действия
 * (например, "Изменить заметку") бот переходит в соответствующее состояние
 * и ожидает дополнительный ввод от пользователя.
 *
 * @since 1.0
 */
public class LogicBot {

    /**
     * Сервис для взаимодействия с базой данных заметок.
     */
    private NoteDatabaseService noteService = new NoteDatabaseService();

    /**
     * Перечисление возможных состояний пользователя в процессе взаимодействия с ботом.
     * Используется для отслеживания контекста диалога.
     */
    private enum State {
        /**
         * Пользователь не находится в каком-либо специальном состоянии.
         */
        NONE,

        /**
         * Ожидание текста новой заметки.
         */
        AWAITING_NOTE_TEXT,

        /**
         * Ожидание идентификатора заметки для редактирования.
         */
        AWAITING_NOTE_ID_FOR_EDIT,

        /**
         * Ожидание нового текста для редактируемой заметки.
         */
        AWAITING_NEW_TEXT_FOR_EDIT,

        /**
         * Ожидание идентификатора заметки для удаления.
         */
        AWAITING_NOTE_ID_FOR_DELETE,

        /**
         * Ожидание подтверждения удаления заметки ("да" или "нет").
         */
        AWAITING_DELETE_CONFIRMATION
    }

    /**
     * Карта для хранения текущего состояния каждого пользователя по его идентификатору.
     */
    private final Map<Long, State> userStates = new HashMap<>();

    /**
     * Карта для временного хранения идентификатора заметки, с которой работает пользователь
     * (например, при редактировании или удалении).
     */
    private final Map<Long, Integer> userPendingNoteId = new HashMap<>();

    /**
     * Обрабатывает входящее сообщение от пользователя с учётом текущего состояния диалога.
     * В зависимости от состояния пользователя, метод либо ожидает дополнительные данные
     * (например, текст заметки или номер для редактирования), либо передаёт управление
     * в главное меню обработки команд.
     *
     * @param userId идентификатор пользователя (обычно Telegram ID)
     * @param input  текстовое сообщение от пользователя
     * @return ответное сообщение для отправки пользователю
     */
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
                        Integer realNoteId = noteService.getNoteIdByIndex(userId, noteId);
                        if (realNoteId!=null) {
                            userPendingNoteId.put(userId, realNoteId);
                            userStates.put(userId, State.AWAITING_NEW_TEXT_FOR_EDIT);
                            String current = noteService.getNoteTextById(userId, realNoteId);
                            return "Текущий текст заметки: «" +
                                    current +
                                    "» Отправьте новый текст.";
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
                    return "Заметка обновлена!";

                case AWAITING_NOTE_ID_FOR_DELETE:
                    if (isNumeric(input)) {
                        int userIndex = Integer.parseInt(input);
                        Integer realId = noteService.getNoteIdByIndex(userId, userIndex);
                        if (realId != null) {
                            String text = noteService.getNoteTextById(userId, realId);
                            userPendingNoteId.put(userId, realId);
                            userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                            return "Вы уверены, что хотите удалить заметку:«" +
                                    text +
                                    "»? Ответьте «да» или «нет».";
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
                        return "Заметка удалена.";
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
    public class ButtonLabels {
        public static final String NEW_NOTE = "Новая заметка";
        public static final String DELETE_NOTE = "Удалить заметку";
        public static final String NOTES_LIST = "Список заметок";
        public static final String EDIT_NOTE = "Изменить заметку";
    }

    /**
     * Обрабатывает команды главного меню, когда пользователь не находится в специальном состоянии.
     * Поддерживает команды для создания, просмотра, редактирования и удаления заметок.
     *
     * @param userId идентификатор пользователя
     * @param input  команда или текст от пользователя
     * @return ответное сообщение для отправки пользователю
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
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
                List<String> numbers = new ArrayList<>();
                for(int i =0;i< notes.size();++i){
                    numbers.add((i+1)+". "+notes.get(i));
                }
                return String.join("\n", numbers);

            case "Удалить заметку":
                List<String> allNotes = noteService.getAllNotes(userId);
                if (allNotes.isEmpty()) {
                    return "Нет заметок для удаления.";
                }
                List<String> allNumbers = new ArrayList<>();
                for(int i =0;i< allNotes.size();++i){
                    allNumbers.add((i+1)+". "+allNotes.get(i));
                }
                userStates.put(userId, State.AWAITING_NOTE_ID_FOR_DELETE);
                return "Введите номер заметки, которую хотите удалить:\n" +
                        String.join("\n", allNumbers);

            case "Изменить заметку":
                List<String> editNotes = noteService.getAllNotes(userId);
                if (editNotes.isEmpty()) {
                    return "Нет заметок для редактирования.";
                }
                List<String> editNumbers = new ArrayList<>();
                for(int i =0;i< editNotes.size();++i){
                    editNumbers.add((i+1)+". "+editNotes.get(i));
                }
                userStates.put(userId, State.AWAITING_NOTE_ID_FOR_EDIT);
                return "Введите номер заметки, которую хотите отредактировать:\n" +
                        String.join("\n", editNumbers);

            default:
                return "Неизвестная команда. Используйте кнопки";
        }
    }

    /**
     * Проверяет, является ли переданная строка корректным целым числом.
     *
     * @param str строка для проверки
     * @return {@code true}, если строка представляет собой целое число; {@code false} в противном случае
     */
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