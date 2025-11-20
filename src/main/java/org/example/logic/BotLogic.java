package org.example.logic;

import org.example.entity.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Основной класс логики бота для управления заметками и напоминаниями.
 * Обрабатывает пользовательские команды и состояния взаимодействия,
 * обеспечивая создание, просмотр, редактирование и удаление заметок и напоминаний
 * с использованием сервисов {@link NoteDatabaseService} и {@link ReminderDatabaseService}.
 * <p>
 * Также поддерживает аутентификацию по логину и привязку платформ.</p>
 *
 * @since 1.1
 */
public class BotLogic {

    private final NoteService noteService;
    private final ReminderService reminderService;
    private final UserService userService;

    /**
     * Конструктор по умолчанию — использует реальные сервисы.
     */
    public BotLogic() {
        this.noteService = new NoteDatabaseService();
        this.reminderService = new ReminderDatabaseService();
        this.userService = new UserDatabaseService();
    }

    /**
     * Тестовый/настраиваемый конструктор — позволяет передать альтернативные реализации сервисов,
     * например, моковые реализации для unit-тестов.
     *
     * @param noteService реализация сервиса заметок
     * @param reminderService реализация сервиса напоминаний
     * @param userService реализация сервиса пользователей
     */
    public BotLogic(NoteService noteService, ReminderService reminderService, UserService userService) {
        this.noteService = noteService;
        this.reminderService = reminderService;
        this.userService = userService;
    }

    /**
     * Перечисление возможных состояний пользователя в процессе взаимодействия с ботом.
     * Используется для отслеживания контекста диалога.
     */
    public enum State {
        AWAITING_REMINDER_TIME_FROM_NOTE,
        /**
         * Пользователь не находится в каком-либо специальном состоянии.
         */
        NONE,

        /**
         * Ожидание ввода логина пользователя.
         */
        AWAITING_LOGIN,

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

        /**
         * Ожидание выбора действия над заметкой (изменить текст/теги, удалить).
         */
        AWAITING_ACTION_ON_NOTE,

        /**
         * Ожидание выбора номера заметки для преобразования в напоминание.
         */
        AWAITING_NOTE_ID_FOR_REMINDER,

        /**
         * Ожидание текста нового напоминания.
         */
        AWAITING_REMINDER_TEXT,

        /**
         * Ожидание даты и времени нового напоминания.
         */
        AWAITING_REMINDER_TIME,

        /**
         * Ожидание выбора действия над напоминанием (изменить текст/время, удалить).
         */
        AWAITING_REMINDER_EDIT_ACTION,

        /**
         * Ожидание новой даты/времени напоминания.
         */
        AWAITING_REMINDER_EDIT_TIME,

        /**
         * Ожидание нового текста напоминания.
         */
        AWAITING_REMINDER_EDIT_TEXT,

        /**
         * Ожидание подтверждения удаления напоминания.
         */
        AWAITING_REMINDER_DELETE_CONFIRMATION,

        /**
         * Ожидание выбора номера напоминания для действия (изменить/удалить).
         */
        AWAITING_REMINDER_ID_FOR_ACTION
    }

    /**
     * Карта для хранения текущего состояния каждого пользователя по его идентификатору.
     */
    private final Map<Long, State> userStates = new HashMap<>();

    /**
     * Карта для временного хранения идентификатора заметки, с которой работает пользователь.
     */
    private final Map<Long, Integer> userPendingNoteId = new HashMap<>();

    /**
     * Карта для временного хранения идентификатора напоминания, с которым работает пользователь.
     */
    private final Map<Long, Integer> userPendingReminderId = new HashMap<>();

    /**
     * Карта для временного хранения логина, введённого пользователем.
     */
    private final Map<Long, String> userPendingLogin = new HashMap<>();

    /**
     * Карта для временного хранения текста напоминания.
     */
    private final Map<Long, String> userPendingReminderText = new HashMap<>();

    /**
     * Класс, содержащий текстовые метки кнопок.
     */
    public class ButtonLabels {
        public static final String NEW_NOTE = "Новая заметка";
        public static final String DELETE_NOTE = "Удалить заметку";
        public static final String NOTES_LIST = "Список заметок";
        public static final String FILTER_BY_TAG = "Фильтр по тегу";
        public static final String EDIT_TAGS = "Изменить теги";
        public static final String EDIT_NOTE = "Изменить заметку";
        public static final String CANCEL = "Отмена";
        public static final String NEW_REMINDER = "Новое напоминание";
        public static final String MY_REMINDERS = "Мои напоминания";
        public static final String CONVERT_TO_REMINDER = "Сделать напоминание";
    }

    /**
     * Возвращает текущее состояние пользователя.
     *
     * @param userId идентификатор пользователя
     * @return текущее состояние
     */
    public State getUserState(long userId) {
        return userStates.getOrDefault(userId, State.NONE);
    }

    /**
     * Обрабатывает входящее текстовое сообщение от пользователя с учётом текущего состояния диалога и платформы.
     * <p>
     * В зависимости от состояния пользователя, метод либо ожидает дополнительные данные
     * (например, текст заметки, дату напоминания), либо передаёт управление в главное меню.
     * </p>
     *
     * @param userId идентификатор пользователя (обычно Telegram ID или Discord ID)
     * @param input  текстовое сообщение от пользователя
     * @param platform платформа, с которой пришло сообщение
     * @return ответное сообщение для отправки пользователю
     */
    public String handleCommand(long userId, String input, Platform platform) {
        String trimmedInput = input.trim();
        if ("Отмена".equalsIgnoreCase(trimmedInput) || "/cancel".equalsIgnoreCase(trimmedInput)) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            userPendingReminderId.remove(userId);
            userPendingLogin.remove(userId);
            userPendingReminderText.remove(userId);
            return "Действие отменено. Вы в главном меню.";
        }

        State state = userStates.getOrDefault(userId, State.NONE);

        try {
            return switch (state) {
                case AWAITING_LOGIN -> handleLogin(userId, input, platform);
                case AWAITING_NOTE_TEXT -> handleNoteText(userId, input);
                case AWAITING_NOTE_ID_FOR_REMINDER ->
                        handleSelectNoteForReminder(userId, input);
                case AWAITING_REMINDER_TEXT -> handleReminderText(userId, input);
                case AWAITING_REMINDER_TIME -> handleReminderTime(userId, input);
                case AWAITING_REMINDER_EDIT_ACTION ->
                        handleReminderEditAction(userId, input);
                case AWAITING_REMINDER_EDIT_TIME ->
                        handleReminderEditTime(userId, input);
                case AWAITING_REMINDER_EDIT_TEXT ->
                        handleReminderEditText(userId, input);
                case AWAITING_REMINDER_DELETE_CONFIRMATION ->
                        handleReminderDeleteConfirmation(userId, input);
                case AWAITING_REMINDER_ID_FOR_ACTION ->
                        handleSelectReminderForAction(userId, input);
                case AWAITING_NOTE_ID_FOR_EDIT ->
                        handleEditNoteSelection(userId, input);
                case AWAITING_NEW_TEXT_FOR_EDIT ->
                        handleNoteTextUpdate(userId, input);
                case AWAITING_NOTE_ID_FOR_DELETE ->
                        handleDeleteNoteSelection(userId, input);
                case AWAITING_DELETE_CONFIRMATION ->
                        handleDeleteConfirmation(userId, input);
                case AWAITING_TAG_FOR_FILTER -> handleTagFilter(userId, input);
                case AWAITING_NOTE_ID_FOR_TAG_EDIT ->
                        handleEditTagSelection(userId, input);
                case AWAITING_NEW_TAGS_INPUT -> handleTagUpdate(userId, input);
                case AWAITING_ACTION_ON_NOTE ->
                        handleNoteActionSelection(userId, input);
                case AWAITING_REMINDER_TIME_FROM_NOTE ->
                        handleReminderTimeFromNote(userId, input);
                default -> handleMainMenu(userId, input);
            };
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка базы данных. Попробуйте позже.";
        }
    }

    private String handleReminderTimeFromNote(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingReminderText.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        String text = userPendingReminderText.get(userId);
        if (text == null) {
            userStates.remove(userId);
            return "Ошибка: текст напоминания не найден.";
        }

        LocalDateTime time;
        try {
            time = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            return "Неверный формат. Попробуйте снова.";
        }

        reminderService.addReminder(login, text, time);

        userStates.remove(userId);
        userPendingReminderText.remove(userId);

        return "Напоминание создано из заметки!\nВы получите уведомление " + time.format(DateTimeFormatter.ofPattern("dd MMMM в HH:mm")) + ".";
    }

    private String handleLogin(long userId, String input, Platform platform) throws SQLException {
        String login = input.trim();
        if (login.isEmpty()) {
            return "Логин не может быть пустым. Попробуйте снова.";
        }

        User existingUser = userService.getUserByLogin(login);

        User userToSave = new User(login);
        if (existingUser != null) {
            userToSave.setTelegramId(existingUser.getTelegramId());
            userToSave.setDiscordId(existingUser.getDiscordId());
            userToSave.setTimezone(existingUser.getTimezone());
        }

        if (platform == Platform.TELEGRAM) {
            userToSave.setTelegramId(userId);
        } else if (platform == Platform.DISCORD) {
            userToSave.setDiscordId(userId);
        }

        userService.registerUser(userToSave.getLogin(), userToSave.getTelegramId(), userToSave.getDiscordId(), userToSave.getTimezone());

        userPendingLogin.put(userId, login);
        userStates.remove(userId);
        return "Добро пожаловать, " + login + "! Вы можете использовать бота.";
    }

    private String handleNoteText(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        noteService.addNote(login, input);
        userStates.remove(userId);
        List<String> tags = extractTagsFromText(input);
        if (!tags.isEmpty()) {
            return "Заметка сохранена! 🏷️ Тег: " + String.join(", ", tags);
        }
        return "Заметка сохранена!";
    }

    private String handleReminderText(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        userPendingReminderText.put(userId, input);
        userStates.put(userId, State.AWAITING_REMINDER_TIME);
        return "Укажите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ (например: 30.10.2025 14:00)";
    }

    private String handleReminderTime(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        String text = userPendingReminderText.get(userId);

        if (text == null) {
            userStates.remove(userId);
            userPendingReminderText.remove(userId);
            return "Ошибка: текст напоминания не найден.";
        }

        LocalDateTime time;
        try {
            time = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            return "Неверный формат. Попробуйте снова.";
        }

        reminderService.addReminder(login, text, time);

        userStates.remove(userId);
        userPendingReminderText.remove(userId);

        return "Напоминание сохранено!\nВы получите уведомление " + time.format(DateTimeFormatter.ofPattern("dd MMMM в HH:mm")) + ".";
    }

    private String handleReminderEditAction(long userId, String input) throws SQLException {
        int reminderId = userPendingReminderId.get(userId);
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        List<Reminder> reminders = reminderService.getUserReminders(login);
        Reminder selectedReminder = null;
        for (Reminder r : reminders) {
            if (r.getId() == reminderId) {
                selectedReminder = r;
                break;
            }
        }

        if (selectedReminder == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: напоминание не найдено.";
        }

        return switch (input) {
            case "Изменить текст" -> {
                userStates.put(userId, State.AWAITING_REMINDER_EDIT_TEXT);
                yield "Текущий текст: " + selectedReminder.getText() + "\nВведите новый текст.";
            }
            case "Изменить дату/время" -> {
                userStates.put(userId, State.AWAITING_REMINDER_EDIT_TIME);
                yield "Текущее время: " + selectedReminder.getReminderTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\nУкажите новое время в формате ДД.ММ.ГГГГ ЧЧ:ММ";
            }
            case "Удалить" -> {
                userStates.put(userId, State.AWAITING_REMINDER_DELETE_CONFIRMATION);
                yield "Вы уверены, что хотите удалить напоминание:\n«" + selectedReminder.getText() + "»?\nОтветьте «да» или «нет».";
            }
            default -> "Неизвестная команда. Выберите действие из списка.";
        };
    }

    private String handleReminderEditTime(long userId, String input) throws SQLException {
        int reminderId = userPendingReminderId.get(userId);
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        List<Reminder> reminders = reminderService.getUserReminders(login);
        Reminder selectedReminder = null;
        for (Reminder r : reminders) {
            if (r.getId() == reminderId) {
                selectedReminder = r;
                break;
            }
        }

        if (selectedReminder == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: напоминание не найдено.";
        }

        LocalDateTime time;
        try {
            time = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            return "Неверный формат. Попробуйте снова.";
        }

        reminderService.updateReminder(reminderId, selectedReminder.getText(), time);

        userStates.remove(userId);
        userPendingReminderId.remove(userId);
        return "Время напоминания обновлено! Новое время: " + time.format(DateTimeFormatter.ofPattern("dd MMMM в HH:mm")) + ".";
    }

    private String handleReminderEditText(long userId, String input) throws SQLException {
        int reminderId = userPendingReminderId.get(userId);
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        List<Reminder> reminders = reminderService.getUserReminders(login);
        Reminder selectedReminder = null;
        for (Reminder r : reminders) {
            if (r.getId() == reminderId) {
                selectedReminder = r;
                break;
            }
        }

        if (selectedReminder == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: напоминание не найдено.";
        }

        reminderService.updateReminder(reminderId, input, selectedReminder.getReminderTime());

        userStates.remove(userId);
        userPendingReminderId.remove(userId);
        return "Текст напоминания обновлён!";
    }


    private String handleReminderDeleteConfirmation(long userId, String input) throws SQLException {
        int reminderId = userPendingReminderId.get(userId);
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if ("да".equalsIgnoreCase(input.trim())) {
            reminderService.deleteReminder(reminderId);
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Напоминание удалено.";
        } else if ("нет".equalsIgnoreCase(input.trim())) {
            userStates.remove(userId);
            userPendingReminderId.remove(userId);
            return "Удаление отменено.";
        } else {
            return "Ответьте «да» или «нет».";
        }
    }

    private String handleSelectNoteForReminder(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if (!isNumeric(input)) {
            return "Введите корректный номер заметки.";
        }

        int noteIndex = Integer.parseInt(input);
        Integer noteId = noteService.getNoteIdByIndex(login, noteIndex);
        if (noteId == null) {
            return "Заметки с таким номером не существует.";
        }

        String noteText = noteService.getNoteTextById(login, noteId);
        if (noteText == null) {
            return "Ошибка: текст заметки не найден.";
        }

        userPendingReminderText.put(userId, noteText);

        userStates.put(userId, State.AWAITING_REMINDER_TIME);
        return "Вы выбрали заметку:\n\"" + noteText + "\"\n\nУкажите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ (например: 30.10.2025 14:00)";
    }


    private String handleMainMenu(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);

        if (input.equals("/start")) {
            userStates.put(userId, State.AWAITING_LOGIN);
            return "Введите ваш логин.";
        }

        if (login == null) {
            return "Вы не авторизованы. Используйте /start.";
        }

        switch (input) {
            case "/new_reminder":
            case ButtonLabels.NEW_REMINDER:
                userStates.put(userId, State.AWAITING_REMINDER_TEXT);
                return "Отправьте текст напоминания.";
            case "/my_reminders":
            case ButtonLabels.MY_REMINDERS:
                userStates.put(userId, State.AWAITING_REMINDER_ID_FOR_ACTION);
                return showUserReminders(login);
            case ButtonLabels.CONVERT_TO_REMINDER:
                return promptNoteToConvert(userId);
            case "/new_note":
            case ButtonLabels.NEW_NOTE:
                userStates.put(userId, State.AWAITING_NOTE_TEXT);
                return "Отправьте текст заметки.";
            case "/all_note":
            case ButtonLabels.NOTES_LIST:
                return showAllNotes(login);
            case "/filter_tag":
            case ButtonLabels.FILTER_BY_TAG:
                List<String> tagsWithCounts = noteService.getAllUserTagsWithCounts(login);
                if (tagsWithCounts.isEmpty()) {
                    return "У вас пока нет тегов.";
                }
                String tagList = String.join("\n", tagsWithCounts);
                userStates.put(userId, State.AWAITING_TAG_FOR_FILTER);
                return "Выберите тег из списка:\n" + tagList + "\nВсе заметки";
            case "/edit_note":
            case ButtonLabels.EDIT_NOTE:
                return promptNoteSelection(userId);
            default:
                return "Неизвестная команда. Используйте кнопки.";
        }
    }


    private String showUserReminders(String login) throws SQLException {
        List<Reminder> reminders = reminderService.getUserReminders(login);
        if (reminders.isEmpty()) {
            return "У вас нет запланированных напоминаний.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            sb.append((i + 1)).append(". ").append(r.getText()).append(" — ")
                    .append(r.getReminderTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    .append("\n");
        }
        return sb + "\n\nВведите номер напоминания для действий (изменить/удалить).";
    }

    private String promptNoteToConvert(long userId) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        List<String> notes = noteService.getAllNotes(login);
        if (notes.isEmpty()) {
            return "Нет заметок для преобразования.";
        }
        String list = IntStream.range(0, notes.size())
                .mapToObj(i -> (i + 1) + ". " + notes.get(i))
                .collect(Collectors.joining("\n"));
        userStates.put(userId, State.AWAITING_NOTE_ID_FOR_REMINDER);
        return "Выберите номер заметки, чтобы сделать из неё напоминание:\n" + list;
    }

    private String handleEditNoteSelection(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if (isNumeric(input)) {
            int noteIndex = Integer.parseInt(input);
            Integer realNoteId = noteService.getNoteIdByIndex(login, noteIndex);
            if (realNoteId != null) {
                userPendingNoteId.put(userId, realNoteId);
                String text = noteService.getNoteTextById(login, realNoteId);
                List<String> tags = noteService.getTagsForNote(realNoteId);
                String tagStr = tags.isEmpty() ? "нет" : String.join(" ", tags);

                userStates.put(userId, State.AWAITING_ACTION_ON_NOTE);

                return String.format(
                        "Текст: %s\nТеги: %s\nВыберите действие:\n[Изменить текст]\n[Изменить теги]\n[Сделать напоминание]\n[Удалить заметку]",
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
                String text = noteService.getNoteTextById(login, noteId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            }
        }
        return "Неизвестная команда. Выберите действие.";
    }

    private String handleNoteTextUpdate(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        Integer noteId = userPendingNoteId.get(userId);
        if (noteId == null) {
            userStates.remove(userId);
            return "Ошибка: заметка не выбрана.";
        }
        noteService.updateNote(login, noteId, input);
        userStates.remove(userId);
        userPendingNoteId.remove(userId);
        return "Заметка обновлена!";
    }

    private String handleDeleteNoteSelection(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if (isNumeric(input)) {
            int userIndex = Integer.parseInt(input);
            Integer realId = noteService.getNoteIdByIndex(login, userIndex);
            if (realId != null) {
                String text = noteService.getNoteTextById(login, realId);
                userPendingNoteId.put(userId, realId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            } else {
                return "Заметки с таким номером не существует.";
            }
        }
        return "Введите корректный номер заметки.";
    }

    private String handleDeleteConfirmation(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if ("да".equalsIgnoreCase(input.trim())) {
            Integer delId = userPendingNoteId.get(userId);
            if (delId != null) {
                noteService.deleteNote(login, delId);
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

    private String handleTagFilter(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if ("Все заметки".equals(input)) {
            return showAllNotes(login);
        }
        String tag = input.trim().toLowerCase();
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        List<String> notes = noteService.getNotesByTag(login, tag);
        if (notes.isEmpty()) {
            return "Заметок с тегом " + tag + " не найдено.";
        }
        return String.join("\n", notes);
    }

    private String handleEditTagSelection(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if (isNumeric(input)) {
            int noteIndex = Integer.parseInt(input);
            Integer realNoteId = noteService.getNoteIdByIndex(login, noteIndex);
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

    private String handleTagUpdate(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        Integer noteId = userPendingNoteId.get(userId);
        if (noteId == null) {
            userStates.remove(userId);
            return "Ошибка: заметка не выбрана.";
        }

        String currentText = noteService.getNoteTextById(login, noteId);
        if (currentText == null) {
            userStates.remove(userId);
            userPendingNoteId.remove(userId);
            return "Заметка не найдена.";
        }

        List<String> newTags = input.trim().isEmpty() ? Collections.emptyList() : extractTagsFromText(input);
        String textWithoutTags = removeTagsFromText(currentText);
        String newText = newTags.isEmpty() ? textWithoutTags
                : (textWithoutTags + " " + String.join(" ", newTags)).trim();

        noteService.updateNote(login, noteId, newText);

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

        userStates.remove(userId);
        userPendingNoteId.remove(userId);
        return response.toString();
    }

    private String showAllNotes(String login) throws SQLException {
        List<String> notes = noteService.getAllNotes(login);
        if (notes.isEmpty()) {
            return "У вас пока нет заметок.";
        }
        return IntStream.range(0, notes.size())
                .mapToObj(i -> (i + 1) + ". " + notes.get(i))
                .collect(Collectors.joining("\n"));
    }

    private String promptNoteSelection(long userId) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        List<String> notes = noteService.getAllNotes(login);
        if (notes.isEmpty()) {
            return "Нет заметок.";
        }
        String list = IntStream.range(0, notes.size())
                .mapToObj(i -> (i + 1) + ". " + notes.get(i))
                .collect(Collectors.joining("\n"));
        userStates.put(userId, State.AWAITING_NOTE_ID_FOR_EDIT);
        return "Введите номер заметки для редактирования:" + "\n" + list;
    }

    private String handleNoteActionSelection(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        switch (input) {
            case "Изменить текст" -> {
                userStates.put(userId, State.AWAITING_NEW_TEXT_FOR_EDIT);
                Integer noteId = userPendingNoteId.get(userId);
                if (noteId == null) {
                    userStates.remove(userId);
                    return "Ошибка: заметка не выбрана.";
                }
                String current = noteService.getNoteTextById(login, noteId);
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
                String text = noteService.getNoteTextById(login, noteId);
                userStates.put(userId, State.AWAITING_DELETE_CONFIRMATION);
                return "Вы уверены, что хотите удалить заметку:\n«" + text + "»?\nОтветьте «да» или «нет».";
            }
            case "Сделать напоминание" -> {
                Integer noteId = userPendingNoteId.get(userId);
                if (noteId == null) {
                    userStates.remove(userId);
                    return "Ошибка: заметка не выбрана.";
                }
                String noteText = noteService.getNoteTextById(login, noteId);
                if (noteText == null) {
                    userStates.remove(userId);
                    return "Ошибка: текст заметки не найден.";
                }

                userPendingReminderText.put(userId, noteText);

                userStates.put(userId, State.AWAITING_REMINDER_TIME_FROM_NOTE);
                return "Вы выбрали заметку:\n\"" + noteText + "\"\n\nУкажите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ (например: 30.10.2025 14:00)";
            }
        }
        return "Неизвестная команда. Выберите действие из списка.";
    }

    private String handleSelectReminderForAction(long userId, String input) throws SQLException {
        String login = userPendingLogin.get(userId);
        if (login == null) {
            userStates.remove(userId);
            return "Ошибка: вы не авторизованы. Используйте /start.";
        }

        if (!isNumeric(input)) {
            return "Введите корректный номер напоминания.";
        }

        int reminderIndex = Integer.parseInt(input);
        List<Reminder> reminders = reminderService.getUserReminders(login);

        if (reminderIndex < 1 || reminderIndex > reminders.size()) {
            return "Напоминания с таким номером не существует.";
        }

        Reminder selectedReminder = reminders.get(reminderIndex - 1);
        userPendingReminderId.put(userId, selectedReminder.getId());

        userStates.put(userId, State.AWAITING_REMINDER_EDIT_ACTION);

        return "Текст: " + selectedReminder.getText() + "\n" +
                "Дата и время: " + selectedReminder.getReminderTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n" +
                "Выберите действие:\n[Изменить текст]\n[Изменить дату/время]\n[Удалить]";
    }

    private List<String> extractTagsFromText(String text) {
        List<String> tags = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[\\p{L}0-9_]+");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(new LinkedHashSet<>(tags));
    }

    private String removeTagsFromText(String text) {
        return text.replaceAll("#[\\p{L}0-9_]+", "").trim().replaceAll("\\s+", " ");
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