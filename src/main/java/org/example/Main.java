package org.example;

import org.example.bots.TelegramBot;
import org.example.bots.DiscordBot;

public class Main {
    public static void main(String[] args) {
        Thread discordThread = new Thread(() -> {
            try {
                new DiscordBot().start();
            } catch (Exception e) {
                System.err.println("Discord bot crashed:");
                e.printStackTrace();
            }
        }, "DiscordBot-Thread");
        Thread telegramThread = new Thread(() -> {
            try {
                new TelegramBot().start();
            } catch (Exception e) {
                System.err.println("Telegram bot crashed:");
                e.printStackTrace();
            }
        }, "TelegramBot-Thread");
        discordThread.start();
        telegramThread.start();
        try {
            discordThread.join();
            telegramThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}