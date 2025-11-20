package org.example.entity;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Интерфейс сервиса для управления напоминаниями.
 * <p>
 * Позволяет создавать, обновлять, удалять и получать напоминания.
 * </p>
 *
 * @since 1.1
 */
public interface ReminderService {
    /**
     * Добавляет новое напоминание.
     *
     * @param login логин пользователя
     * @param text текст напоминания
     * @param time дата и время отправки
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void addReminder(String login, String text, LocalDateTime time) throws SQLException;

    /**
     * Получает все напоминания пользователя.
     *
     * @param login логин пользователя
     * @return список напоминаний
     * @throws SQLException если произошла ошибка при работе с БД
     */
    List<Reminder> getUserReminders(String login) throws SQLException;

    /**
     * Обновляет текст и время напоминания.
     *
     * @param id идентификатор напоминания
     * @param text новый текст
     * @param time новое время
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void updateReminder(int id, String text, LocalDateTime time) throws SQLException;

    /**
     * Удаляет напоминание по идентификатору.
     *
     * @param id идентификатор напоминания
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void deleteReminder(int id) throws SQLException;

    /**
     * Возвращает список напоминаний, время которых наступило.
     *
     * @param now текущее время
     * @return список напоминаний
     * @throws SQLException если произошла ошибка при работе с БД
     */
    List<Reminder> getDueReminders(LocalDateTime now) throws SQLException;
}