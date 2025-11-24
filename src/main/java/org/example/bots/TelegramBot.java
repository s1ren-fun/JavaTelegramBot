package org.example.bots;

import org.example.entity.Platform;
import org.example.logic.BotLogic;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Класс, реализующий TelegramLongPollingBot и связывающий BotLogic с Telegram API.
 * Теперь поддерживает inline-кнопки (через InlineButtonHandler).
 */
public class TelegramBot extends TelegramLongPollingBot {

    /**
     * Экземпляр логики бота, используемый для обработки команд и состояний.
     */
    private final BotLogic logicBot = new BotLogic();

    /**
     * Обработчик inline-кнопок (в т.ч. кнопки Отмена).
     */
    private final InlineButtonHandler inlineButtonHandler = new InlineButtonHandler();


    /**
     * Инициализирует и регистрирует бота в Telegram API.
     * <p>Метод создаёт экземпляр {@link TelegramBotsApi} и регистрирует текущий бот. Для удержания
     * потока работы приложения используется {@link java.util.concurrent.CountDownLatch}
     * (его задача — не позволить main() завершиться, чтобы бот продолжил работать).
     */
    public void start() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update == null) return;

            if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                inlineButtonHandler.handleCallback(update.getCallbackQuery(), this, logicBot);
                return;
            }

            if (update.getMessage() == null || update.getMessage().getText() == null) return;

            String chatId = String.valueOf(update.getMessage().getChatId());
            long userId = Long.parseLong(String.valueOf(update.getMessage().getFrom().getId()));
            String text = update.getMessage().getText();
            String response = logicBot.handleCommand(userId, text, Platform.TELEGRAM);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);
            setButtons(message, userId);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Устанавливает клавиатуру или inline-кнопки (в зависимости от состояния пользователя).
     * Для некоторых состояний (например, ожидание ввода текста или выбор действия над заметкой)
     * мы используем inline-кнопки (в том числе кнопку "Отмена"), которые появляются непосредственно
     * под сообщением и обрабатываются в {@link InlineButtonHandler}.
     *
     * @param sendMessage сообщение, к которому нужно прикрепить разметку
     * @param userId      идентификатор пользователя (для определения состояния)
     */
    public synchronized void setButtons(SendMessage sendMessage, long userId) {
        BotLogic.State userState = logicBot.getUserState(userId);
        if (userState == BotLogic.State.AWAITING_ACTION_ON_NOTE || userState == BotLogic.State.AWAITING_NOTE_TEXT) {
            sendMessage.setReplyMarkup(inlineButtonHandler.createMarkupForState(userState));
            return;
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        if (userState == BotLogic.State.AWAITING_NOTE_ID_FOR_REMINDER) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(BotLogic.ButtonLabels.CANCEL));
            keyboard.add(row);
        } else if (userState == BotLogic.State.AWAITING_REMINDER_TEXT) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(BotLogic.ButtonLabels.CANCEL));
            keyboard.add(row);
        } else if (userState == BotLogic.State.AWAITING_REMINDER_TIME) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(BotLogic.ButtonLabels.CANCEL));
            keyboard.add(row);
        } else {
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton(BotLogic.ButtonLabels.NEW_NOTE));
            row1.add(new KeyboardButton(BotLogic.ButtonLabels.NOTES_LIST));
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton(BotLogic.ButtonLabels.FILTER_BY_TAG));
            row2.add(new KeyboardButton(BotLogic.ButtonLabels.EDIT_NOTE));
            KeyboardRow row3 = new KeyboardRow();
            row3.add(new KeyboardButton(BotLogic.ButtonLabels.NEW_REMINDER));
            row3.add(new KeyboardButton(BotLogic.ButtonLabels.MY_REMINDERS));
            keyboard.add(row1);
            keyboard.add(row2);
            keyboard.add(row3);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);
    }

    /**
     * Удобный метод отправки сообщения с прикреплением клавиатуры, используя существующий setButtons.
     *
     * @param chatId чат, куда отправить
     * @param text   текст сообщения
     * @param userId id пользователя (нужен для выбора клавиатуры)
     */
    public void sendMessageWithButtons(String chatId, String text, long userId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        setButtons(msg, userId);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Возвращает имя пользователя бота, используемое при регистрации.
     * @return имя пользователя бота
     */
    @Override
    public String getBotUsername() {
        return "JavaVoice";
    }

    /**
     * Возвращает токен пользователя бота.
     * @return токен бота, извлекаемый из системного свойства {@code TOKEN_TELEGRAM}
     */
    @Override
    public String getBotToken() {
        return System.getProperty("TOKEN_TELEGRAM");
    }
}
