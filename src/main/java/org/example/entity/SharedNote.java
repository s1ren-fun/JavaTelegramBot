package org.example.entity;

import java.util.List;

/**
 * Сущность общей заметки.
 * <p>
 * Содержит информацию о тексте заметки, создателе и участниках,
 * имеющих доступ к редактированию.
 * </p>
 *
 * @since 1.2
 */
public class SharedNote {
    private final int id;
    private final String text;
    private final String creatorLogin;
    private final List<String> members;

    /**
     * Создает экземпляр общей заметки.
     *
     * @param id идентификатор заметки
     * @param text текст заметки
     * @param creatorLogin логин создателя
     * @param members список логинов участников с правами на редактирование
     */
    public SharedNote(int id, String text, String creatorLogin, List<String> members) {
        this.id = id;
        this.text = text;
        this.creatorLogin = creatorLogin;
        this.members = members;
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
     * Возвращает логин создателя заметки.
     *
     * @return логин создателя
     */
    public String getCreatorLogin() {
        return creatorLogin;
    }

    /**
     * Возвращает список логинов участников с правами на редактирование.
     *
     * @return список участников
     */
    public List<String> getMembers() {
        return members;
    }
}