package org.example.entity;

import java.util.List;

/**
 * Сущность заметки с информацией о типе доступа.
 * <p>
 * Используется для отображения всех доступных пользователю заметок
 * с различными типами доступа: личные, общие или только для чтения.
 * </p>
 *
 * @since 1.2
 */
public class AccessibleNote {
    /**
     * Тип доступа к заметке.
     */
    public enum Type {
        /**
         * Личная заметка пользователя.
         */
        PERSONAL,

        /**
         * Общая заметка с полными правами доступа.
         */
        SHARED,

        /**
         * Заметка с правами только на чтение.
         */
        READ_ONLY
    }

    private final int id;
    private final String text;
    private final Type type;
    private final String ownerLogin;
    private final List<String> sharedMembers;

    /**
     * Создает экземпляр доступной заметки.
     *
     * @param id идентификатор заметки
     * @param text текст заметки
     * @param type тип доступа
     * @param ownerLogin логин владельца/создателя
     * @param sharedMembers список участников (для общих заметок)
     */
    public AccessibleNote(int id, String text, Type type, String ownerLogin, List<String> sharedMembers) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.ownerLogin = ownerLogin;
        this.sharedMembers = sharedMembers;
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
     * Возвращает тип доступа к заметке.
     *
     * @return тип доступа
     */
    public Type getType() {
        return type;
    }

    /**
     * Возвращает логин владельца/создателя заметки.
     *
     * @return логин владельца
     */
    public String getOwnerLogin() {
        return ownerLogin;
    }

    /**
     * Возвращает список участников для общей заметки.
     * Для личных заметок и заметок только для чтения возвращает null.
     *
     * @return список участников или null
     */
    public List<String> getSharedMembers() {
        return sharedMembers;
    }
}