package org.example.entity;

import java.sql.SQLException;
import java.util.List;

/**
 * Интерфейс сервиса для управления заметками пользователей.
 * <p>
 * Определяет контракт для выполнения операций с заметками: создание, чтение,
 * обновление, удаление, а также дополнительные операции для работы с тегами.
 * </p>
 * <p>
 * Данный интерфейс спроектирован для использования в боте Telegram и обеспечивает
 * всю необходимую функциональность для управления заметками пользователей
 * с возможностью фильтрации по тегам и организации данных.
 * </p>
 * <p>
 * Заметки теперь привязаны к {@code login}, а не к {@code userId}, что позволяет
 * использовать их на разных платформах (Telegram, Discord) при входе под одним логином.
 * </p>
 *
 * @since 1.1
 */
public interface NoteService {
    /**
     * Добавляет новую заметку для пользователя.
     *
     * @param login логин пользователя
     * @param text текст заметки, который может содержать теги в формате #тег
     * @throws SQLException если произошла ошибка при сохранении в базу данных
     */
    void addNote(String login, String text) throws SQLException;

    /**
     * Получает реальный идентификатор заметки по её порядковому номеру для пользователя.
     * <p>
     * Порядковые номера начинаются с 1 для первой заметки в списке.
     * </p>
     *
     * @param login логин пользователя
     * @param index порядковый номер заметки (начиная с 1)
     * @return реальный идентификатор заметки в базе данных или null, если заметка не найдена
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    Integer getNoteIdByIndex(String login, int index) throws SQLException;

    /**
     * Получает текст заметки по её идентификатору.
     *
     * @param login логин пользователя
     * @param noteId идентификатор заметки
     * @return текст заметки или null, если заметка не найдена
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    String getNoteTextById(String login, int noteId) throws SQLException;

    /**
     * Получает список тегов для указанной заметки.
     *
     * @param noteId идентификатор заметки
     * @return список тегов, связанных с заметкой (пустой список, если тегов нет)
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getTagsForNote(int noteId) throws SQLException;

    /**
     * Обновляет текст существующей заметки.
     * <p>
     * При обновлении текста автоматически обрабатываются теги в новом тексте,
     * обновляя соответствующие связи в базе данных.
     * </p>
     *
     * @param login логин пользователя
     * @param noteId идентификатор заметки
     * @param newText новый текст заметки
     * @throws SQLException если произошла ошибка при обновлении данных
     */
    void updateNote(String login, int noteId, String newText) throws SQLException;

    /**
     * Удаляет заметку пользователя по её идентификатору.
     * <p>
     * При удалении заметки также удаляются все связанные с ней теги.
     * </p>
     *
     * @param login логин пользователя
     * @param noteId идентификатор заметки для удаления
     * @throws SQLException если произошла ошибка при удалении данных
     */
    void deleteNote(String login, int noteId) throws SQLException;

    /**
     * Получает список всех заметок пользователя, отфильтрованных по указанному тегу.
     *
     * @param login логин пользователя
     * @param tag тег для фильтрации в формате "#тег"
     * @return список текстов заметок с указанным тегом
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getNotesByTag(String login, String tag) throws SQLException;

    /**
     * Получает список всех тегов пользователя с количеством заметок для каждого тега.
     * <p>
     * Каждый элемент списка имеет формат "#тег (количество)".
     * </p>
     *
     * @param login логин пользователя
     * @return список строк с тегами и количеством заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getAllUserTagsWithCounts(String login) throws SQLException;

    /**
     * Получает список всех заметок пользователя.
     * <p>
     * Заметки возвращаются в порядке убывания даты создания (последние добавленные - первые).
     * </p>
     *
     * @param login логин пользователя
     * @return список текстов всех заметок пользователя
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getAllNotes(String login) throws SQLException;

    /**
     * Создает общую заметку.
     *
     * @param creatorLogin логин создателя заметки
     * @param text текст заметки
     * @return id созданной заметки
     * @throws SQLException если произошла ошибка при сохранении в базу данных
     */
    int createSharedNote(String creatorLogin, String text) throws SQLException;

    /**
     * Добавляет пользователя к общей заметке с полными правами.
     *
     * @param sharedNoteId id общей заметки
     * @param userLogin логин пользователя для добавления
     * @param addedByLogin логин пользователя, который добавляет
     * @throws SQLException если произошла ошибка при сохранении в базу данных
     */
    void addMemberToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException;

    /**
     * Возвращает список общих заметок, в которых участвует пользователь.
     *
     * @param userLogin логин пользователя
     * @return список общих заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<SharedNote> getSharedNotesForMember(String userLogin) throws SQLException;

    /**
     * Возвращает все заметки, доступные пользователю (личные, общие и доступные для чтения).
     *
     * @param userLogin логин пользователя
     * @return список всех доступных заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<AccessibleNote> getAllAccessibleNotes(String userLogin) throws SQLException;

    /**
     * Обновляет текст общей заметки.
     *
     * @param sharedNoteId ID общей заметки
     * @param newText новый текст заметки
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    void updateSharedNote(int sharedNoteId, String newText) throws SQLException;

    /**
     * Получает список логинов пользователей, имеющих доступ к общей заметке.
     *
     * @param sharedNoteId ID общей заметки
     * @return список логинов участников
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getSharedNoteMembers(int sharedNoteId) throws SQLException;

    /**
     * Предоставляет доступ на чтение к личной заметке другому пользователю.
     *
     * @param noteId идентификатор заметки
     * @param ownerLogin логин владельца заметки
     * @param userLogin логин пользователя, которому предоставляется доступ
     * @throws SQLException при ошибке обращения к базе данных
     */
    void grantReadPermission(int noteId, String ownerLogin, String userLogin) throws SQLException;

    /**
     * Проверяет, имеет ли пользователь доступ на чтение к заметке.
     *
     * @param noteId идентификатор заметки
     * @param userLogin логин пользователя
     * @return true если пользователь имеет доступ, иначе false
     * @throws SQLException при ошибке обращения к базе данных
     */
    boolean hasReadPermission(int noteId, String userLogin) throws SQLException;

    /**
     * Возвращает список заметок, доступных пользователю для чтения (включая чужие).
     *
     * @param userLogin логин пользователя
     * @return список доступных заметок
     * @throws SQLException при ошибке обращения к базе данных
     */
    List<NoteWithOwner> getNotesWithReadPermissions(String userLogin) throws SQLException;

    /**
     * Получает общую заметку по её ID.
     *
     * @param sharedNoteId ID общей заметки
     * @return общая заметка или null, если не найдена
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    SharedNote getSharedNoteById(int sharedNoteId) throws SQLException;

    /**
     * Удаляет пользователя из общей заметки.
     *
     * @param sharedNoteId ID общей заметки
     * @param userLogin логин пользователя для удаления
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    void removeUserFromSharedNote(int sharedNoteId, String userLogin) throws SQLException;

    /**
     * Добавляет пользователя в общую заметку.
     *
     * @param sharedNoteId ID общей заметки
     * @param userLogin логин пользователя для добавления
     * @param addedByLogin логин пользователя, который добавляет
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    void addUserToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException;

    /**
     * Получает текст общей заметки по её ID.
     *
     * @param sharedNoteId ID общей заметки
     * @return текст заметки или null, если не найдена
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    String getSharedNoteText(int sharedNoteId) throws SQLException;
    /**
     * Получает все доступные пользователю заметки, отфильтрованные по тегу.
     *
     * @param login логин пользователя
     * @param tag тег для фильтрации
     * @return список заметок с информацией о типе доступа
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<AccessibleNote> getAllAccessibleNotesByTag(String login, String tag) throws SQLException;

    /**
     * Получает список всех тегов со счетчиками из всех доступных заметок пользователя.
     *
     * @param login логин пользователя
     * @return список строк в формате "#тег — X заметок/заметки/заметка"
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<String> getAllAccessibleTagsWithCounts(String login) throws SQLException;
    /**
     * Возвращает все заметки, доступные пользователю для редактирования (личные и общие).
     *
     * @param login логин пользователя
     * @return список доступных заметок
     * @throws SQLException если произошла ошибка при обращении к базе данных
     */
    List<AccessibleNote> getAllEditableNotes(String login) throws SQLException;

}