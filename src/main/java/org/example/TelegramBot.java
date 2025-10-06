package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для основного взаимодействия с API Telegram
 * <p>Обрабатывает входящие обновления (сообщения), перенаправляя их в {@link LogicBot} для получения ответа.</p>
 * <p>Основные обязанности</p>
 * <ul>
 *     <li>Получение входящих сообщений от пользователей.</li>
 *     <li>Передача текста в {@link LogicBot} для дальнейшей обработки</li>
 *     <li>Формирование и отправка ответа пользователю</li>
 * </ul>
 * @since 1.0
 */
public class TelegramBot extends TelegramLongPollingBot{

    private LogicBot logicBot = new LogicBot();
    /**
     * Обрабатывает входящие сообщения от пользователей.
     * @param update объект {@link Update}, содержащий данные нового сообщения
     */
    @Override
    public void onUpdateReceived(Update update) {
            if(update.hasMessage() && update.getMessage().hasText()){
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                long userId = update.getMessage().getFrom().getId();

                String response = logicBot.handleCommand(userId,text);

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

    public synchronized void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(new KeyboardButton("Новая заметка"));
        firstRow.add(new KeyboardButton("Удалить заметку"));

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(new KeyboardButton("Список заметок"));

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
