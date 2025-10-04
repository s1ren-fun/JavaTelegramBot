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
public class TelegramBot extends TelegramLongPollingBot {

    private final LogicBot logicBot = new LogicBot(); // Один инстанс на всё время работы

    /**
     * Обрабатывает входящие сообщения от пользователей.
     * @param update объект {@link Update}, содержащий данные нового сообщения
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            long chatId = message.getChatId();

            String response = logicBot.handleCommand(text);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(response);

            // Добавляем клавиатуру (опционально — можно вызывать только при определённых условиях)
            setButtons(sendMessage);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Настраивает клавиатуру для сообщения.
     */
    public synchronized void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(new KeyboardButton("Новая заметка")); // Обычные кавычки!
        firstRow.add(new KeyboardButton("Удалить заметку")); // Закрыта скобка, обычные кавычки

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add(new KeyboardButton("Список заметок")); // Закрыта скобка, обычные кавычки


        keyboard.add(firstRow);
        keyboard.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    @Override
    public String getBotUsername() {
        return "JavaVoice";
    }

    @Override
    public String getBotToken() {
        // ⚠️ В реальном проекте токен НЕЛЬЗЯ хранить в коде!
        // Используйте System.getenv("TELEGRAM_BOT_TOKEN") или application.properties
        return "8295616955:AAHMn1KFNqG2gYxpz0wPK4wrVfhBmmvIhkM";
    }
}