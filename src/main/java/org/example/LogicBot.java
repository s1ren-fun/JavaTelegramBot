package org.example;

public class LogicBot {
    public String handleCommand(String text){
        String response;

        switch (text){
            case "/start":
            case "/help":
                response = "Привет! Я твой Java-бот 🤖. Я готов повторять за тобой";
                break;
            default:
                response = "Ты написал: " + text;
        }
        return response;
    }
}
