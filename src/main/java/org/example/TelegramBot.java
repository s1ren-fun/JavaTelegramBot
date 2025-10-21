package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Основной класс Telegram-бота, реализующий взаимодействие с Telegram Bot API.
 * <p>
 * Данный класс наследуется от {@link TelegramLongPollingBot} и обрабатывает
 * входящие сообщения от пользователей, делегируя логику обработки команд
 * экземпляру {@link LogicBot}. Также отвечает за формирование и отправку
 * ответных сообщений с интерактивной клавиатурой.
 * </p>
 * <p>
 * Основные обязанности:
 * </p>
 * <ul>
 *     <li>Получение и обработка входящих текстовых сообщений.</li>
 *     <li>Передача текста сообщения и идентификатора пользователя в {@link LogicBot}.</li>
 *     <li>Формирование ответного сообщения с кнопками быстрого доступа.</li>
 *     <li>Отправка ответа пользователю через Telegram Bot API.</li>
 * </ul>
 *
 * @since 1.0
 */
public class TelegramBot extends TelegramLongPollingBot {

    /**
     * Экземпляр логического обработчика команд, отвечающий за бизнес-логику бота.
     */
    private final LogicBot logicBot = new LogicBot();

    /**
     * Обрабатывает входящие обновления от Telegram API.
     * <p>
     * Метод вызывается автоматически при получении нового сообщения.
     * Поддерживается только обработка текстовых сообщений.
     * Нетекстовые обновления (например, стикеры, фото и т.д.) игнорируются.
     * </p>
     *
     * @param update объект, содержащий информацию о новом событии (сообщении, callback и т.д.)
     *               от Telegram. Обрабатывается только если содержит текстовое сообщение.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();

            String response = logicBot.handleCommand(userId, text);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);

            setButtons(message);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Настраивает интерактивную клавиатуру (reply keyboard) для отправляемого сообщения.
     * <p>
     * Добавляет стандартный набор кнопок для удобного взаимодействия с ботом:
     * «Новая заметка», «Удалить заметку», «Список заметок», «Изменить заметку».
     * </p>
     * <p>
     * Метод синхронизирован для обеспечения потокобезопасности при одновременной отправке
     * нескольких сообщений (хотя в текущей реализации это маловероятно).
     * </p>
     *
     * @param sendMessage объект {@link SendMessage}, к которому будет прикреплена клавиатура
     */
    public synchronized void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(new KeyboardButton(LogicBot.ButtonLabels.NEW_NOTE));
        firstRow.add(new KeyboardButton(LogicBot.ButtonLabels.DELETE_NOTE));
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(new KeyboardButton(LogicBot.ButtonLabels.NOTES_LIST));
        secondRow.add(new KeyboardButton(LogicBot.ButtonLabels.EDIT_NOTE));
        keyboard.add(firstRow);
        keyboard.add(secondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    /**
     * Возвращает имя пользователя Telegram-бота.
     * @return строка с именем бота
     */
    @Override
    public String getBotUsername() {
        return "JavaVoice";
    }

    /**
     * Возвращает токен
     * <p>Токен должен быть заранее установлен в системное свойство {@code TelegramToken}.</p>
     * @return строка с токеном бота
     */
    @Override
    public String getBotToken() {
        return System.getProperty("TelegramToken");
    }
}