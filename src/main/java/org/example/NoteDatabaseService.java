package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDatabaseService {

    private final String dbUrl;

    public NoteDatabaseService(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }

    // Конструктор по умолчанию — использует файл notes.db
    public NoteDatabaseService() {
        this("jdbc:sqlite:notes.db");
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id BIGINT NOT NULL,
                    text TEXT NOT NULL
                );
                """;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации базы данных", e);
        }
    }

    // Добавление новой заметки
    public void addNote(long userId, String text) throws SQLException {
        String sql = "INSERT INTO notes (user_id, text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, text);
            pstmt.executeUpdate();
        }
    }

    // Получение всех заметок пользователя
    public List<String> getAllNotes(long userId) throws SQLException {
        List<String> notes = new ArrayList<>();
        String sql = "SELECT id, text FROM notes WHERE user_id = ? ORDER BY id";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                notes.add(id + ". " + text);
            }
        }
        return notes;
    }

    // Получение текста заметки по ID
    public String getNoteTextById(long userId, int noteId) throws SQLException {
        String sql = "SELECT text FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("text");
            }
            return null; // заметка не найдена
        }
    }

    // Обновление заметки
    public boolean updateNote(long userId, int noteId, String newText) throws SQLException {
        String sql = "UPDATE notes SET text = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newText);
            pstmt.setInt(2, noteId);
            pstmt.setLong(3, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Удаление заметки
    public boolean deleteNote(long userId, int noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Проверка существования заметки у пользователя
    public boolean noteExists(long userId, int noteId) throws SQLException {
        String sql = "SELECT 1 FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            return pstmt.executeQuery().next();
        }
    }
}