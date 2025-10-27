package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с базой данных заметок пользователей.
 * <p>
 * Использует SQLite в качестве СУБД и хранит заметки в таблице {@code notes},
 * где каждая запись привязана к уникальному идентификатору пользователя (например, Telegram ID).
 * <p>
 * Поддерживает операции:
 * </p>
 * <ul>
 *     <li>Создание таблицы при первом запуске</li>
 *     <li>Добавление новой заметки</li>
 *     <li>Получение всех заметок пользователя</li>
 *     <li>Получение текста заметки по её идентификатору</li>
 *     <li>Обновление текста заметки</li>
 *     <li>Удаление заметки</li>
 *     <li>Проверка существования заметки у пользователя</li>
 * </ul>
 *
 * @since 1.0
 */
public class NoteDatabaseService implements NoteService {

    /**
     * URL подключения к базе данных SQLite.
     */
    private final String dbUrl;

    /**
     * Создаёт экземпляр сервиса с указанным URL базы данных и автоматически
     * инициализирует структуру базы (создаёт таблицу {@code notes}, если она отсутствует).
     *
     * @param dbUrl URL подключения к базе данных в формате JDBC (например, {@code "jdbc:sqlite:notes.db"})
     * @throws RuntimeException если произошла ошибка при инициализации базы данных
     */
    public NoteDatabaseService(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }

    /**
     * Конструктор по умолчанию.
     * <p>
     * Использует локальный файл базы данных {@code notes.db} в текущей рабочей директории.
     * </p>
     */
    public NoteDatabaseService() {
        this("jdbc:sqlite:notes.db");
    }

    /**
     * Инициализирует базу данных: создаёт таблицу {@code notes}, если она ещё не существует.
     * <p>
     * Таблица содержит следующие поля:
     * </p>
     * <ul>
     *     <li>{@code id} — автоинкрементный первичный ключ</li>
     *     <li>{@code user_id} — идентификатор пользователя (BIGINT)</li>
     *     <li>{@code text} — текст заметки (TEXT)</li>
     * </ul>
     *
     * @throws RuntimeException если произошла ошибка SQL при создании таблицы
     */
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

    /**
     * Добавляет новую заметку для указанного пользователя.
     *
     * @param userId идентификатор пользователя (например, Telegram ID)
     * @param text   текст новой заметки (не должен быть {@code null})
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    public void addNote(long userId, String text) throws SQLException {
        String sql = "INSERT INTO notes (user_id, text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, text);
            pstmt.executeUpdate();
        }
    }

    /**
     * Возвращает список всех заметок указанного пользователя в формате
     * {@code "id. текст"}.
     * <p>
     * Заметки сортируются по возрастанию идентификатора.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return список строк вида {@code "1. Купить молоко"}, или пустой список, если заметок нет
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    public List<String> getAllNotes(long userId) throws SQLException {
        List<String> notes = new ArrayList<>();
        String sql = "SELECT id, text FROM notes WHERE user_id = ? ORDER BY id";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notes.add(rs.getString("text"));
            }
        }
        return notes;
    }

    /**
     * Возвращает текст заметки по её идентификатору и идентификатору пользователя.
     * <p>
     * Если заметка не найдена или не принадлежит указанному пользователю, возвращается {@code null}.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param noteId идентификатор заметки
     * @return текст заметки или {@code null}, если заметка не существует или не принадлежит пользователю
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
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
            return null;
        }
    }

    /**
     * Обновляет текст существующей заметки.
     * <p>
     * Обновление происходит только если заметка с указанным {@code noteId}
     * существует и принадлежит указанному пользователю.
     * </p>
     *
     * @param userId   идентификатор пользователя
     * @param noteId   идентификатор заметки
     * @param newText  новый текст заметки
     * @return {@code true}, если заметка была успешно обновлена; {@code false} — если не найдена
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
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

    /**
     * Удаляет заметку по её идентификатору и идентификатору пользователя.
     * <p>
     * Удаление происходит только если заметка принадлежит указанному пользователю.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param noteId идентификатор заметки
     * @return {@code true}, если заметка была успешно удалена; {@code false} — если не найдена
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    public boolean deleteNote(long userId, int noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Проверяет, существует ли заметка с указанным идентификатором у данного пользователя.
     *
     * @param userId идентификатор пользователя
     * @param noteId идентификатор заметки
     * @return {@code true}, если заметка существует и принадлежит пользователю; {@code false} — иначе
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    public boolean noteExists(long userId, int noteId) throws SQLException {
        String sql = "SELECT 1 FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            return pstmt.executeQuery().next();
        }
    }

    /**
     * Возвращает реальный идентификатор заметки (id из базы данных) по её порядковому номеру для пользователя.
     * @param userId идентификатор пользователя
     * @param index порядковый номер заметки
     * @return реальный идентификатор заметки в базе данных или {@code null}, если заметки с таким номером не существует
     * @throws SQLException
     */
    public Integer getNoteIdByIndex(long userId, int index) throws SQLException {
        if (index < 1) return null;

        String sql = "SELECT id FROM notes WHERE user_id = ? ORDER BY id LIMIT 1 OFFSET ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, index - 1); // OFFSET начинается с 0
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }
}