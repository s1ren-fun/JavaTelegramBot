package org.example.entity;

import java.sql.SQLException;
import java.util.List;

/**
 * NoteService — интерфейс для работы с хранилищем заметок.
 * Определяет поведение, ожидаемое LogicBot и тестами.
 */
public interface NoteService {
    void addNote(long userId, String text) throws SQLException;
    List<String> getAllNotes(long userId) throws SQLException;
    Integer getNoteIdByIndex(long userId, int index) throws SQLException;
    String getNoteTextById(long userId, int noteId) throws SQLException;
    boolean updateNote(long userId, int noteId, String newText) throws SQLException;
    boolean deleteNote(long userId, int noteId) throws SQLException;
    List<String> getTagsForNote(int noteId) throws SQLException;
    List<String> getNotesByTag(long userId, String tag) throws SQLException;
    List<String> getAllUserTagsWithCounts(long userId) throws SQLException;
}
