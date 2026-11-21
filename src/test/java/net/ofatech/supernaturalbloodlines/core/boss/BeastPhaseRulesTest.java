package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.BeastPhaseRules.Event;
import org.junit.jupiter.api.Test;

class BeastPhaseRulesTest {
    private static BeastPhase next(BeastPhase p, float health, boolean trapped, int spirit, boolean returned, boolean returnsOnce) {
        return BeastPhaseRules.next(p, health, trapped, spirit, returned, returnsOnce);
    }

    @Test
    void theBeastCollapsesAtAQuarterOfItsHealthAndNotBefore() {
        assertEquals(BeastPhase.BEAST, next(BeastPhase.BEAST, 0.26F, false, 0, false, true));
        assertEquals(BeastPhase.SPIRIT, next(BeastPhase.BEAST, 0.25F, false, 0, false, true), "the edge collapses");
        assertEquals(BeastPhase.SPIRIT, next(BeastPhase.BEAST, 0.01F, false, 0, false, true));
        assertEquals(BeastPhase.BEAST, next(BeastPhase.BEAST, 1.0F, false, 0, false, true));
    }

    @Test
    void aBeastTrappedInMountainAshCollapsesWhateverItsHealth() {
        assertEquals(BeastPhase.SPIRIT, next(BeastPhase.BEAST, 1.0F, true, 0, false, true));
        assertEquals(BeastPhase.SPIRIT, next(BeastPhase.BEAST, 0.6F, true, 0, false, true));
    }

    @Test
    void aDyingBeastDoesNotCollapseItDies() {
        assertEquals(BeastPhase.BEAST, next(BeastPhase.BEAST, 0F, false, 0, false, true));
        assertEquals(BeastPhase.BEAST, next(BeastPhase.BEAST, 0F, true, 0, false, true));
    }

    @Test
    void theSpiritKeepsItsWindowAndThenReturnsOnceOrIsGone() {
        assertEquals(BeastPhase.SPIRIT, next(BeastPhase.SPIRIT, 0.1F, false, 1, false, true), "one tick left is still the window");
        assertEquals(BeastPhase.RETURN_PENDING, next(BeastPhase.SPIRIT, 0.1F, false, 0, false, true), "the first escape returns");
        assertEquals(BeastPhase.GONE, next(BeastPhase.SPIRIT, 0.1F, false, 0, true, true), "the second escape is for good");
        assertEquals(BeastPhase.GONE, next(BeastPhase.SPIRIT, 0.1F, false, -5, false, false), "with the setting off it never returns");
    }

    @Test
    void noOtherPhaseMovesByItself() {
        for (BeastPhase p : BeastPhase.values()) {
            if (p != BeastPhase.BEAST && p != BeastPhase.SPIRIT) {
                assertEquals(p, next(p, 0.1F, true, 0, false, true), p + " moved by itself");
            }
        }
    }

    @Test
    void eventsMoveOnlyThePhasesTheyBelongTo() {
        assertEquals(BeastPhase.HOST, BeastPhaseRules.after(BeastPhase.NONE, Event.HOST_MADE));
        assertEquals(BeastPhase.HOST, BeastPhaseRules.after(BeastPhase.FAILED, Event.HOST_MADE));
        assertEquals(BeastPhase.KILLED, BeastPhaseRules.after(BeastPhase.KILLED, Event.HOST_MADE), "a finished story starts nothing");
        assertEquals(BeastPhase.BEAST, BeastPhaseRules.after(BeastPhase.BEAST, Event.HOST_MADE));
        assertEquals(BeastPhase.BEAST, BeastPhaseRules.after(BeastPhase.HOST, Event.CHANGE));
        assertEquals(BeastPhase.BEAST, BeastPhaseRules.after(BeastPhase.RETURN_PENDING, Event.CHANGE));
        assertEquals(BeastPhase.SPIRIT, BeastPhaseRules.after(BeastPhase.SPIRIT, Event.CHANGE), "a collapsed Beast does not change again");
        assertEquals(BeastPhase.KILLED, BeastPhaseRules.after(BeastPhase.SPIRIT, Event.PIKE_HIT));
        assertEquals(BeastPhase.BEAST, BeastPhaseRules.after(BeastPhase.BEAST, Event.PIKE_HIT), "the pike kills only the Spirit");
        assertEquals(BeastPhase.HOST, BeastPhaseRules.after(BeastPhase.HOST, Event.PIKE_HIT));
    }

    @Test
    void aHostThatDiesFailsTheEncounterWhereverItWasNeededButNeverAFinishedOne() {
        assertEquals(BeastPhase.FAILED, BeastPhaseRules.after(BeastPhase.HOST, Event.HOST_LOST));
        assertEquals(BeastPhase.FAILED, BeastPhaseRules.after(BeastPhase.SPIRIT, Event.HOST_LOST));
        assertEquals(BeastPhase.FAILED, BeastPhaseRules.after(BeastPhase.RETURN_PENDING, Event.HOST_LOST));
        assertEquals(BeastPhase.GONE, BeastPhaseRules.after(BeastPhase.GONE, Event.HOST_LOST), "after it got away nothing can be farmed");
        assertEquals(BeastPhase.KILLED, BeastPhaseRules.after(BeastPhase.KILLED, Event.HOST_LOST));
        assertEquals(BeastPhase.NONE, BeastPhaseRules.after(BeastPhase.NONE, Event.HOST_LOST));
    }

    @Test
    void resetStartsOverFromAnyPhase() {
        for (BeastPhase p : BeastPhase.values()) {
            assertEquals(BeastPhase.NONE, BeastPhaseRules.after(p, Event.RESET));
        }
    }

    @Test
    void aHostMayBeMadeTwiceInAWorldAndOnlyAfterNothingOrAFailure() {
        assertTrue(BeastPhaseRules.hostMayBeMade(BeastPhase.NONE, 0));
        assertTrue(BeastPhaseRules.hostMayBeMade(BeastPhase.FAILED, 1));
        assertFalse(BeastPhaseRules.hostMayBeMade(BeastPhase.FAILED, 2), "a second respawn would be a farm");
        assertFalse(BeastPhaseRules.hostMayBeMade(BeastPhase.NONE, 2));
        for (BeastPhase p : new BeastPhase[] {BeastPhase.HOST, BeastPhase.BEAST, BeastPhase.SPIRIT, BeastPhase.RETURN_PENDING, BeastPhase.GONE, BeastPhase.KILLED}) {
            assertFalse(BeastPhaseRules.hostMayBeMade(p, 0), p + " may not be made again");
        }
    }

    @Test
    void theChangeComesOnALaterFullMoonNightOrWhenForcedAndOnlyForAWaitingHost() {
        assertTrue(BeastPhaseRules.changeDue(BeastPhase.HOST, false, true, 9, 1), "a later full moon night");
        assertFalse(BeastPhaseRules.changeDue(BeastPhase.HOST, false, true, 1, 1), "the night the Host was made is not a later one");
        assertFalse(BeastPhaseRules.changeDue(BeastPhase.HOST, false, false, 9, 1), "no full moon");
        assertTrue(BeastPhaseRules.changeDue(BeastPhase.HOST, true, false, 1, 1), "forced, any night");
        assertTrue(BeastPhaseRules.changeDue(BeastPhase.RETURN_PENDING, false, true, 9, 1));
        assertFalse(BeastPhaseRules.changeDue(BeastPhase.RETURN_PENDING, false, true, 1, 1));
        for (BeastPhase p : new BeastPhase[] {BeastPhase.NONE, BeastPhase.BEAST, BeastPhase.SPIRIT, BeastPhase.GONE, BeastPhase.KILLED, BeastPhase.FAILED}) {
            assertFalse(BeastPhaseRules.changeDue(p, true, true, 99, 1), p + " changes");
        }
    }

    @Test
    void aReturningBeastStartsAtHalfAndANewOneAtFull() {
        assertEquals(0.5F, BeastPhaseRules.startHealth(true));
        assertEquals(1.0F, BeastPhaseRules.startHealth(false));
        assertEquals(0.5F, BeastPhaseRules.RETURN_HEALTH);
        assertEquals(0.25F, BeastPhaseRules.COLLAPSE_AT);
    }

    @Test
    void theHostIsProtectedExactlyWhileTheEncounterNeedsIt() {
        for (BeastPhase p : BeastPhase.values()) {
            boolean expected = p == BeastPhase.HOST || p == BeastPhase.SPIRIT || p == BeastPhase.RETURN_PENDING;
            assertEquals(expected, BeastPhaseRules.hostProtected(p), p.name());
        }
    }

    @Test
    void thePhaseKnowsWhereAHostStandsAndWhenTheStoryIsOver() {
        assertTrue(BeastPhase.HOST.hasHost() && BeastPhase.SPIRIT.hasHost() && BeastPhase.RETURN_PENDING.hasHost() && BeastPhase.GONE.hasHost() && BeastPhase.KILLED.hasHost());
        assertFalse(BeastPhase.NONE.hasHost() || BeastPhase.BEAST.hasHost() || BeastPhase.FAILED.hasHost());
        assertTrue(BeastPhase.GONE.over() && BeastPhase.KILLED.over());
        assertFalse(BeastPhase.HOST.over() || BeastPhase.FAILED.over() || BeastPhase.NONE.over());
        assertEquals(BeastPhase.NONE, BeastPhase.of("nonsense"), "a bad saved name starts over");
        assertEquals(BeastPhase.SPIRIT, BeastPhase.of("SPIRIT"));
    }
}
