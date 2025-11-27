package org.example;

import org.example.entity.ReminderService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MockReminderService — in-memory реализация ReminderService для тестов.
 */
public class MockReminderService implements ReminderService {
    private class ReminderEntry {
        int id;
        String login;
        String text;
        LocalDateTime reminderTime;

        ReminderEntry(int id, String login, String text, LocalDateTime reminderTime) {
            this.id = id;
            this.login = login;
            this.text = text;
            this.reminderTime = reminderTime;
        }
    }

    private final List<MockReminderService.ReminderEntry> reminders = new ArrayList<>();
    private int nextId = 1;

    @Override
    public void addReminder(String login, String text, LocalDateTime time) {
        reminders.add(new MockReminderService.ReminderEntry(nextId++, login, text, time));
    }

    @Override
    public List<org.example.entity.Reminder> getUserReminders(String login) {
        return reminders.stream()
                .filter(r -> r.login.equals(login))
                .map(r -> new org.example.entity.Reminder(r.id, r.login, r.text, r.reminderTime))
                .collect(Collectors.toList());
    }

    @Override
    public void updateReminder(int id, String text, LocalDateTime time) {
        for (MockReminderService.ReminderEntry r : reminders) {
            if (r.id == id) {
                if (text != null) r.text = text;
                if (time != null) r.reminderTime = time;
                break;
            }
        }
    }

    @Override
    public void deleteReminder(int id) {
        reminders.removeIf(r -> r.id == id);
    }

    @Override
    public List<org.example.entity.Reminder> getDueReminders(LocalDateTime now) {
        return reminders.stream()
                .filter(r -> !r.reminderTime.isAfter(now))
                .map(r -> new org.example.entity.Reminder(r.id, r.login, r.text, r.reminderTime))
                .collect(Collectors.toList());
    }
}
