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

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Ты написал: " + text);

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
