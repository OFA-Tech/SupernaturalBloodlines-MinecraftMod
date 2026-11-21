package net.ofatech.supernaturalbloodlines.parts.species.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class MemorySnapshotsTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static MemorySnapshots.Memory memory(String gene) {
        return new MemorySnapshots.Memory(List.of("Anna"), List.of("doppelganger:" + gene));
    }

    @Test
    void whatWasReadIsWhatIsReadAndNothingLaterAndNeverForSomebodyElse() {
        MemorySnapshots s = new MemorySnapshots();
        s.put(USER, SUBJECT, memory("unknown"));
        assertEquals(List.of("doppelganger:unknown"), s.of(USER, SUBJECT).orElseThrow().genes());
        assertTrue(s.of(OTHER, SUBJECT).isEmpty(), "a pack-mate of the user read nothing");
        assertTrue(s.of(USER, OTHER).isEmpty(), "nor did the user read anybody else");
    }

    @Test
    void aSecondReadReplacesTheFirstSoTheUserNeverHoldsTwoVersions() {
        MemorySnapshots s = new MemorySnapshots();
        s.put(USER, SUBJECT, memory("unknown"));
        s.put(USER, SUBJECT, memory("known"));
        assertEquals(List.of("doppelganger:known"), s.of(USER, SUBJECT).orElseThrow().genes());
        assertEquals(1, s.subjectCount(USER));
    }

    @Test
    void aMemoryWithNothingToSayKeepsThatDistinctFromAnEmptyListWhichMeansNone() {
        MemorySnapshots s = new MemorySnapshots();
        s.put(USER, SUBJECT, new MemorySnapshots.Memory(null, List.of()));
        assertNull(s.of(USER, SUBJECT).orElseThrow().sire(), "an NPC Original or a creature as the sire reads \"?\"");
        assertEquals(List.of(), s.of(USER, SUBJECT).orElseThrow().genes());
        MemorySnapshots again = MemorySnapshots.load(s.save(new CompoundTag(), null), null);
        assertNull(again.of(USER, SUBJECT).orElseThrow().sire(), "the difference survives a save");
        assertEquals(List.of(), again.of(USER, SUBJECT).orElseThrow().genes());
    }

    @Test
    void theOldestReadsAreDroppedAtTheCaps() {
        MemorySnapshots s = new MemorySnapshots();
        for (int i = 0; i < MemorySnapshots.MAX_SUBJECTS + 5; i++) {
            s.put(USER, new UUID(0, i), memory("unknown"));
        }
        assertEquals(MemorySnapshots.MAX_SUBJECTS, s.subjectCount(USER));
        assertTrue(s.of(USER, new UUID(0, 0)).isEmpty(), "the oldest went");
        assertTrue(s.of(USER, new UUID(0, MemorySnapshots.MAX_SUBJECTS + 4)).isPresent(), "the newest stayed");
        MemorySnapshots many = new MemorySnapshots();
        for (int i = 0; i < MemorySnapshots.MAX_OBSERVERS + 3; i++) {
            many.put(new UUID(1, i), SUBJECT, memory("unknown"));
        }
        assertEquals(MemorySnapshots.MAX_OBSERVERS, many.observerCount());
        assertTrue(many.of(new UUID(1, 0), SUBJECT).isEmpty());
    }

    @Test
    void readingNeverMovesOrCreatesAnything() {
        MemorySnapshots s = new MemorySnapshots();
        s.put(USER, SUBJECT, memory("unknown"));
        s.of(OTHER, SUBJECT);
        s.of(USER, OTHER);
        assertEquals(1, s.observerCount());
        assertEquals(1, s.subjectCount(USER));
    }

    @Test
    void aSaveRoundTripsAndADamagedEntryIsSkippedNotTheFile() {
        MemorySnapshots s = new MemorySnapshots();
        s.put(USER, SUBJECT, memory("unknown"));
        CompoundTag saved = s.save(new CompoundTag(), null);
        ListTag observers = saved.getList("observers", 10);
        CompoundTag bad = new CompoundTag();
        bad.putString("observer", "not a uuid");
        observers.add(0, bad);
        MemorySnapshots loaded = MemorySnapshots.load(saved, null);
        assertEquals(List.of("Anna"), loaded.of(USER, SUBJECT).orElseThrow().sire());
        assertEquals(1, loaded.observerCount());
        assertFalse(loaded.of(USER, SUBJECT).isEmpty());
    }
}
