package org.example.scheduler;

import org.example.bots.DiscordBot;
import org.example.bots.TelegramBot;

/**
 * Реализация {@link NotificationSender}, которая делегирует отправку
 * соответствующему боту в зависимости от типа платформы.
 * <p>
 * Используется для объединения {@link TelegramBot} и {@link DiscordBot}
 * в единую точку отправки уведомлений для {@link ReminderScheduler}.
 * </p>
 *
 * @since 1.1
 */
public class CompositeNotificationSender implements NotificationSender {

    private final TelegramBot telegramBot;
    private final DiscordBot discordBot;

    public CompositeNotificationSender(TelegramBot telegramBot, DiscordBot discordBot) {
        this.telegramBot = telegramBot;
        this.discordBot = discordBot;
    }

    @Override
    public void sendTelegramNotification(Long userId, String message) {
        telegramBot.sendTelegramNotification(userId, message);
    }

    @Override
    public void sendDiscordNotification(Long userId, String message) {
        discordBot.sendDiscordNotification(userId, message);
    }
}