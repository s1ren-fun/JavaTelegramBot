package org.example;

import org.example.entity.User;
import org.example.entity.UserService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MockUserService — in-memory реализация UserService для тестов.
 */
public class MockUserService implements UserService {
    private class UserEntry {
        String login;
        Long telegramId;
        Long discordId;
        java.time.ZoneId timezone;

        UserEntry(String login, Long telegramId, Long discordId, java.time.ZoneId timezone) {
            this.login = login;
            this.telegramId = telegramId;
            this.discordId = discordId;
            this.timezone = timezone != null ? timezone : java.time.ZoneId.of("UTC");
        }
    }

    private final Map<String, MockUserService.UserEntry> users = new HashMap<>();

    @Override
    public void registerUser(String login, Long telegramId, Long discordId, java.time.ZoneId timezone) {
        users.put(login, new MockUserService.UserEntry(login, telegramId, discordId, timezone));
    }

    @Override
    public void updateTelegramId(String login, Long telegramId) {
        MockUserService.UserEntry u = users.get(login);
        if (u != null) u.telegramId = telegramId;
    }

    @Override
    public void updateDiscordId(String login, Long discordId) {
        UserEntry u = users.get(login);
        if (u != null) u.discordId = discordId;
    }

    @Override
    public User getUserByLogin(String login) {
        MockUserService.UserEntry entry = users.get(login);
        if (entry == null) return null;
        return new User(entry.login, entry.telegramId, entry.discordId, entry.timezone);
    }

    @Override
    public User getUserByTelegramId(Long telegramId) throws SQLException {
        MockUserService.UserEntry entry = users.get(telegramId);
        if (entry == null) return null;
        return new User(entry.login, entry.telegramId, entry.discordId, entry.timezone);
    }

    @Override
    public User getUserByDiscordId(Long discordId) throws SQLException {
        MockUserService.UserEntry entry = users.get(discordId);
        if (entry == null) return null;
        return new User(entry.login, entry.telegramId, entry.discordId, entry.timezone);
    }
}