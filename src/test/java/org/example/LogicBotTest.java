package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogicBotTest {

    @Test
    void testEchoCommand() {
        assertEquals("Ты написал: TEST", new LogicBot().handleCommand("TEST"));
    }

    @Test
    void testStartCommand() {
        assertEquals("Привет! Я твой Java-бот 🤖. Я готов повторять за тобой", new LogicBot().handleCommand("/start"));
    }

    @Test
    void testHelpCommand() {
        assertEquals("Привет! Я твой Java-бот 🤖. Я готов повторять за тобой", new LogicBot().handleCommand("/help"));
    }
}