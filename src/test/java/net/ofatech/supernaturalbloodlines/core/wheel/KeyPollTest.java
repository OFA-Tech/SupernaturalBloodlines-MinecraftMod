package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.wheel.KeyPoll.Kind;
import org.junit.jupiter.api.Test;

class KeyPollTest {
    @Test
    void aScanCodeKeyEndsWhenTheWindowStopsBeingActive() {
        assertTrue(KeyPoll.pendingStillDown(Kind.SCANCODE, false, true), "no poll exists: the press stands until the release event");
        assertFalse(KeyPoll.pendingStillDown(Kind.SCANCODE, false, false), "the release went to another window and will never come");
        assertFalse(KeyPoll.pendingStillDown(Kind.SCANCODE, true, false));
    }

    @Test
    void aPolledKeyOrButtonIsHeldOnlyWhileThePollSaysSoAndTheWindowIsActive() {
        for (Kind k : new Kind[] {Kind.KEYSYM, Kind.MOUSE}) {
            assertTrue(KeyPoll.pendingStillDown(k, true, true), k.name());
            assertFalse(KeyPoll.pendingStillDown(k, false, true), k.name());
            assertFalse(KeyPoll.pendingStillDown(k, true, false), k.name());
        }
    }
}
