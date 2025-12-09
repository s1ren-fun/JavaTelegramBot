package org.example.scheduler;

import org.example.entity.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Планировщик напоминаний.
 * <p>
 * Проверяет каждую минуту, есть ли напоминания к отправке, и отправляет их в нужную платформу
 * через {@link NotificationSender}.
 * </p>
 *
 * @since 1.2
 */
public class ReminderScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ReminderService reminderService;
    private final UserService userService;
    private final NotificationSender notificationSender;

    /**
     * Создаёт экземпляр планировщика с указанными зависимостями.
     *
     * @param reminderService сервис для работы с напоминаниями
     * @param userService сервис для работы с пользователями
     * @param notificationSender интерфейс для отправки уведомлений
     */
    public ReminderScheduler(ReminderService reminderService, UserService userService, NotificationSender notificationSender) {
        this.reminderService = reminderService;
        this.userService = userService;
        this.notificationSender = notificationSender;
    }

    /**
     * Запускает планировщик.
     * <p>
     * Планировщик проверяет наличие просроченных напоминаний каждую минуту.
     * </p>
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 0, 20, TimeUnit.SECONDS);
    }

    /**
     * Останавливает планировщик.
     * <p>
     * Прекращает выполнение задач и освобождает ресурсы.
     * </p>
     */
    public void stop() {
        scheduler.shutdown();
    }

    /**
     * Проверяет и отправляет все просроченные напоминания.
     */
    public void checkAndSendReminders() {
        try {
            List<Reminder> dueReminders = reminderService.getDueReminders(LocalDateTime.now());
            for (Reminder reminder : dueReminders) {
                sendReminder(reminder);
                reminderService.deleteReminder(reminder.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет напоминание пользователю в соответствующую платформу.
     *
     * @param reminder объект напоминания для отправки
     */
    private void sendReminder(Reminder reminder) {
        try {
            User user = userService.getUserByLogin(reminder.getLogin());
            if (user == null) {
                System.err.println("Ошибка: пользователь с логином " + reminder.getLogin() + " не найден для напоминания.");
                return;
            }

            String message = "⏰ Напоминание!\nНе забудьте: " + reminder.getText();

            boolean sent = false;

            if (user.getTelegramId() != null) {
                System.out.println("Планировщик: Отправка напоминания в Telegram пользователю " + user.getTelegramId());
                try {
                    notificationSender.sendTelegramNotification(user.getTelegramId(), message);
                    sent = true;
                } catch (Exception e) {
                    System.err.println("Ошибка отправки в Telegram пользователю " + user.getTelegramId() + ": " + e.getMessage());
                }
            }

            if (user.getDiscordId() != null) {
                System.out.println("Планировщик: Отправка напоминания в Discord пользователю " + user.getDiscordId());
                try {
                    notificationSender.sendDiscordNotification(user.getDiscordId(), message);
                    sent = true;
                } catch (Exception e) {
                    System.err.println("Ошибка отправки в Discord пользователю " + user.getDiscordId() + ": " + e.getMessage());
                }
            }

            if (!sent) {
                System.err.println("Предупреждение: Напоминание для логина " + reminder.getLogin() + " не было отправлено никуда: нет привязанных ID.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}