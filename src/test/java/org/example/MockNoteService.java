package org.example;

import org.example.entity.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MockNoteService — in-memory реализация NoteService для тестов.
 * Полностью переписана для корректной работы с общими заметками и правами доступа.
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

    private class NotePermission {
        int noteId;
        String userLogin;
        String permissionType;
        String grantedBy;
        LocalDateTime grantedAt;

        NotePermission(int noteId, String userLogin, String permissionType, String grantedBy) {
            this.noteId = noteId;
            this.userLogin = userLogin;
            this.permissionType = permissionType;
            this.grantedBy = grantedBy;
            this.grantedAt = LocalDateTime.now();
        }
    }

    private class InternalSharedNote {
        int id;
        String text;
        String creatorLogin;
        List<String> members;
        List<String> tags;

        InternalSharedNote(int id, String text, String creatorLogin, List<String> members) {
            this.id = id;
            this.text = text;
            this.creatorLogin = creatorLogin;
            this.members = new ArrayList<>(members);
            this.tags = new ArrayList<>();
        }

        SharedNote toSharedNote() {
            return new SharedNote(id, text, creatorLogin, new ArrayList<>(members));
        }
    }

    private final Map<String, List<Note>> personalNotes = new HashMap<>();
    private final List<InternalSharedNote> sharedNotes = new ArrayList<>();
    private final List<NotePermission> notePermissions = new ArrayList<>();

    private int nextNoteId = 1;
    private int nextSharedNoteId = 1;

    private InternalSharedNote getInternalSharedNoteById(int id) {
        for (InternalSharedNote note : sharedNotes) {
            if (note.id == id) {
                return note;
            }
        }
        return null;
    }

    private List<String> extractTags(String text) {
        List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#[\\p{L}0-9_]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return new ArrayList<>(new LinkedHashSet<>(tags));
    }

    private Note getNoteById(int id) {
        for (List<Note> notes : personalNotes.values()) {
            for (Note note : notes) {
                if (note.id == id) {
                    return note;
                }
            }
        }
        return null;
    }

    @Override
    public void addNote(String login, String text) {
        List<String> tags = extractTags(text);
        personalNotes.computeIfAbsent(login, k -> new ArrayList<>())
                .add(new Note(nextNoteId++, login, text, tags));
    }

    @Override
    public Integer getNoteIdByIndex(String login, int index) {
        List<Note> userNotes = personalNotes.getOrDefault(login, Collections.emptyList());
        if (index < 1 || index > userNotes.size()) {
            return null;
        }
        return userNotes.get(index - 1).id;
    }

    @Override
    public String getNoteTextById(String login, int noteId) {
        Note note = getNoteById(noteId);
        if (note != null && note.login.equals(login)) {
            return note.text;
        }
        return null;
    }

    @Override
    public List<String> getTagsForNote(int noteId) {
        Note note = getNoteById(noteId);
        if (note != null) {
            return new ArrayList<>(note.tags);
        }
        return Collections.emptyList();
    }

    @Override
    public void updateNote(String login, int noteId, String newText) {
        Note note = getNoteById(noteId);
        if (note != null && note.login.equals(login)) {
            note.text = newText;
            note.tags = extractTags(newText);
        }
    }

    @Override
    public void deleteNote(String login, int noteId) {
        List<Note> userNotes = personalNotes.get(login);
        if (userNotes != null) {
            userNotes.removeIf(note -> note.id == noteId);
        }
    }

    @Override
    public List<String> getNotesByTag(String login, String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return getAllNotes(login);
        }
        String normalizedTag = tag.toLowerCase();
        return personalNotes.getOrDefault(login, Collections.emptyList())
                .stream()
                .filter(note -> note.tags.contains(normalizedTag))
                .map(note -> note.text)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllUserTagsWithCounts(String login) {
        Map<String, Integer> tagCount = new HashMap<>();
        for (Note note : personalNotes.getOrDefault(login, Collections.emptyList())) {
            for (String tag : note.tags) {
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
        return personalNotes.getOrDefault(login, Collections.emptyList())
                .stream()
                .map(note -> note.text)
                .collect(Collectors.toList());
    }

    @Override
    public int createSharedNote(String creatorLogin, String text) throws SQLException {
        List<String> tags = extractTags(text);
        List<String> members = new ArrayList<>();
        members.add(creatorLogin);

        InternalSharedNote sharedNote = new InternalSharedNote(nextSharedNoteId, text, creatorLogin, members);
        sharedNote.tags = tags;
        sharedNotes.add(sharedNote);
        int id = nextSharedNoteId;
        nextSharedNoteId++;
        return id;
    }

    @Override
    public void addMemberToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        if (note != null && !note.members.contains(userLogin)) {
            note.members.add(userLogin);
        }
    }

    @Override
    public List<SharedNote> getSharedNotesForMember(String userLogin) throws SQLException {
        List<SharedNote> result = new ArrayList<>();
        for (InternalSharedNote note : sharedNotes) {
            if (note.members.contains(userLogin)) {
                result.add(note.toSharedNote());
            }
        }
        return result;
    }

    @Override
    public List<AccessibleNote> getAllAccessibleNotes(String userLogin) throws SQLException {
        List<AccessibleNote> result = new ArrayList<>();

        List<Note> userPersonalNotes = personalNotes.getOrDefault(userLogin, Collections.emptyList());
        for (Note note : userPersonalNotes) {
            result.add(new AccessibleNote(
                    note.id,
                    note.text,
                    AccessibleNote.Type.PERSONAL,
                    userLogin,
                    null
            ));
        }

        List<SharedNote> userSharedNotes = getSharedNotesForMember(userLogin);
        for (SharedNote sharedNote : userSharedNotes) {
            result.add(new AccessibleNote(
                    sharedNote.getId(),
                    sharedNote.getText(),
                    AccessibleNote.Type.SHARED,
                    sharedNote.getCreatorLogin(),
                    sharedNote.getMembers()
            ));
        }

        List<NoteWithOwner> readableNotes = getNotesWithReadPermissions(userLogin);
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

    @Override
    public void updateSharedNote(int sharedNoteId, String newText) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        if (note != null) {
            note.text = newText;
            note.tags = extractTags(newText);
        }
    }

    @Override
    public List<String> getSharedNoteMembers(int sharedNoteId) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        if (note != null) {
            return new ArrayList<>(note.members);
        }
        return Collections.emptyList();
    }

    @Override
    public void grantReadPermission(int noteId, String ownerLogin, String userLogin) throws SQLException {
        Note note = getNoteById(noteId);
        if (note != null && note.login.equals(ownerLogin)) {
            boolean alreadyHasPermission = notePermissions.stream()
                    .anyMatch(p -> p.noteId == noteId && p.userLogin.equals(userLogin));

            if (!alreadyHasPermission) {
                notePermissions.add(new NotePermission(noteId, userLogin, "READ", ownerLogin));
            }
        }
    }

    @Override
    public boolean hasReadPermission(int noteId, String userLogin) throws SQLException {
        return notePermissions.stream()
                .anyMatch(p -> p.noteId == noteId && p.userLogin.equals(userLogin) && "READ".equals(p.permissionType));
    }

    @Override
    public List<NoteWithOwner> getNotesWithReadPermissions(String userLogin) throws SQLException {
        List<NoteWithOwner> result = new ArrayList<>();

        for (NotePermission permission : notePermissions) {
            if (permission.userLogin.equals(userLogin) && "READ".equals(permission.permissionType)) {
                Note note = getNoteById(permission.noteId);
                if (note != null) {
                    result.add(new NoteWithOwner(note.id, note.text, note.login));
                }
            }
        }

        return result;
    }

    @Override
    public SharedNote getSharedNoteById(int sharedNoteId) throws SQLException {
        InternalSharedNote internalNote = getInternalSharedNoteById(sharedNoteId);
        return internalNote != null ? internalNote.toSharedNote() : null;
    }

    @Override
    public void removeUserFromSharedNote(int sharedNoteId, String userLogin) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        if (note != null) {
            note.members.removeIf(userLogin::equals);
        }
    }

    @Override
    public void addUserToSharedNote(int sharedNoteId, String userLogin, String addedByLogin) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        if (note != null && !note.members.contains(userLogin)) {
            note.members.add(userLogin);
        }
    }

    @Override
    public String getSharedNoteText(int sharedNoteId) throws SQLException {
        InternalSharedNote note = getInternalSharedNoteById(sharedNoteId);
        return note != null ? note.text : null;
    }

    @Override
    public List<AccessibleNote> getAllAccessibleNotesByTag(String login, String tag) throws SQLException {
        String normalizedTag = tag.toLowerCase();
        List<AccessibleNote> result = new ArrayList<>();

        List<Note> userPersonalNotes = personalNotes.getOrDefault(login, Collections.emptyList());
        for (Note note : userPersonalNotes) {
            if (note.tags.contains(normalizedTag)) {
                result.add(new AccessibleNote(
                        note.id,
                        note.text,
                        AccessibleNote.Type.PERSONAL,
                        login,
                        null
                ));
            }
        }

        List<InternalSharedNote> allSharedNotes = new ArrayList<>(sharedNotes);
        for (InternalSharedNote sharedNote : allSharedNotes) {
            if (sharedNote.members.contains(login) && sharedNote.tags.contains(normalizedTag)) {
                result.add(new AccessibleNote(
                        sharedNote.id,
                        sharedNote.text,
                        AccessibleNote.Type.SHARED,
                        sharedNote.creatorLogin,
                        new ArrayList<>(sharedNote.members)
                ));
            }
        }

        List<NoteWithOwner> readableNotes = getNotesWithReadPermissions(login);
        for (NoteWithOwner note : readableNotes) {
            Note internalNote = getNoteById(note.getId());
            if (internalNote != null && internalNote.tags.contains(normalizedTag)) {
                result.add(new AccessibleNote(
                        note.getId(),
                        note.getText(),
                        AccessibleNote.Type.READ_ONLY,
                        note.getOwnerLogin(),
                        null
                ));
            }
        }

        return result;
    }

    @Override
    public List<String> getAllAccessibleTagsWithCounts(String login) throws SQLException {
        Map<String, Integer> tagCount = new HashMap<>();

        List<Note> userPersonalNotes = personalNotes.getOrDefault(login, Collections.emptyList());
        for (Note note : userPersonalNotes) {
            for (String tag : note.tags) {
                tagCount.merge(tag, 1, Integer::sum);
            }
        }

        List<InternalSharedNote> allSharedNotes = new ArrayList<>(sharedNotes);
        for (InternalSharedNote sharedNote : allSharedNotes) {
            if (sharedNote.members.contains(login)) {
                for (String tag : sharedNote.tags) {
                    tagCount.merge(tag, 1, Integer::sum);
                }
            }
        }

        List<NoteWithOwner> readableNotes = getNotesWithReadPermissions(login);
        for (NoteWithOwner note : readableNotes) {
            Note internalNote = getNoteById(note.getId());
            if (internalNote != null) {
                for (String tag : internalNote.tags) {
                    tagCount.merge(tag, 1, Integer::sum);
                }
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
    public List<AccessibleNote> getAllEditableNotes(String login) throws SQLException {
        List<AccessibleNote> result = new ArrayList<>();

        List<Note> userPersonalNotes = personalNotes.getOrDefault(login, Collections.emptyList());
        for (Note note : userPersonalNotes) {
            result.add(new AccessibleNote(
                    note.id,
                    note.text,
                    AccessibleNote.Type.PERSONAL,
                    login,
                    null
            ));
        }

        List<InternalSharedNote> allSharedNotes = new ArrayList<>(sharedNotes);
        for (InternalSharedNote sharedNote : allSharedNotes) {
            if (sharedNote.members.contains(login)) {
                result.add(new AccessibleNote(
                        sharedNote.id,
                        sharedNote.text,
                        AccessibleNote.Type.SHARED,
                        sharedNote.creatorLogin,
                        new ArrayList<>(sharedNote.members)
                ));
            }
        }

        return result;
    }
}