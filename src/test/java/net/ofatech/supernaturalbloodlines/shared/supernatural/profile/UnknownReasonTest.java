package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnknownReasonTest {
    @Test
    void aMissingLineIsUnknownWhileOfflineOnlyForAnOfflinePerson() {
        assertTrue(UnknownReason.whileOffline(Fact.LINE, Map.of(Fact.PRESENCE, List.of("offline"))));
        assertFalse(UnknownReason.whileOffline(Fact.LINE, Map.of(Fact.PRESENCE, List.of("online"))), "an online person has a different reason");
        assertFalse(UnknownReason.whileOffline(Fact.LINE, Map.of()), "no presence fact: nothing to say");
        assertFalse(UnknownReason.whileOffline(Fact.LINE, Map.of(Fact.PRESENCE, List.of("offline"), Fact.LINE, List.of("Klaus"))), "a known line is not unknown");
        assertFalse(UnknownReason.whileOffline(Fact.GENES, Map.of(Fact.PRESENCE, List.of("offline"))), "only the line depends on their own record");
    }
}

