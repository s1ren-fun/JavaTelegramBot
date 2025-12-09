package org.example.bots;

import org.example.entity.Platform;
import org.example.logic.BotLogic;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик inline-кнопок (в частности — кнопки "Отмена").
 * <p>
 * Этот класс отвечает за создание InlineKeyboardMarkup для конкретных состояний
 * и обработку CallbackQuery — он вызывает BotLogic для получения результата
 * (без импортов Telegram API в BotLogic), а затем выполняет отправку / редактирование
 * сообщений через Telegram API (путём вызова методов TelegramBot).
 * </p>
 */
public class InlineButtonHandler {
    public static final String DATA_CANCEL = "INLINE_CANCEL";
    public static final String DATA_DELETE_NOTE = "INLINE_DELETE_NOTE";
    public static final String DATA_CONVERT_TO_REMINDER = "INLINE_CONVERT_TO_REMINDER";
    public static final String DATA_EDIT_NOTE = "INLINE_EDIT_NOTE";
    public static final String DATA_EDIT_TAGS = "INLINE_EDIT_TAGS";
    public static final String DATA_MANAGE_ACCESS = "INLINE_MANAGE_ACCESS";
    public static final String DATA_GRANT_READ = "INLINE_GRANT_READ";
    public static final String DATA_ADD_USER = "INLINE_ADD_USER";
    public static final String DATA_REMOVE_USER = "INLINE_REMOVE_USER";


    /**
     * Создаёт InlineKeyboardMarkup для указанного состояния пользователя.
     * Поддерживаемые состояния:
     * - AWAITING_ACTION_ON_NOTE: три кнопки — Удалить заметку, Сделать напоминание, Отмена
     * - AWAITING_NOTE_TEXT: одна кнопка — Отмена
     *
     * @param state состояние пользователя из BotLogic
     * @return InlineKeyboardMarkup для прикрепления к сообщению
     */
    public InlineKeyboardMarkup createMarkupForState(org.example.logic.BotLogic.State state) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (state == BotLogic.State.AWAITING_ACTION_ON_NOTE) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton delete = new InlineKeyboardButton();
            delete.setText(BotLogic.ButtonLabels.DELETE_NOTE);
            delete.setCallbackData(DATA_DELETE_NOTE);
            row.add(delete);

            InlineKeyboardButton updateText = new InlineKeyboardButton();
            updateText.setText(BotLogic.ButtonLabels.EDIT_NOTE_T);
            updateText.setCallbackData(DATA_EDIT_NOTE);
            row.add(updateText);

            InlineKeyboardButton updateTags = new InlineKeyboardButton();
            updateTags.setText(BotLogic.ButtonLabels.EDIT_TAGS);
            updateTags.setCallbackData(DATA_EDIT_TAGS);
            row.add(updateTags);

            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton convert = new InlineKeyboardButton();
            convert.setText(BotLogic.ButtonLabels.CONVERT_TO_REMINDER);
            convert.setCallbackData(DATA_CONVERT_TO_REMINDER);
            row2.add(convert);

            InlineKeyboardButton cancel = new InlineKeyboardButton();
            cancel.setText(BotLogic.ButtonLabels.CANCEL);
            cancel.setCallbackData(DATA_CANCEL);
            row2.add(cancel);

            List<InlineKeyboardButton> accessRow = new ArrayList<>();
            InlineKeyboardButton manageAccess = new InlineKeyboardButton();
            manageAccess.setText(BotLogic.ButtonLabels.MANAGE_ACCESS);
            manageAccess.setCallbackData(DATA_MANAGE_ACCESS);
            accessRow.add(manageAccess);

            InlineKeyboardButton grantRead = new InlineKeyboardButton();
            grantRead.setText(BotLogic.ButtonLabels.GRANT_READ_PERMISSION);
            grantRead.setCallbackData(DATA_GRANT_READ);
            accessRow.add(grantRead);

            rows.add(row);
            rows.add(row2);
            rows.add(accessRow);
        }
        else if (state == BotLogic.State.AWAITING_SHARED_NOTE_ACCESS_ACTION) {
            try {
                List<InlineKeyboardButton> r1 = new ArrayList<>();
                InlineKeyboardButton addUser = new InlineKeyboardButton();
                addUser.setText(BotLogic.ButtonLabels.ADD_USER);
                addUser.setCallbackData(DATA_ADD_USER);
                r1.add(addUser);

                InlineKeyboardButton removeUser = new InlineKeyboardButton();
                removeUser.setText(BotLogic.ButtonLabels.REMOVE_USER);
                removeUser.setCallbackData(DATA_REMOVE_USER);
                r1.add(removeUser);

                rows.add(r1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton cancel = new InlineKeyboardButton();
            cancel.setText(BotLogic.ButtonLabels.CANCEL);
            cancel.setCallbackData(DATA_CANCEL);
            row.add(cancel);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Обрабатывает CallbackQuery — вызывает BotLogic с соответствующей текстовой
     * командой (например, "Отмена"), отвечает на CallbackQuery и удаляет inline-разметку
     * у сообщения, затем отправляет итоговый ответ пользователю через TelegramBot.
     *
     * @param callbackQuery пришедший CallbackQuery
     * @param bot           экземпляр TelegramBot (для отправки сообщений)
     * @param logicBot      экземпляр BotLogic (чтобы передать результат логике)
     */
    public void handleCallback(CallbackQuery callbackQuery, TelegramBot bot, BotLogic logicBot) {
        if (callbackQuery == null || callbackQuery.getData() == null) return;
        String data = callbackQuery.getData();
        String mappedLabel = null;

        switch (data) {
            case DATA_CANCEL -> mappedLabel = BotLogic.ButtonLabels.CANCEL;
            case DATA_DELETE_NOTE -> mappedLabel = BotLogic.ButtonLabels.DELETE_NOTE;
            case DATA_CONVERT_TO_REMINDER -> mappedLabel = BotLogic.ButtonLabels.CONVERT_TO_REMINDER;
            case DATA_EDIT_NOTE -> mappedLabel = BotLogic.ButtonLabels.EDIT_NOTE;
            case DATA_EDIT_TAGS ->  mappedLabel = BotLogic.ButtonLabels.EDIT_TAGS;
            case DATA_ADD_USER -> mappedLabel = BotLogic.ButtonLabels.ADD_USER;
            case DATA_REMOVE_USER -> mappedLabel = BotLogic.ButtonLabels.REMOVE_USER;
            case DATA_MANAGE_ACCESS ->  mappedLabel = BotLogic.ButtonLabels.MANAGE_ACCESS;
            case DATA_GRANT_READ ->  mappedLabel = BotLogic.ButtonLabels.GRANT_READ_PERMISSION;
        }

        if (mappedLabel == null) {
            return;
        }

        long userId = callbackQuery.getFrom().getId();
        String chatId = String.valueOf(callbackQuery.getMessage().getChatId());
        String response;
        try {
            response = logicBot.handleCommand(userId, mappedLabel, Platform.TELEGRAM);
        } catch (Exception e) {
            response = "Ошибка при обработке действия.";
            e.printStackTrace();
        }

        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText("Выполняется...");
            bot.execute(answer);

            EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
            edit.setChatId(chatId);
            edit.setMessageId(callbackQuery.getMessage().getMessageId());
            edit.setReplyMarkup(null);
            bot.execute(edit);

            bot.sendMessageWithButtons(chatId, response, userId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

