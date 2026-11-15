package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoverRunTest {
    private static final String U = CoveredKeys.UNBOUND;
    private static final List<String> COVERED = List.of("a", "b");

    private final List<String> events = new ArrayList<>();
    private final Map<String, String> binds = new LinkedHashMap<>();
    private final Set<String> unparsable = new java.util.HashSet<>();
    private Map<String, String> stored = new LinkedHashMap<>();
    private boolean writeWorks = true;
    private boolean blocked;

    private final CoverRun.Keys keys = new CoverRun.Keys() {
        @Override
        public Map<String, String> current() {
            return new HashMap<>(binds);
        }

        @Override
        public Map<String, String> defaults() {
            return Map.of("a", "key.keyboard.r", "b", "key.keyboard.v");
        }

        @Override
        public boolean set(String id, String bind) {
            if (unparsable.contains(id)) {
                return false;
            }
            events.add("set " + id);
            binds.put(id, bind);
            return true;
        }

        @Override
        public void save() {
            events.add("save");
        }
    };

    private final CoverRun.Store store = new CoverRun.Store() {
        @Override
        public Map<String, String> read() {
            return new LinkedHashMap<>(stored);
        }

        @Override
        public boolean write(Map<String, String> record) {
            if (!writeWorks) {
                events.add("write failed");
                return false;
            }
            events.add("write");
            stored = new LinkedHashMap<>(record);
            return true;
        }

        @Override
        public boolean blocked() {
            return blocked;
        }
    };

    @Test
    void aFailedRecordWriteLeavesTheBindsUntouched() {
        binds.put("a", "key.keyboard.g");
        writeWorks = false;
        CoverRun.Result r = CoverRun.run(keys, store, COVERED, true);
        assertEquals(CoverRun.Outcome.RECORD_NOT_SAVED, r.outcome());
        assertEquals("key.keyboard.g", binds.get("a"));
        assertEquals(List.of("write failed"), events, "no key set, no options saved");
        assertTrue(stored.isEmpty());
    }

    @Test
    void coveringWritesTheRecordBeforeItUnbinds() {
        binds.put("a", "key.keyboard.g");
        CoverRun.run(keys, store, COVERED, true);
        assertEquals(List.of("write", "set a", "save"), events);
        assertEquals(U, binds.get("a"));
        assertEquals("key.keyboard.g", stored.get("a"));
    }

    @Test
    void restoringSetsTheKeysBeforeItDropsTheRecord() {
        binds.put("a", U);
        stored.put("a", "key.keyboard.g");
        CoverRun.run(keys, store, COVERED, false);
        assertEquals(List.of("set a", "save", "write"), events);
        assertEquals("key.keyboard.g", binds.get("a"));
        assertTrue(stored.isEmpty());
    }

    @Test
    void aBlockedStoreCoversNothing() {
        binds.put("a", "key.keyboard.g");
        blocked = true;
        CoverRun.Result r = CoverRun.run(keys, store, COVERED, true);
        assertEquals(CoverRun.Outcome.BLOCKED, r.outcome());
        assertEquals("key.keyboard.g", binds.get("a"));
        assertTrue(events.isEmpty());
    }

    @Test
    void aBindThatCannotBeAppliedKeepsItsRecord() {
        binds.put("a", U);
        stored.put("a", "key.keyboard.nonsense");
        unparsable.add("a");
        CoverRun.run(keys, store, COVERED, false);
        assertEquals(U, binds.get("a"));
        assertEquals("key.keyboard.nonsense", stored.get("a"), "the record entry was dropped");
    }

    @Test
    void aFailedWriteWhileRestoringStillRestoresTheKey() {
        binds.put("a", U);
        stored.put("a", "key.keyboard.g");
        writeWorks = false;
        CoverRun.Result r = CoverRun.run(keys, store, COVERED, false);
        assertEquals("key.keyboard.g", binds.get("a"));
        assertEquals(CoverRun.Outcome.RECORD_NOT_SAVED, r.outcome());
    }
}
