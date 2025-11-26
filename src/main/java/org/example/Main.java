package org.example;

import org.example.bots.TelegramBot;
import org.example.bots.DiscordBot;
import org.example.scheduler.ReminderScheduler;
import org.example.scheduler.CompositeNotificationSender;
import org.example.entity.*;

/**
 * Главный класс приложения.
 * <p>
 * Запускает Telegram-бота, Discord-бота и планировщик напоминаний в отдельных потоках.
 * </p>
 */
public class Main {
    /**
     * Точка входа в приложение.
     * <p>
     * Создаёт и запускает:
     * </p>
     * <ul>
     *     <li>Telegram-бота</li>
     *     <li>Discord-бота</li>
     *     <li>Планировщик напоминаний</li>
     * </ul>
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        ReminderService reminderService = new ReminderDatabaseService();
        UserService userService = new UserDatabaseService();
        TelegramBot telegramBot = new TelegramBot();
        DiscordBot discordBot = new DiscordBot();
        CompositeNotificationSender notificationSender = new CompositeNotificationSender(telegramBot, discordBot);
        ReminderScheduler scheduler = new ReminderScheduler(reminderService, userService, notificationSender);
        Thread discordThread = new Thread(() -> {
            try {
                discordBot.start();
            } catch (Exception e) {
                System.err.println("Discord bot crashed:");
                e.printStackTrace();
            }
        }, "DiscordBot-Thread");

        Thread telegramThread = new Thread(() -> {
            try {
                telegramBot.start();
            } catch (Exception e) {
                System.err.println("Telegram bot crashed:");
                e.printStackTrace();
            }
        }, "TelegramBot-Thread");

        Thread schedulerThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                scheduler.start();
            } catch (Exception e) {
                System.err.println("Reminder scheduler crashed:");
                e.printStackTrace();
            }
        }, "Scheduler-Thread");

        discordThread.start();
        telegramThread.start();
        schedulerThread.start();

        try {
            discordThread.join();
            telegramThread.join();
            schedulerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}