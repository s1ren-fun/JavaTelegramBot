package org.example.entity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link ReminderService} с использованием SQLite.
 *
 * @since 1.1
 */
public class ReminderDatabaseService implements ReminderService {
    private final String dbUrl;

    /**
     * Создаёт экземпляр сервиса с указанным URL базы данных и автоматически
     * инициализирует структуру базы (создаёт таблицу {@code reminders}, если она отсутствует).
     *
     * @param dbUrl URL подключения к базе данных в формате JDBC (например, {@code "jdbc:sqlite:notes.db"})
     * @throws RuntimeException если произошла ошибка при инициализации базы данных
     */
    public ReminderDatabaseService(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }

    /**
     * Конструктор по умолчанию.
     * <p>
     * Использует локальный файл базы данных {@code notes.db} в текущей рабочей директории.
     * </p>
     */
    public ReminderDatabaseService() {
        this("jdbc:sqlite:notes.db");
    }

    /**
     * Инициализирует базу данных: создаёт таблицу {@code reminders}, если она отсутствует.
     * <p>
     * Структура таблицы:
     * </p>
     * <ul>
     *     <li>{@code reminders}: {@code id} (PK), {@code login}, {@code text}, {@code reminder_time}</li>
     * </ul>
     *
     * @throws RuntimeException если произошла ошибка SQL при создании таблицы
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            String createReminders = """
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT NOT NULL,
                    text TEXT NOT NULL,
                    reminder_time DATETIME NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createReminders);
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации таблицы напоминаний", e);
        }
    }

    @Override
    public void addReminder(String login, String text, LocalDateTime time) throws SQLException {
        String sql = """
            INSERT INTO reminders (login, text, reminder_time)
            VALUES (?, ?, ?)
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, text);
            pstmt.setTimestamp(3, Timestamp.valueOf(time));
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Reminder> getUserReminders(String login) throws SQLException {
        String sql = """
            SELECT id, login, text, reminder_time
            FROM reminders
            WHERE login = ?
            ORDER BY reminder_time
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            List<Reminder> reminders = new ArrayList<>();
            while (rs.next()) {
                reminders.add(new Reminder(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("text"),
                        rs.getTimestamp("reminder_time").toLocalDateTime()
                ));
            }
            return reminders;
        }
    }

    @Override
    public void updateReminder(int id, String text, LocalDateTime time) throws SQLException {
        String sql = "UPDATE reminders SET text = ?, reminder_time = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, text);
            pstmt.setTimestamp(2, Timestamp.valueOf(time));
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteReminder(int id) throws SQLException {
        String sql = "DELETE FROM reminders WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Reminder> getDueReminders(LocalDateTime now) throws SQLException {
        String sql = """
            SELECT id, login, text, reminder_time
            FROM reminders
            WHERE reminder_time <= ?
            ORDER BY reminder_time
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(now));
            ResultSet rs = pstmt.executeQuery();
            List<Reminder> reminders = new ArrayList<>();
            while (rs.next()) {
                reminders.add(new Reminder(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("text"),
                        rs.getTimestamp("reminder_time").toLocalDateTime()
                ));
            }
            return reminders;
        }
    }
}