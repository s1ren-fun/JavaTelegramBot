package org.example.entity;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис для работы с базой данных заметок пользователей.
 * <p>
 * Использует SQLite в качестве СУБД и хранит заметки в таблице {@code notes},
 * где каждая запись привязана к уникальному логину пользователя.
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
 * @since 1.1
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
     *     <li>{@code notes}: {@code id} (PK), {@code login}, {@code text}</li>
     *     <li>{@code note_tags}: {@code id} (PK), {@code note_id} (FK → notes.id), {@code tag}</li>
     * </ul>
     * <p>
     * Внешний ключ {@code note_id} настроен с опцией {@code ON DELETE CASCADE}.
     * </p>
     *
     * @throws RuntimeException если произошла ошибка SQL при создании таблиц
     */
    /**
     * Инициализирует базу данных: создаёт таблицы {@code notes}, {@code note_tags},
     * {@code shared_notes}, {@code shared_note_members}, и {@code note_permissions}, если они отсутствуют.
     * <p>
     * Структура таблиц:
     * </p>
     * <ul>
     *     <li>{@code notes}: {@code id} (PK), {@code login}, {@code text}</li>
     *     <li>{@code note_tags}: {@code id} (PK), {@code note_id} (FK → notes.id), {@code tag}</li>
     *     <li>{@code shared_notes}: {@code id} (PK), {@code text}, {@code creator_login}, {@code created_at}</li>
     *     <li>{@code shared_note_members}: {@code id} (PK), {@code shared_note_id} (FK), {@code user_login}, {@code added_by}, {@code added_at}</li>
     *     <li>{@code note_permissions}: {@code id} (PK), {@code note_id} (FK), {@code user_login}, {@code permission_type}, {@code granted_by}, {@code granted_at}</li>
     * </ul>
     * <p>
     * Внешние ключи настроены с опцией {@code ON DELETE CASCADE}.
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
                    login TEXT NOT NULL,
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

            String createSharedNotes = """
                CREATE TABLE IF NOT EXISTS shared_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    creator_login TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """;

            String createSharedNoteMembers = """
                CREATE TABLE IF NOT EXISTS shared_note_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    shared_note_id INTEGER NOT NULL,
                    user_login TEXT NOT NULL,
                    added_by TEXT NOT NULL,
                    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (shared_note_id) REFERENCES shared_notes(id) ON DELETE CASCADE
                );
                """;

            String createNotePermissions = """
                CREATE TABLE IF NOT EXISTS note_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL,
                    user_login TEXT NOT NULL,
                    permission_type TEXT NOT NULL CHECK(permission_type IN ('READ', 'WRITE')),
                    granted_by TEXT NOT NULL,
                    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
                );
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createNotes);
                stmt.execute(createNoteTags);
                stmt.execute(createSharedNotes);
                stmt.execute(createSharedNoteMembers);
                stmt.execute(createNotePermissions);
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации базы данных", e);
        }
    }

    @Override
    public void addNote(String login, String text) throws SQLException {
        String sqlNote = "INSERT INTO notes (login, text) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sqlNote, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, login);
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

    @Override
    public List<String> getNotesByTag(String login, String tag) throws SQLException {
        List<String> notes = new ArrayList<>();
        if (tag == null || tag.trim().isEmpty()) {
            String sql = "SELECT text FROM notes WHERE login = ? ORDER BY id";
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
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
                WHERE n.login = ? AND nt.tag = ?
                ORDER BY n.id
                """;
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, tag.toLowerCase());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    notes.add(rs.getString("text"));
                }
            }
        }
        return notes;
    }

    @Override
    public List<String> getAllNotes(String login) throws SQLException {
        return getNotesByTag(login, null);
    }

    @Override
    public String getNoteTextById(String login, int noteId) throws SQLException {
        String sql = "SELECT text FROM notes WHERE id = ? AND login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("text");
            }
            return null;
        }
    }

    @Override
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

    @Override
    public void updateNote(String login, int noteId, String newText) throws SQLException {
        if (!noteExists(login, noteId)) return;

        String sql = "UPDATE notes SET text = ? WHERE id = ? AND login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newText);
                pstmt.setInt(2, noteId);
                pstmt.setString(3, login);
                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    List<String> newTags = extractTags(newText);
                    updateTagsForNote(conn, noteId, newTags);
                }
                conn.commit();
            }
        }
    }

    @Override
    public void deleteNote(String login, int noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ? AND login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<String> getAllUserTagsWithCounts(String login) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = """
            SELECT nt.tag, COUNT(*) as cnt
            FROM note_tags nt
            JOIN notes n ON nt.note_id = n.id
            WHERE n.login = ?
            GROUP BY nt.tag
            ORDER BY nt.tag
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
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

    /**
     * Проверяет, существует ли заметка с указанным идентификатором у данного пользователя.
     *
     * @param login  логин пользователя
     * @param noteId идентификатор заметки
     * @return {@code true}, если заметка существует и принадлежит пользователю; {@code false} — иначе
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    private boolean noteExists(String login, int noteId) throws SQLException {
        String sql = "SELECT 1 FROM notes WHERE id = ? AND login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, login);
            return pstmt.executeQuery().next();
        }
    }

    @Override
    public Integer getNoteIdByIndex(String login, int index) throws SQLException {
        if (index < 1) return null;

        String sql = "SELECT id FROM notes WHERE login = ? ORDER BY id LIMIT 1 OFFSET ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setInt(2, index - 1);
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

    /**
     * Создает общую заметку.
     *
     * @param creatorLogin логин создателя заметки
     * @param text текст заметки
     * @return id созданной заметки
     * @throws SQLException если произошла ошибка при сохранении в базу данных
     */
    @Override
    public int createSharedNote(String creatorLogin, String text) throws SQLException {
        String sql = "INSERT INTO shared_notes (text, creator_login) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, text);
            pstmt.setString(2, creatorLogin);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int noteId = rs.getInt(1);
                    List<String> tags = extractTags(text);
                    saveTagsForSharedNote(conn, noteId, tags);
                    return noteId;
                }
                throw new SQLException("Ошибка создания общей заметки: не удалось получить ID");
            }
        }
    }

    /**
     * Добавляет пользователя к общей заметке с полными правами.
     *
     * @param sharedNoteId id общей заметки
     * @param userLogin логин пользователя для добавления
     * @param addedByLogin логин пользователя, который добавляет
     * @throws SQLException если произошла ошибка при сохранении в базу данных
     */
    @Override
    public void addMemberToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException {
        String sql = "INSERT INTO shared_note_members (shared_note_id, user_login, added_by) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedNoteId);
            pstmt.setString(2, userLogin);
            pstmt.setString(3, addedByLogin);
            pstmt.executeUpdate();
        }
    }

    /**
     * Возвращает список общих заметок, в которых участвует пользователь.
     *
     * @param userLogin логин пользователя
     * @return список общих заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    @Override
    public List<SharedNote> getSharedNotesForMember(String userLogin) throws SQLException {
        List<SharedNote> sharedNotes = new ArrayList<>();
        String sql = """
            SELECT sn.id, sn.text, sn.creator_login, 
                   GROUP_CONCAT(snm.user_login) as members
            FROM shared_notes sn
            JOIN shared_note_members snm ON sn.id = snm.shared_note_id
            WHERE snm.user_login = ?
            GROUP BY sn.id
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userLogin);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String creatorLogin = rs.getString("creator_login");
                String membersStr = rs.getString("members");
                List<String> members = new ArrayList<>();
                if (membersStr != null && !membersStr.isEmpty()) {
                    for (String member : membersStr.split(",")) {
                        members.add(member.trim());
                    }
                }
                sharedNotes.add(new SharedNote(id, text, creatorLogin, members));
            }
        }
        return sharedNotes;
    }


    /**
     * Возвращает все заметки, доступные пользователю (личные, общие и доступные для чтения).
     *
     * @param login логин пользователя
     * @return список всех доступных заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    @Override
    public List<AccessibleNote> getAllAccessibleNotes(String login) throws SQLException {
        List<AccessibleNote> result = new ArrayList<>();

        List<String> personalNotes = getAllNotes(login);
        for (int i = 0; i < personalNotes.size(); i++) {
            Integer noteId = getNoteIdByIndex(login, i + 1);
            if (noteId != null) {
                result.add(new AccessibleNote(
                        noteId,
                        personalNotes.get(i),
                        AccessibleNote.Type.PERSONAL,
                        login,
                        null
                ));
            }
        }

        List<SharedNote> sharedNotes = getSharedNotesForMember(login);
        for (SharedNote sharedNote : sharedNotes) {
            result.add(new AccessibleNote(
                    sharedNote.getId(),
                    sharedNote.getText(),
                    AccessibleNote.Type.SHARED,
                    sharedNote.getCreatorLogin(),
                    sharedNote.getMembers()
            ));
        }

        List<NoteWithOwner> readableNotes = getNotesWithReadPermissions(login);
        for (NoteWithOwner note : readableNotes) {
            result.add(new AccessibleNote(
                    note.getId(),
                    note.getText(),
                    AccessibleNote.Type.READ_ONLY,
                    note.getOwnerLogin(),
                    null
            ));
        }

        return result;
    }

    /**
     * Удаляет пользователя из общих заметок (удаляет все привязки).
     *
     * @param userLogin логин пользователя для удаления
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    public void removeUserFromSharedNotes(String userLogin) throws SQLException {
        String sql = "DELETE FROM shared_note_members WHERE user_login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userLogin);
            pstmt.executeUpdate();
        }
    }


    @Override
    public void updateSharedNote(int sharedNoteId, String newText) throws SQLException {
        String sql = "UPDATE shared_notes SET text = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newText);
            pstmt.setInt(2, sharedNoteId);
            pstmt.executeUpdate();

            List<String> newTags = extractTags(newText);
            updateTagsForNote(conn, sharedNoteId, newTags);
        }
    }


    @Override
    public List<String> getSharedNoteMembers(int sharedNoteId) throws SQLException {
        List<String> members = new ArrayList<>();
        String sql = "SELECT user_login FROM shared_note_members WHERE shared_note_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedNoteId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                members.add(rs.getString("user_login"));
            }
        }
        return members;
    }

    /**
     * Сохраняет теги для общей заметки в таблице {@code note_tags}.
     * <p>
     * Все существующие теги для этой заметки предварительно удаляются.
     * </p>
     *
     * @param conn активное соединение с БД (в рамках транзакции)
     * @param noteId идентификатор общей заметки
     * @param tags список тегов
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    private void saveTagsForSharedNote(Connection conn, int noteId, List<String> tags) throws SQLException {
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

    @Override
    public void grantReadPermission(int noteId, String ownerLogin, String userLogin) throws SQLException {
        if (!noteExists(ownerLogin, noteId)) {
            throw new SQLException("Заметка не существует или не принадлежит указанному владельцу");
        }

        if (hasReadPermission(noteId, userLogin)) {
            return;
        }

        String sql = """
        INSERT INTO note_permissions (note_id, user_login, permission_type, granted_by)
        VALUES (?, ?, 'READ', ?)
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, userLogin);
            pstmt.setString(3, ownerLogin);
            pstmt.executeUpdate();
        }
    }

    @Override
    public boolean hasReadPermission(int noteId, String userLogin) throws SQLException {
        String sql = """
        SELECT 1 FROM note_permissions
        WHERE note_id = ? AND user_login = ? AND permission_type = 'READ'
        LIMIT 1
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, userLogin);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<NoteWithOwner> getNotesWithReadPermissions(String userLogin) throws SQLException {
        List<NoteWithOwner> notes = new ArrayList<>();
        String sql = """
        SELECT n.id, n.text, n.login as owner_login
        FROM notes n
        JOIN note_permissions np ON n.id = np.note_id
        WHERE np.user_login = ? AND np.permission_type = 'READ'
        ORDER BY n.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userLogin);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String ownerLogin = rs.getString("owner_login");
                notes.add(new NoteWithOwner(id, text, ownerLogin));
            }
        }
        return notes;
    }

    @Override
    public SharedNote getSharedNoteById(int sharedNoteId) throws SQLException {
        String sql = """
        SELECT sn.id, sn.text, sn.creator_login,
               GROUP_CONCAT(snm.user_login) as members
        FROM shared_notes sn
        LEFT JOIN shared_note_members snm ON sn.id = snm.shared_note_id
        WHERE sn.id = ?
        GROUP BY sn.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedNoteId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String creatorLogin = rs.getString("creator_login");
                String membersStr = rs.getString("members");
                List<String> members = new ArrayList<>();
                if (membersStr != null && !membersStr.isEmpty()) {
                    for (String member : membersStr.split(",")) {
                        members.add(member.trim());
                    }
                }
                return new SharedNote(id, text, creatorLogin, members);
            }
            return null;
        }
    }

    @Override
    public void removeUserFromSharedNote(int sharedNoteId, String userLogin) throws SQLException {
        String sql = "DELETE FROM shared_note_members WHERE shared_note_id = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedNoteId);
            pstmt.setString(2, userLogin);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void addUserToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException {
        String checkSql = "SELECT 1 FROM shared_note_members WHERE shared_note_id = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, sharedNoteId);
            checkStmt.setString(2, userLogin);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return;
            }
        }

        String insertSql = "INSERT INTO shared_note_members (shared_note_id, user_login, added_by) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, sharedNoteId);
            pstmt.setString(2, userLogin);
            pstmt.setString(3, addedByLogin);
            pstmt.executeUpdate();
        }
    }

    public String getSharedNoteText(int sharedNoteId) throws SQLException {
        String sql = "SELECT text FROM shared_notes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedNoteId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("text");
            }
            return null;
        }
    }
    @Override
    public List<AccessibleNote> getAllEditableNotes(String login) throws SQLException {
        List<AccessibleNote> result = new ArrayList<>();

        String personalNotesSql = "SELECT id, text FROM notes WHERE login = ? ORDER BY id";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(personalNotesSql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                result.add(new AccessibleNote(id, text, AccessibleNote.Type.PERSONAL, login, null));
            }
        }

        String sharedNotesSql = """
        SELECT sn.id, sn.text, sn.creator_login
        FROM shared_notes sn
        JOIN shared_note_members snm ON sn.id = snm.shared_note_id
        WHERE snm.user_login = ?
        ORDER BY sn.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sharedNotesSql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String creatorLogin = rs.getString("creator_login");
                List<String> members = getSharedNoteMembers(id);
                result.add(new AccessibleNote(id, text, AccessibleNote.Type.SHARED, creatorLogin, members));
            }
        }

        return result;
    }
    @Override
    public List<AccessibleNote> getAllAccessibleNotesByTag(String login, String tag) throws SQLException {
        List<AccessibleNote> result = new ArrayList<>();

        String personalNotesSql = """
        SELECT n.id, n.text
        FROM notes n
        JOIN note_tags nt ON n.id = nt.note_id
        WHERE n.login = ? AND nt.tag = ?
        ORDER BY n.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(personalNotesSql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, tag.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                result.add(new AccessibleNote(id, text, AccessibleNote.Type.PERSONAL, login, null));
            }
        }

        String sharedNotesSql = """
        SELECT sn.id, sn.text, sn.creator_login
        FROM shared_notes sn
        JOIN shared_note_members snm ON sn.id = snm.shared_note_id
        JOIN note_tags nt ON sn.id = nt.note_id
        WHERE snm.user_login = ? AND nt.tag = ?
        ORDER BY sn.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sharedNotesSql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, tag.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String creatorLogin = rs.getString("creator_login");
                List<String> members = getSharedNoteMembers(id);
                result.add(new AccessibleNote(id, text, AccessibleNote.Type.SHARED, creatorLogin, members));
            }
        }

        String readableNotesSql = """
        SELECT n.id, n.text, n.login as owner_login
        FROM notes n
        JOIN note_permissions np ON n.id = np.note_id
        JOIN note_tags nt ON n.id = nt.note_id
        WHERE np.user_login = ? AND np.permission_type = 'READ' AND nt.tag = ?
        ORDER BY n.id
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(readableNotesSql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, tag.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                String ownerLogin = rs.getString("owner_login");
                result.add(new AccessibleNote(id, text, AccessibleNote.Type.READ_ONLY, ownerLogin, null));
            }
        }

        return result;
    }

    @Override
    public List<String> getAllAccessibleTagsWithCounts(String login) throws SQLException {
        Map<String, Integer> tagCounts = new LinkedHashMap<>();

        String sql = """
        SELECT tag, COUNT(*) as cnt
        FROM (
            -- Теги из личных заметок
            SELECT nt.tag
            FROM notes n
            JOIN note_tags nt ON n.id = nt.note_id
            WHERE n.login = ?
            
            UNION ALL
            
            -- Теги из общих заметок
            SELECT nt.tag
            FROM shared_notes sn
            JOIN shared_note_members snm ON sn.id = snm.shared_note_id
            JOIN note_tags nt ON sn.id = nt.note_id
            WHERE snm.user_login = ?
            
            UNION ALL
            
            -- Теги из заметок с правами на чтение
            SELECT nt.tag
            FROM notes n
            JOIN note_permissions np ON n.id = np.note_id
            JOIN note_tags nt ON n.id = nt.note_id
            WHERE np.user_login = ? AND np.permission_type = 'READ'
        ) all_tags
        GROUP BY tag
        ORDER BY tag
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, login);
            pstmt.setString(3, login);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String tag = rs.getString("tag");
                int count = rs.getInt("cnt");
                tagCounts.put(tag, count);
            }
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
            String tag = entry.getKey();
            int count = entry.getValue();
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

        return result;
    }
}