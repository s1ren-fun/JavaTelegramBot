package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogicBotTest {

    //проверка зеркалки сообщения
    @Test
    void testEchoCommand() {
        assertEquals("Ты написал: TEST", new LogicBot().handleCommand("TEST"));
    }

    //проверка команды /start
    @Test
    void testStartCommand() {
        assertEquals("Привет! Я твой Java-бот 🤖. Я готов повторять за тобой", new LogicBot().handleCommand("/start"));
    }

    //проверка команды /help
    @Test
    void testHelpCommand() {
        assertEquals("Привет! Я твой Java-бот 🤖. Я готов повторять за тобой", new LogicBot().handleCommand("/help"));
    }
}