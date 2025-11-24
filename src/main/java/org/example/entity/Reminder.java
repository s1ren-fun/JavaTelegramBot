package org.example.entity;

import java.time.LocalDateTime;

/**
 * Сущность напоминания.
 * <p>
 * Содержит информацию о тексте напоминания, времени отправки и пользователе.
 * </p>
 *
 * @since 1.1
 */
public class Reminder {
    private int id;
    private String login;
    private String text;
    private LocalDateTime reminderTime;

    /**
     * Создаёт экземпляр напоминания с указанными параметрами.
     *
     * @param id идентификатор напоминания
     * @param login логин пользователя, которому принадлежит напоминание
     * @param text текст напоминания
     * @param reminderTime время, когда должно быть отправлено напоминание
     */
    public Reminder(int id, String login, String text, LocalDateTime reminderTime) {
        this.id = id;
        this.login = login;
        this.text = text;
        this.reminderTime = reminderTime;
    }

    /**
     * Возвращает идентификатор напоминания.
     *
     * @return идентификатор
     */
    public int getId() { return id; }

    /**
     * Возвращает логин пользователя, которому принадлежит напоминание.
     *
     * @return логин пользователя
     */
    public String getLogin() { return login; }

    /**
     * Возвращает текст напоминания.
     *
     * @return текст напоминания
     */
    public String getText() { return text; }

    /**
     * Возвращает время, когда должно быть отправлено напоминание.
     *
     * @return время отправки напоминания
     */
    public LocalDateTime getReminderTime() { return reminderTime; }

    /**
     * Устанавливает идентификатор напоминания.
     *
     * @param id идентификатор
     */
    public void setId(int id) { this.id = id; }

    /**
     * Устанавливает логин пользователя, которому принадлежит напоминание.
     *
     * @param login логин пользователя
     */
    public void setLogin(String login) { this.login = login; }

    /**
     * Устанавливает текст напоминания.
     *
     * @param text текст
     */
    public void setText(String text) { this.text = text; }

    /**
     * Устанавливает время, когда должно быть отправлено напоминание.
     *
     * @param reminderTime время отправки напоминания
     */
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
}