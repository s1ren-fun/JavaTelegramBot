package org.example.logic;

import org.example.entity.NoteService;
import org.example.entity.NoteDatabaseService;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class BotLogic {

    /**
     * Сервис для взаимодействия с базой данных заметок.
     */
    private final NoteService noteService;


    /**
     * Конструктор по умолчанию — использует реальную базу данных.
     */
    public BotLogic() {
        this.noteService = new NoteDatabaseService();
    }

    /**
     * Тестовый/настраиваемый конструктор — позволяет передать альтернативную реализацию NoteDatabaseService,
     * например, моковую реализацию для unit-тестов. Это предотвращает создание реальной базы данных при тестировании.
     *
     * @param noteService реализация сервиса заметок
     */
    public BotLogic(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * Перечисление возможных состояний пользователя в процессе взаимодействия с ботом.
     * Используется для отслеживания контекста диалога.
     */
    public enum State {
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
        AWAITING_DELETE_CONFIRMATION,

        /**
         * Ожидание выбора тега для фильтрации заметок.
         */
        AWAITING_TAG_FOR_FILTER,

        /**
         * Ожидание номера заметки для редактирования её тегов.
         */
        AWAITING_NOTE_ID_FOR_TAG_EDIT,

        /**
         * Ожидание нового списка тегов для выбранной заметки.
         */
        AWAITING_NEW_TAGS_INPUT,
        AWAITING_ACTION_ON_NOTE
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

    public class ButtonLabels {
        public static final String NEW_NOTE = "Новая заметка";
        public static final String DELETE_NOTE = "Удалить заметку";
        public static final String NOTES_LIST = "Список заметок";
        public static final String FILTER_BY_TAG = "Фильтр по тегу";
        public static final String VIEW_TAGS = "Теги";
        public static final String EDIT_TAGS = "Изменить теги";
        public static final String EDIT_NOTE = "Изменить заметку";
        public static final String CANCEL = "Отмена";
    }
    public State getUserState(long userId) {
        return userStates.getOrDefault(userId, State.NONE);
    }
    /**
     * Обрабатывает входящее текстовое сообщение от пользователя с учётом текущего состояния диалога.
     * <p>
     * В зависимости от состояния пользователя, метод либо ожидает дополнительные данные
     * (например, текст заметки, номер для редактирования или тег),
     * либо передаёт управление в главное меню обработки команд.
     * </p>
     *
     * @param userId идентификатор пользователя (обычно Telegram ID)
     * @param input  текстовое сообщение от пользователя
     * @return ответное сообщение для отправки пользователю
     */
    public String handleCommand(long userId, String input) {
        String trimmedInput = input.trim();
        if ("Отмена".equalsIgnoreCase(trimmedInput) || "/cancel".equalsIgnoreCase(trimmedInput)) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            return "Действие отменено. Вы в главном меню.";
        }
        State state = userStates.getOrDefault(userId, State.NONE);

        try {
            switch (state) {
                case AWAITING_NOTE_TEXT:
                    noteService.addNote(userId, input);
                    userStates.remove(userId);
                    List<String> tags = extractTagsFromText(input);
                    if (!tags.isEmpty()) {
                        return "Заметка сохранена! 🏷️ Тег: " + String.join(", ", tags);
                    }
                    return "Заметка сохранена!";

                case AWAITING_NOTE_ID_FOR_EDIT:
                    return handleEditNoteSelection(userId, input);

                case AWAITING_NEW_TEXT_FOR_EDIT:
                    return handleNoteTextUpdate(userId, input);

                case AWAITING_NOTE_ID_FOR_DELETE:
                    return handleDeleteNoteSelection(userId, input);

                case AWAITING_DELETE_CONFIRMATION:
                    return handleDeleteConfirmation(userId, input);

                case AWAITING_TAG_FOR_FILTER:
                    return handleTagFilter(userId, input);

                case AWAITING_NOTE_ID_FOR_TAG_EDIT:
                    return handleEditTagSelection(userId, input);

                case AWAITING_NEW_TAGS_INPUT:
                    return handleTagUpdate(userId, input);

                case AWAITING_ACTION_ON_NOTE:
                    return handleNoteActionSelection(userId, input);

                default:
                    return handleMainMenu(userId, input);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка базы данных. Попробуйте позже.";
        }
    }

    /**
     * Обрабатывает выбор заметки для редактирования (текста или тегов).
     * <p>
     * Если ввод — число, загружается заметка и отображаются её данные.
     * Если ввод — команда действия («Изменить теги», «Удалить заметку»),
     * выполняется соответствующий переход в новое состояние.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param input  ввод пользователя (номер заметки или команда действия)
     * @return ответное сообщение
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleEditNoteSelection(long userId, String input) throws SQLException {
        if (isNumeric(input)) {
            int noteIndex = Integer.parseInt(input);
            Integer realNoteId = noteService.getNoteIdByIndex(userId, noteIndex);
            if (realNoteId != null) {
                userPendingNoteId.put(userId, realNoteId);
                String text = noteService.getNoteTextById(userId, realNoteId);
                List<String> tags = noteService.getTagsForNote(realNoteId);
                String tagStr = tags.isEmpty() ? "нет" : String.join(" ", tags);

                userStates.put(userId, State.AWAITING_ACTION_ON_NOTE);

                return String.format(
                        "Текст: %s\nТеги: %s\nВыберите действие:\n[Изменить текст]\n[Изменить теги]\n[Удалить заметку]",
                        text, tagStr
                );
            } else {
                return "Заметки с таким номером нет. Попробуйте снова.";
            }

        } else if (input.equals(ButtonLabels.EDIT_TAGS)) {
            Integer noteId = userPendingNoteId.get(userId);
            if (noteId != null) {
                userStates.put(userId, State.AWAITING_NEW_TAGS_INPUT);
                return "Отправьте новые теги (например: #работа #важное) или оставьте пустым для удаления всех тегов.";
            }
        } else if (input.equals(ButtonLabels.DELETE_NOTE)) {
            Integer noteId = userPendingNoteId.get(userId);
            if (noteId != null) {
                String text = noteService.getNoteTextById(userId, noteId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            }
        }
        return "Неизвестная команда. Выберите действие.";
    }

    /**
     * Обновляет текст выбранной заметки.
     *
     * @param userId идентификатор пользователя
     * @param input  новый текст заметки
     * @return сообщение об успешном обновлении или ошибке
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleNoteTextUpdate(long userId, String input) throws SQLException {
        Integer noteId = userPendingNoteId.get(userId);
        if (noteId == null) {
            userStates.remove(userId);
            return "Ошибка: заметка не выбрана.";
        }
        noteService.updateNote(userId, noteId, input);
        userStates.remove(userId);
        userPendingNoteId.remove(userId);
        return "Заметка обновлена!";
    }

    /**
     * Обрабатывает выбор заметки для удаления.
     *
     * @param userId идентификатор пользователя
     * @param input  номер заметки
     * @return сообщение с подтверждением удаления или ошибкой
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleDeleteNoteSelection(long userId, String input) throws SQLException {
        if (isNumeric(input)) {
            int userIndex = Integer.parseInt(input);
            Integer realId = noteService.getNoteIdByIndex(userId, userIndex);
            if (realId != null) {
                String text = noteService.getNoteTextById(userId, realId);
                userPendingNoteId.put(userId, realId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            } else {
                return "Заметки с таким номером не существует.";
            }
        }
        return "Введите корректный номер заметки.";
    }

    /**
     * Обрабатывает подтверждение удаления заметки.
     *
     * @param userId идентификатор пользователя
     * @param input  «да» или «нет»
     * @return результат операции
     */
    private String handleDeleteConfirmation(long userId, String input) {
        if ("да".equalsIgnoreCase(input.trim())) {
            Integer delId = userPendingNoteId.get(userId);
            if (delId != null) {
                try {
                    noteService.deleteNote(userId, delId);
                } catch (SQLException e) {
                    return "Ошибка при удалении.";
                }
            }
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
    }

    /**
     * Фильтрует заметки по выбранному тегу.
     * <p>
     * Поддерживает специальное значение «Все заметки» для отмены фильтрации.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param input  тег или команда «Все заметки»
     * @return список заметок с указанным тегом или сообщение об отсутствии
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleTagFilter(long userId, String input) throws SQLException {
        if ("Все заметки".equals(input)) {
            return showAllNotes(userId);
        }
        String tag = input.trim().toLowerCase();
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        List<String> notes = noteService.getNotesByTag(userId, tag);
        if (notes.isEmpty()) {
            return "Заметок с тегом " + tag + " не найдено.";
        }
        return String.join("\n", notes);
    }

    /**
     * Обрабатывает выбор заметки для редактирования её тегов.
     *
     * @param userId идентификатор пользователя
     * @param input  номер заметки
     * @return запрос на ввод новых тегов или сообщение об ошибке
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleEditTagSelection(long userId, String input) throws SQLException {
        if (isNumeric(input)) {
            int noteIndex = Integer.parseInt(input);
            Integer realNoteId = noteService.getNoteIdByIndex(userId, noteIndex);
            if (realNoteId != null) {
                userPendingNoteId.put(userId, realNoteId);
                userStates.put(userId, State.AWAITING_NEW_TAGS_INPUT);
                return "Отправьте новые теги (например: #продукты #список) или оставьте пустым для удаления всех тегов.";
            } else {
                return "Заметки с таким номером нет.";
            }
        }
        return "Введите корректный номер заметки.";
    }

    /**
     * Обновляет теги у выбранной заметки.
     * <p>
     * Сохраняет оригинальный текст заметки, удаляя из него старые теги,
     * и добавляет новые теги из ввода пользователя.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param input  новые теги или пустая строка для удаления всех
     * @return сообщение с результатом обновления тегов
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleTagUpdate(long userId, String input) throws SQLException {
        Integer noteId = userPendingNoteId.get(userId);
        if (noteId == null) {
            userStates.remove(userId);
            return "Ошибка: заметка не выбрана.";
        }

        String currentText = noteService.getNoteTextById(userId, noteId);
        if (currentText == null) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            return "Заметка не найдена.";
        }

        List<String> newTags = input.trim().isEmpty() ? Collections.emptyList() : extractTagsFromText(input);
        String textWithoutTags = removeTagsFromText(currentText);
        String newText = newTags.isEmpty() ? textWithoutTags
                : (textWithoutTags + " " + String.join(" ", newTags)).trim();

        noteService.updateNote(userId, noteId, newText);

        List<String> oldTags = noteService.getTagsForNote(noteId);
        if (newTags.isEmpty()) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            return "Все теги удалены!";
        }

        Set<String> oldSet = new HashSet<>(oldTags);
        Set<String> newSet = new HashSet<>(newTags);
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);

        StringBuilder response = new StringBuilder("Теги обновлены!");
        if (!removed.isEmpty()) {
            response.append(" Удалён(ы): ").append(String.join(", ", removed));
        }
        if (!added.isEmpty()) {
            response.append(" Добавлен(ы): ").append(String.join(", ", added));
        }
        if (newSet.size() == 1 && removed.isEmpty() && added.isEmpty()) {
            response.append(" Новый тег: ").append(newTags.getFirst());
        }

        userStates.remove(userId);
        userPendingNoteId.remove(userId);
        return response.toString();
    }

    /**
     * Обрабатывает команды главного меню, когда пользователь не находится в специальном состоянии.
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
            case "/new_note":
            case ButtonLabels.NEW_NOTE:
                userStates.put(userId, State.AWAITING_NOTE_TEXT);
                return "Отправьте текст заметки.";
            case "/all_note":
            case ButtonLabels.NOTES_LIST:
                return showAllNotes(userId);
            case "/filter_tag":
            case ButtonLabels.FILTER_BY_TAG:
                List<String> tagsWithCounts = noteService.getAllUserTagsWithCounts(userId);
                if (tagsWithCounts.isEmpty()) {
                    return "У вас пока нет тегов.";
                }
                String tagList = String.join("\n", tagsWithCounts);
                userStates.put(userId, State.AWAITING_TAG_FOR_FILTER);
                return "Выберите тег из списка:\n" + tagList + "\nВсе заметки";

            case ButtonLabels.VIEW_TAGS:
                List<String> allTags = noteService.getAllUserTagsWithCounts(userId);
                if (allTags.isEmpty()) {
                    return "У вас пока нет тегов.";
                }
                return "Доступные теги:\n" + String.join("\n", allTags);
            case "/edit_note":
            case ButtonLabels.EDIT_NOTE:
                return promptNoteSelection(userId);

            default:
                return "Неизвестная команда. Используйте кнопки.";
        }
    }
    /**
     * Формирует и возвращает список всех заметок пользователя с нумерацией.
     *
     * @param userId идентификатор пользователя
     * @return отформатированный список заметок или сообщение об их отсутствии
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String showAllNotes(long userId) throws SQLException {
        List<String> notes = noteService.getAllNotes(userId);
        if (notes.isEmpty()) {
            return "У вас пока нет заметок.";
        }
        return IntStream.range(0, notes.size())
                .mapToObj(i -> (i + 1) + ". " + notes.get(i))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Запрашивает у пользователя выбор заметки для редактирования текста.
     *
     * @param userId идентификатор пользователя
     * @return сообщение со списком заметок и запросом на ввод номера
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String promptNoteSelection(long userId) throws SQLException {
        List<String> notes = noteService.getAllNotes(userId);
        if (notes.isEmpty()) {
            return "Нет заметок.";
        }
        String list = IntStream.range(0, notes.size())
                .mapToObj(i -> (i + 1) + ". " + notes.get(i))
                .collect(Collectors.joining("\n"));
        userStates.put(userId, State.AWAITING_NOTE_ID_FOR_EDIT);
        return "Введите номер заметки для редактирования:" + "\n" + list;
    }

    /**
     * Извлекает теги из текста в формате {@code #тег}.
     * <p>
     * Поддерживаемый формат: {@code #} + буквы/цифры/нижнее подчёркивание.
     * Результат приводится к нижнему регистру, дубликаты удаляются.
     * </p>
     *
     * @param text текст заметки
     * @return список уникальных тегов в нижнем регистре
     */
    private List<String> extractTagsFromText(String text) {
        List<String> tags = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[\\p{L}0-9_]+");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(new LinkedHashSet<>(tags));
    }

    /**
     * Удаляет все теги из текста заметки.
     * <p>
     * Удаляет подстроки, соответствующие шаблону {@code #тег}, и нормализует пробелы.
     * </p>
     *
     * @param text исходный текст заметки
     * @return текст без тегов
     */
    private String removeTagsFromText(String text) {
        return text.replaceAll("#[\\p{L}0-9_]+", "").trim().replaceAll("\\s+", " ");
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

    /**
     * Обрабатывает выбор действия над выбранной заметкой.
     *
     * @param userId идентификатор пользователя
     * @param input  команда действия
     * @return ответное сообщение
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    private String handleNoteActionSelection(long userId, String input) throws SQLException {
        switch (input) {
            case "Изменить текст" -> {
                userStates.put(userId, State.AWAITING_NEW_TEXT_FOR_EDIT);
                Integer noteId = userPendingNoteId.get(userId);
                if (noteId == null) {
                    userStates.remove(userId);
                    return "Ошибка: заметка не выбрана.";
                }
                String current = noteService.getNoteTextById(userId, noteId);
                return "Текущий текст заметки: «" + current + "» Отправьте новый текст.";
            }
            case ButtonLabels.EDIT_TAGS -> {
                userStates.put(userId, State.AWAITING_NEW_TAGS_INPUT);
                return "Отправьте новые теги (например: #работа #важное) или оставьте пустым для удаления всех тегов.";
            }
            case ButtonLabels.DELETE_NOTE -> {
                Integer noteId = userPendingNoteId.get(userId);
                if (noteId == null) {
                    userStates.remove(userId);
                    return "Ошибка: заметка не выбрана.";
                }
                String text = noteService.getNoteTextById(userId, noteId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            }
        }
        return "Неизвестная команда. Выберите действие из списка.";
    }
}