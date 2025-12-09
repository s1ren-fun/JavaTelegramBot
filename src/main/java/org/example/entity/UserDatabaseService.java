package org.example.entity;

import java.sql.*;
import java.time.ZoneId;

/**
 * Реализация {@link UserService} с использованием SQLite.
 *
 * @since 1.1
 */
public class UserDatabaseService implements UserService {
    private final String dbUrl;

    /**
     * Создаёт экземпляр сервиса с указанным URL базы данных и автоматически
     * инициализирует структуру базы (создаёт таблицу {@code users}, если она отсутствует).
     *
     * @param dbUrl URL подключения к базе данных в формате JDBC (например, {@code "jdbc:sqlite:notes.db"})
     * @throws RuntimeException если произошла ошибка при инициализации базы данных
     */
    public UserDatabaseService(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }

    /**
     * Конструктор по умолчанию.
     * <p>
     * Использует локальный файл базы данных {@code notes.db} в текущей рабочей директории.
     * </p>
     */
    public UserDatabaseService() {
        this("jdbc:sqlite:notes.db");
    }

    /**
     * Инициализирует базу данных: создаёт таблицу {@code users}, если она отсутствует.
     * <p>
     * Структура таблицы:
     * </p>
     * <ul>
     *     <li>{@code users}: {@code login} (PK), {@code telegram_id}, {@code discord_id}, {@code timezone}</li>
     * </ul>
     *
     * @throws RuntimeException если произошла ошибка SQL при создании таблицы
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    login TEXT PRIMARY KEY,
                    telegram_id BIGINT,
                    discord_id BIGINT,
                    timezone TEXT DEFAULT 'UTC'
                );
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsers);
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации таблицы пользователей", e);
        }
    }

    @Override
    public void registerUser(String login, Long telegramId, Long discordId, ZoneId timezone) throws SQLException {
        User existingUser = getUserByLogin(login);

        Long newTelegramId = telegramId != null ? telegramId : (existingUser != null ? existingUser.getTelegramId() : null);
        Long newDiscordId = discordId != null ? discordId : (existingUser != null ? existingUser.getDiscordId() : null);
        ZoneId newTimezone = timezone != null ? timezone : (existingUser != null ? existingUser.getTimezone() : ZoneId.of("UTC"));

        String sql = """
        INSERT INTO users (login, telegram_id, discord_id, timezone)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(login) DO UPDATE SET
            telegram_id = CASE WHEN excluded.telegram_id != 0 THEN excluded.telegram_id ELSE users.telegram_id END,
            discord_id = CASE WHEN excluded.discord_id != 0 THEN excluded.discord_id ELSE users.discord_id END,
            timezone = excluded.timezone
        """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setLong(2, newTelegramId != null ? newTelegramId : 0);
            pstmt.setLong(3, newDiscordId != null ? newDiscordId : 0);
            pstmt.setString(4, newTimezone.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTelegramId(String login, Long telegramId) throws SQLException {
        String sql = "UPDATE users SET telegram_id = ? WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, telegramId);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateDiscordId(String login, Long discordId) throws SQLException {
        String sql = "UPDATE users SET discord_id = ? WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
        }
    }

    @Override
    public User getUserByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String tzId = rs.getString("timezone");
                ZoneId tz = tzId != null ? ZoneId.of(tzId) : ZoneId.of("UTC");
                return new User(
                        rs.getString("login"),
                        rs.getLong("telegram_id") != 0 ? rs.getLong("telegram_id") : null,
                        rs.getLong("discord_id") != 0 ? rs.getLong("discord_id") : null,
                        tz
                );
            }
            return null;
        }
    }

    @Override
    public User getUserByTelegramId(Long telegramId) throws SQLException {
        String sql = "SELECT * FROM users WHERE telegram_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, telegramId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String tzId = rs.getString("timezone");
                ZoneId tz = tzId != null ? ZoneId.of(tzId) : ZoneId.of("UTC");
                return new User(
                        rs.getString("login"),
                        rs.getLong("telegram_id") != 0 ? rs.getLong("telegram_id") : null,
                        rs.getLong("discord_id") != 0 ? rs.getLong("discord_id") : null,
                        tz
                );
            }
            return null;
        }
    }

    @Override
    public User getUserByDiscordId(Long discordId) throws SQLException {
        String sql = "SELECT * FROM users WHERE discord_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String tzId = rs.getString("timezone");
                ZoneId tz = tzId != null ? ZoneId.of(tzId) : ZoneId.of("UTC");
                return new User(
                        rs.getString("login"),
                        rs.getLong("telegram_id") != 0 ? rs.getLong("telegram_id") : null,
                        rs.getLong("discord_id") != 0 ? rs.getLong("discord_id") : null,
                        tz
                );
            }
            return null;
        }
    }
}