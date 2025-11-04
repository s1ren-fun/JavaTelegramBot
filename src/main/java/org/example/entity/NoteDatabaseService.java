package org.example.entity;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Инициализирует базу данных: создаёт таблицы {@code notes} и {@code note_tags}, если они отсутствуют.
     * <p>
     * Структура таблиц:
     * </p>
     * <ul>
     *     <li>{@code notes}: {@code id} (PK), {@code user_id}, {@code text}</li>
     *     <li>{@code note_tags}: {@code id} (PK), {@code note_id} (FK → notes.id), {@code tag}</li>
     * </ul>
     * <p>
     * Внешний ключ {@code note_id} настроен с опцией {@code ON DELETE CASCADE}.
     * </p>
     *
     * @throws RuntimeException если произошла ошибка SQL при создании таблиц
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            String createNotes = """
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id BIGINT NOT NULL,
                    text TEXT NOT NULL
                );
                """;

            String createNoteTags = """
                CREATE TABLE IF NOT EXISTS note_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL,
                    tag TEXT NOT NULL,
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
                );
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createNotes);
                stmt.execute(createNoteTags);
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации базы данных", e);
        }
    }

    public void addNote(long userId, String text) throws SQLException {
        String sqlNote = "INSERT INTO notes (user_id, text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sqlNote, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, text);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int noteId = rs.getInt(1);
                    List<String> tags = extractTags(text);
                    saveTagsForNote(conn, noteId, tags);
                }
            }
            conn.commit();
        }
    }

    public List<String> getAllNotes(long userId) throws SQLException {
        return getNotesByTag(userId, null);
    }

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

    public void updateNote(long userId, int noteId, String newText) throws SQLException {
        if (!noteExists(userId, noteId)) return;

        String sql = "UPDATE notes SET text = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newText);
                pstmt.setInt(2, noteId);
                pstmt.setLong(3, userId);
                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    List<String> newTags = extractTags(newText);
                    updateTagsForNote(conn, noteId, newTags);
                }
                conn.commit();
            }
        }
    }

    public void deleteNote(long userId, int noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
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

    /**
     * Извлекает теги из текста в формате {@code #тег}.
     * <p>
     * Поддерживаемый формат: {@code #} + буквы/цифры/нижнее подчёркивание.
     * </p>
     *
     * @param text текст заметки
     * @return список уникальных тегов в нижнем регистре
     */
    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#[\\p{L}0-9_]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(new LinkedHashSet<>(tags));
    }

    /**
     * Сохраняет теги для заметки в таблице {@code note_tags}.
     * <p>
     * Все существующие теги для этой заметки предварительно удаляются.
     * </p>
     *
     * @param conn   активное соединение с БД (в рамках транзакции)
     * @param noteId идентификатор заметки
     * @param tags   список тегов
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    private void saveTagsForNote(Connection conn, int noteId, List<String> tags) throws SQLException {
        if (tags.isEmpty()) return;

        String insertTag = "INSERT INTO note_tags (note_id, tag) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertTag)) {
            for (String tag : tags) {
                pstmt.setInt(1, noteId);
                pstmt.setString(2, tag);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    /**
     * Обновляет теги у заметки: удаляет старые и сохраняет новые.
     *
     * @param conn   активное соединение с БД
     * @param noteId идентификатор заметки
     * @param newTags новый список тегов
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    private void updateTagsForNote(Connection conn, int noteId, List<String> newTags) throws SQLException {
        String deleteOld = "DELETE FROM note_tags WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteOld)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
        saveTagsForNote(conn, noteId, newTags);
    }

    public List<String> getTagsForNote(int noteId) throws SQLException {
        List<String> tags = new ArrayList<>();
        String sql = "SELECT tag FROM note_tags WHERE note_id = ? ORDER BY id";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tags.add(rs.getString("tag"));
            }
        }
        return tags;
    }

    public List<String> getNotesByTag(long userId, String tag) throws SQLException {
        List<String> notes = new ArrayList<>();
        if (tag == null || tag.trim().isEmpty()) {
            String sql = "SELECT text FROM notes WHERE user_id = ? ORDER BY id";
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    notes.add(rs.getString("text"));
                }
            }
        } else {
            String sql = """
                SELECT DISTINCT n.text
                FROM notes n
                JOIN note_tags nt ON n.id = nt.note_id
                WHERE n.user_id = ? AND nt.tag = ?
                ORDER BY n.id
                """;
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, tag.toLowerCase());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    notes.add(rs.getString("text"));
                }
            }
        }
        return notes;
    }

    public List<String> getAllUserTagsWithCounts(long userId) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = """
            SELECT nt.tag, COUNT(*) as cnt
            FROM note_tags nt
            JOIN notes n ON nt.note_id = n.id
            WHERE n.user_id = ?
            GROUP BY nt.tag
            ORDER BY nt.tag
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String tag = rs.getString("tag");
                int count = rs.getInt("cnt");
                String suffix;
                if (count % 10 == 1 && count % 100 != 11) {
                    suffix = "заметка";
                } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
                    suffix = "заметки";
                } else {
                    suffix = "заметок";
                }
                result.add(tag + " — " + count + " " + suffix);
            }
        }
        return result;
    }
}