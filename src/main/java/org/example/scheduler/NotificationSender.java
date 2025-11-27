package org.example.scheduler;

/**
 * Интерфейс для отправки уведомлений пользователю.
 * <p>
 * Позволяет планировщику напоминаний быть независимым от конкретных реализаций ботов (Telegram, Discord).
 * </p>
 *
 * @since 1.1
 */
public interface NotificationSender {

    /**
     * Отправляет текстовое сообщение пользователю в Telegram.
     *
     * @param userId идентификатор пользователя в Telegram
     * @param message текст сообщения
     */
    void sendTelegramNotification(Long userId, String message);

    /**
     * Отправляет текстовое сообщение пользователю в Discord.
     *
     * @param userId идентификатор пользователя в Discord
     * @param message текст сообщения
     */
    void sendDiscordNotification(Long userId, String message);
}