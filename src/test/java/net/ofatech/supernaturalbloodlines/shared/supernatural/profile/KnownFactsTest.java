package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnownFactsTest {
    @Test
    void sizeIsCappedAndTheOldestSubjectGoesFirst() {
        KnownFacts k = new KnownFacts();
        UUID first = UUID.randomUUID();
        k.add(first, EnumSet.of(Fact.GENES));
        for (int i = 0; i < KnownFacts.MAX_SUBJECTS; i++) {
            k.add(UUID.randomUUID(), EnumSet.of(Fact.LINE));
        }
        assertEquals(KnownFacts.MAX_SUBJECTS, k.size());
        assertTrue(k.facts(first).isEmpty(), "the oldest was kept past the cap");
    }

    @Test
    void learningAgainKeepsASubjectLongerAndMergesTheFacts() {
        KnownFacts k = new KnownFacts();
        UUID a = UUID.randomUUID();
        k.add(a, EnumSet.of(Fact.GENES));
        for (int i = 0; i < KnownFacts.MAX_SUBJECTS - 1; i++) {
            k.add(UUID.randomUUID(), EnumSet.of(Fact.LINE));
        }
        k.add(a, EnumSet.of(Fact.SIRE)); // a is the newest now
        k.add(UUID.randomUUID(), EnumSet.of(Fact.LINE));
        assertEquals(EnumSet.of(Fact.GENES, Fact.SIRE), k.facts(a));
    }

    @Test
    void readingFromStorageSkipsWhatItCannotUseInsteadOfFailing() {
        UUID good = UUID.randomUUID();
        Map<String, List<String>> raw = new LinkedHashMap<>();
        raw.put("not a uuid", List.of("GENES"));
        raw.put(good.toString(), List.of("GENES", "FROM_A_LATER_VERSION", "LINE", "GENES"));
        raw.put(UUID.randomUUID().toString(), List.of("NOPE"));
        raw.put(UUID.randomUUID().toString(), null);
        KnownFacts k = KnownFacts.fromStrings(raw);
        assertEquals(1, k.size());
        assertEquals(EnumSet.of(Fact.GENES, Fact.LINE), k.facts(good));
        assertTrue(KnownFacts.fromStrings(null).isEmpty());
    }

    @Test
    void anOverlongStoredListKeepsTheNewestEntries() {
        Map<String, List<String>> raw = new LinkedHashMap<>();
        UUID oldest = UUID.randomUUID();
        UUID newest = UUID.randomUUID();
        raw.put(oldest.toString(), List.of("GENES"));
        for (int i = 0; i < KnownFacts.MAX_SUBJECTS + 50; i++) {
            raw.put(UUID.randomUUID().toString(), List.of("LINE"));
        }
        raw.put(newest.toString(), List.of("SIRE"));
        KnownFacts k = KnownFacts.fromStrings(raw);
        assertEquals(KnownFacts.MAX_SUBJECTS, k.size());
        assertTrue(k.facts(oldest).isEmpty());
        assertEquals(EnumSet.of(Fact.SIRE), k.facts(newest));
    }

    @Test
    void storageRoundTrips() {
        KnownFacts k = new KnownFacts();
        UUID a = UUID.randomUUID();
        k.add(a, EnumSet.of(Fact.GENES, Fact.LINE));
        assertEquals(EnumSet.of(Fact.GENES, Fact.LINE), KnownFacts.fromStrings(k.toStrings()).facts(a));
    }
}

