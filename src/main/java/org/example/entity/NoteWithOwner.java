package org.example.entity;

/**
 * Сущность заметки с информацией о владельце.
 * <p>
 * Используется для отображения заметок, к которым пользователь имеет
 * права на чтение, но не является владельцем.
 * </p>
 *
 * @since 1.2
 */
public class NoteWithOwner {
    private final int id;
    private final String text;
    private final String ownerLogin;

    /**
     * Создает экземпляр заметки с информацией о владельце.
     *
     * @param id идентификатор заметки
     * @param text текст заметки
     * @param ownerLogin логин владельца заметки
     */
    public NoteWithOwner(int id, String text, String ownerLogin) {
        this.id = id;
        this.text = text;
        this.ownerLogin = ownerLogin;
    }

    /**
     * Возвращает идентификатор заметки.
     *
     * @return идентификатор
     */
    public int getId() {
        return id;
    }

    /**
     * Возвращает текст заметки.
     *
     * @return текст
     */
    public String getText() {
        return text;
    }

    /**
     * Возвращает логин владельца заметки.
     *
     * @return логин владельца
     */
    public String getOwnerLogin() {
        return ownerLogin;
    }
}