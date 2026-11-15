package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordFileTest {
    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private RecordFile record() {
        return new RecordFile(dir.resolve("keys.json"), () -> 1000L, warnings::add);
    }

    private List<String> files() throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void writesAtomicallyAndLeavesNoTemporaryFile() throws IOException {
        RecordFile r = record();
        assertTrue(r.write(Map.of("a", "key.keyboard.g:ALT")));
        assertEquals(List.of("keys.json"), files());
        assertEquals(Map.of("a", "key.keyboard.g:ALT"), r.read());
        assertTrue(r.write(Map.of("a", "key.keyboard.h")));
        assertEquals(Map.of("a", "key.keyboard.h"), r.read());
        assertEquals(List.of("keys.json"), files());
    }

    @Test
    void aFailedWriteLeavesTheOldRecordInPlace() throws IOException {
        RecordFile r = record();
        assertTrue(r.write(Map.of("a", "key.keyboard.g")));
        Files.createDirectory(dir.resolve("keys.json.tmp")); // writing the temporary file now fails
        assertFalse(r.write(Map.of("a", "key.keyboard.h")));
        assertEquals(Map.of("a", "key.keyboard.g"), r.read());
        assertFalse(warnings.isEmpty());
    }

    @Test
    void anUnreadableRecordIsSetAsideAndBlocksWriting() throws IOException {
        Files.writeString(dir.resolve("keys.json"), "{ this is not json");
        RecordFile r = record();
        assertTrue(r.read().isEmpty());
        assertEquals(List.of("keys.json.bad-1000"), files(), "the corrupt file is kept, not overwritten");
        assertEquals("{ this is not json", Files.readString(dir.resolve("keys.json.bad-1000")));
        assertTrue(r.blocked());
        assertFalse(r.write(Map.of("a", "key.keyboard.g")), "a new record was written while a .bad file waits");
        assertEquals(List.of("keys.json.bad-1000"), files());
    }

    @Test
    void aSecondCorruptionNeverReplacesTheFirstBadFile() throws IOException {
        Files.writeString(dir.resolve("keys.json"), "first");
        RecordFile r = record();
        r.read();
        Files.writeString(dir.resolve("keys.json"), "second"); // same clock tick on purpose
        r.read();
        assertEquals(List.of("keys.json.bad-1000", "keys.json.bad-1000-1"), files());
        assertEquals("first", Files.readString(dir.resolve("keys.json.bad-1000")));
        assertEquals("second", Files.readString(dir.resolve("keys.json.bad-1000-1")));
    }

    @Test
    void writingIsPossibleAgainOnceThePlayerRemovedTheBadFiles() throws IOException {
        Files.writeString(dir.resolve("keys.json"), "garbage");
        RecordFile r = record();
        r.read();
        Files.delete(dir.resolve("keys.json.bad-1000"));
        assertFalse(r.blocked());
        assertTrue(r.write(Map.of("a", "key.keyboard.g")));
    }
}
