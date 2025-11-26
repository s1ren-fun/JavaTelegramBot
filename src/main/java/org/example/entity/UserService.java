package org.example.entity;

import java.sql.SQLException;

/**
 * Интерфейс сервиса для управления пользователями.
 * <p>
 * Позволяет регистрировать пользователей, обновлять их платформенные идентификаторы,
 * а также получать информацию по логину.
 * </p>
 *
 * @since 1.1
 */
public interface UserService {
    /**
     * Регистрирует нового пользователя или обновляет существующего.
     *
     * @param login логин пользователя
     * @param telegramId идентификатор в Telegram (может быть null)
     * @param discordId идентификатор в Discord (может быть null)
     * @param timezone часовой пояс (если null, используется UTC)
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void registerUser(String login, Long telegramId, Long discordId, java.time.ZoneId timezone) throws SQLException;

    /**
     * Обновляет Telegram ID пользователя.
     *
     * @param login логин пользователя
     * @param telegramId идентификатор в Telegram
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void updateTelegramId(String login, Long telegramId) throws SQLException;

    /**
     * Обновляет Discord ID пользователя.
     *
     * @param login логин пользователя
     * @param discordId идентификатор в Discord
     * @throws SQLException если произошла ошибка при работе с БД
     */
    void updateDiscordId(String login, Long discordId) throws SQLException;

    /**
     * Получает пользователя по логину.
     *
     * @param login логин пользователя
     * @return объект пользователя или null, если не найден
     * @throws SQLException если произошла ошибка при работе с БД
     */
    User getUserByLogin(String login) throws SQLException;

    /**
     * Получает пользователя по Telegram ID.
     *
     * @param telegramId идентификатор в Telegram
     * @return объект пользователя или null, если не найден
     * @throws SQLException если произошла ошибка при работе с БД
     */
    User getUserByTelegramId(Long telegramId) throws SQLException;

    /**
     * Получает пользователя по Discord ID.
     *
     * @param discordId идентификатор в Discord
     * @return объект пользователя или null, если не найден
     * @throws SQLException если произошла ошибка при работе с БД
     */
    User getUserByDiscordId(Long discordId) throws SQLException;
}