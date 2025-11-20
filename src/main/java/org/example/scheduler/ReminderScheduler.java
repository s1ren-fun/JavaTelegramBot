package org.example.scheduler;

import org.example.entity.Reminder;
import org.example.entity.User;
import org.example.entity.ReminderService;
import org.example.bots.TelegramBot;
import org.example.bots.DiscordBot;
import org.example.entity.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Планировщик напоминаний.
 * <p>
 * Проверяет каждую минуту, есть ли напоминания к отправке, и отправляет их в нужную платформу.
 * </p>
 *
 * @since 1.1
 */
public class ReminderScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ReminderService reminderService;
    private final UserService userService;
    private final TelegramBot telegramBot;
    private final DiscordBot discordBot;

    /**
     * Создаёт экземпляр планировщика с указанными зависимостями.
     *
     * @param reminderService сервис для работы с напоминаниями
     * @param userService сервис для работы с пользователями
     * @param telegramBot экземпляр Telegram-бота для отправки уведомлений
     * @param discordBot экземпляр Discord-бота для отправки уведомлений
     */
    public ReminderScheduler(ReminderService reminderService, UserService userService, TelegramBot telegramBot, DiscordBot discordBot) {
        this.reminderService = reminderService;
        this.userService = userService;
        this.telegramBot = telegramBot;
        this.discordBot = discordBot;
    }

    /**
     * Запускает планировщик.
     * <p>
     * Планировщик проверяет наличие просроченных напоминаний каждую минуту.
     * </p>
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 0, 1, TimeUnit.MINUTES);
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
    private void checkAndSendReminders() {
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
                    SendMessage msg = new SendMessage();
                    msg.setChatId(String.valueOf(user.getTelegramId()));
                    msg.setText(message);
                    telegramBot.execute(msg);
                    sent = true;
                } catch (Exception e) {
                    System.err.println("Ошибка отправки в Telegram пользователю " + user.getTelegramId() + ": " + e.getMessage());
                }
            }

            if (user.getDiscordId() != null) {
                System.out.println("Планировщик: Отправка напоминания в Discord пользователю " + user.getDiscordId());
                try {
                    discordBot.sendPrivateMessage(user.getDiscordId(), message);
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