package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot{

    @Override
    public void onUpdateReceived(Update update) {
            if(update.hasMessage() && update.getMessage().hasText()){
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String response;

            switch (text){
                case "/start":
                case "/help":
                    response = "Привет! Я твой Java-бот 🤖. Я готов повторять за тобой";
                    break;
                default:
                    response = "Ты написал: " + text;
            }
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

        @Override
    public String getBotUsername() {
        return "JavaVoce";
    }

    @Override
    public String getBotToken() {
        return "8295616955:AAHMn1KFNqG2gYxpz0wPK4wrVfhBmmvIhkM";
    }
}
