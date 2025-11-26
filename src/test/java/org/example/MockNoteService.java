package org.example;

import org.example.entity.NoteService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MockNoteService — in-memory реализация NoteService для тестов.
 */
public class MockNoteService implements NoteService {
    private class Note {
        int id;
        String login;
        String text;
        List<String> tags;

        Note(int id, String login, String text, List<String> tags) {
            this.id = id;
            this.login = login;
            this.text = text;
            this.tags = new ArrayList<>(tags);
        }
    }

    private final Map<String, List<MockNoteService.Note>> storage = new HashMap<>();
    private int nextId = 1;

    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#[\\p{L}0-9_]+");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(new LinkedHashSet<>(tags));
    }

    @Override
    public void addNote(String login, String text) {
        List<String> tags = extractTags(text);
        storage.computeIfAbsent(login, k -> new ArrayList<>()).add(new MockNoteService.Note(nextId++, login, text, tags));
    }

    @Override
    public Integer getNoteIdByIndex(String login, int index) {
        List<MockNoteService.Note> list = storage.getOrDefault(login, Collections.emptyList());
        if (index < 1 || index > list.size()) return null;
        return list.get(index - 1).id;
    }

    @Override
    public String getNoteTextById(String login, int noteId) {
        return storage.getOrDefault(login, Collections.emptyList())
                .stream()
                .filter(n -> n.id == noteId && n.login.equals(login))
                .findFirst()
                .map(n -> n.text)
                .orElse(null);
    }

    @Override
    public List<String> getTagsForNote(int noteId) {
        for (List<MockNoteService.Note> notes : storage.values()) {
            for (MockNoteService.Note n : notes) {
                if (n.id == noteId) {
                    return new ArrayList<>(n.tags);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void updateNote(String login, int noteId, String newText) {
        List<MockNoteService.Note> list = storage.getOrDefault(login, Collections.emptyList());
        for (MockNoteService.Note n : list) {
            if (n.id == noteId && n.login.equals(login)) {
                n.text = newText;
                n.tags = extractTags(newText);
                return;
            }
        }
    }

    @Override
    public void deleteNote(String login, int noteId) {
        List<MockNoteService.Note> list = storage.getOrDefault(login, Collections.emptyList());
        list.removeIf(n -> n.id == noteId && n.login.equals(login));
    }

    @Override
    public List<String> getNotesByTag(String login, String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return getAllNotes(login);
        }
        String normalizedTag = tag.toLowerCase();
        return storage.getOrDefault(login, Collections.emptyList())
                .stream()
                .filter(n -> n.tags.contains(normalizedTag))
                .map(n -> n.text)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllUserTagsWithCounts(String login) {
        Map<String, Integer> tagCount = new HashMap<>();
        for (MockNoteService.Note n : storage.getOrDefault(login, Collections.emptyList())) {
            for (String tag : n.tags) {
                tagCount.merge(tag, 1, Integer::sum);
            }
        }
        return tagCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
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
                    return tag + " — " + count + " " + suffix;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllNotes(String login) {
        return storage.getOrDefault(login, Collections.emptyList())
                .stream()
                .map(n -> n.text)
                .collect(Collectors.toList());
    }
}
