package org.example;

import org.example.scheduler.NotificationSender;

/**
 * DummyNotificationSender - in-memory реализация NotificationSender для тестов
 */
public class DummyNotificationSender implements NotificationSender {
    @Override
    public void sendTelegramNotification(Long userId, String message) {
        System.out.println("[DUMMY] Попытка отправить в Telegram пользователю " + userId + ": " + message);
    }

    @Override
    public void sendDiscordNotification(Long userId, String message) {
        System.out.println("[DUMMY] Попытка отправить в Discord пользователю " + userId + ": " + message);
    }
}
