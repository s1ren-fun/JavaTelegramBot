package org.example.entity;

import java.time.ZoneId;

/**
 * Сущность пользователя.
 * <p>
 * Хранит логин, идентификаторы в Telegram и Discord, а также часовой пояс.
 * </p>
 *
 * @since 1.1
 */
public class User {
    private String login;
    private Long telegramId;
    private Long discordId;
    private ZoneId timezone;

    /**
     * Создаёт экземпляр пользователя с указанными параметрами.
     *
     * @param login логин пользователя
     * @param telegramId идентификатор в Telegram (может быть null)
     * @param discordId идентификатор в Discord (может быть null)
     * @param timezone часовой пояс (если null, используется UTC)
     */
    public User(String login, Long telegramId, Long discordId, ZoneId timezone) {
        this.login = login;
        this.telegramId = telegramId;
        this.discordId = discordId;
        this.timezone = timezone != null ? timezone : ZoneId.of("UTC");
    }

    /**
     * Создаёт экземпляр пользователя с логином и часовым поясом по умолчанию (UTC).
     *
     * @param login логин пользователя
     */
    public User(String login) {
        this(login, null, null, ZoneId.of("UTC"));
    }

    /**
     * Возвращает логин пользователя.
     *
     * @return логин
     */
    public String getLogin() { return login; }

    /**
     * Возвращает идентификатор пользователя в Telegram.
     *
     * @return идентификатор Telegram или null
     */
    public Long getTelegramId() { return telegramId; }

    /**
     * Возвращает идентификатор пользователя в Discord.
     *
     * @return идентификатор Discord или null
     */
    public Long getDiscordId() { return discordId; }

    /**
     * Возвращает часовой пояс пользователя.
     *
     * @return часовой пояс
     */
    public ZoneId getTimezone() { return timezone; }

    /**
     * Устанавливает идентификатор пользователя в Telegram.
     *
     * @param telegramId идентификатор Telegram
     */
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    /**
     * Устанавливает идентификатор пользователя в Discord.
     *
     * @param discordId идентификатор Discord
     */
    public void setDiscordId(Long discordId) { this.discordId = discordId; }

    /**
     * Устанавливает часовой пояс пользователя.
     *
     * @param timezone часовой пояс
     */
    public void setTimezone(ZoneId timezone) { this.timezone = timezone; }
}