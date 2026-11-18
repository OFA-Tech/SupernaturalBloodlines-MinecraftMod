package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClaimAllWaitingTest {
    @Test
    void theButtonStaysShutUntilTheServerHasTakenTheSentTasks() {
        Set<String> sent = Set.of("a", "b");
        assertTrue(ClaimAll.waiting(sent, 1000, List.of("a", "b"), 2000), "still claimable: the sync has not come");
        assertFalse(ClaimAll.waiting(sent, 1000, List.of("c"), 2000), "the synced ladders no longer list them");
        assertFalse(ClaimAll.waiting(sent, 1000, List.of("a"), 1000 + ClaimAll.TIMEOUT_MS), "a silent server does not lock it for ever");
        assertFalse(ClaimAll.waiting(Set.of(), 0, List.of("a"), 10), "nothing was sent");
    }
}

