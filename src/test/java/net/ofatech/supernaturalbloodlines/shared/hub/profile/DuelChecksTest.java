package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.shared.hub.profile.DuelChecks.State;
import org.junit.jupiter.api.Test;

class DuelChecksTest {
    @Test
    void nearbyAndFreeMeansTheRulesAreMetExceptTheServersCooldown() {
        var c = DuelChecks.of(31, false);
        assertEquals(State.OK, c.get(0).state());
        assertEquals(State.OK, c.get(1).state());
        assertEquals("31 blocks", c.get(1).detail());
        assertEquals(State.UNKNOWN, c.get(2).state());
        assertEquals(State.OK, c.get(3).state());
    }

    @Test
    void tooFarFailsTheRangeAndAnOpenDuelFailsFree() {
        assertEquals(State.FAIL, DuelChecks.of(80, false).get(1).state());
        assertEquals(State.OK, DuelChecks.of(64, false).get(1).state());
        assertEquals(State.FAIL, DuelChecks.of(10, true).get(3).state());
    }

    @Test
    void notLoadedMeansTheClientCannotSay() {
        var c = DuelChecks.of(-1, false);
        assertEquals(State.UNKNOWN, c.get(0).state());
        assertEquals(State.UNKNOWN, c.get(1).state());
    }
}

