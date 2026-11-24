package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State;
import org.junit.jupiter.api.Test;

class StoneRulesTest {
    @Test
    void activeStateRemovesNamespaceInfinityFromTheTensuraCategory() {
        assertTrue(StoneRules.hidesTensuraEntry(State.ACTIVE, "infinity"));
        assertFalse(StoneRules.hidesTensuraEntry(State.ACTIVE, "tensura"), "another namespace is never hidden");
        assertFalse(StoneRules.hidesTensuraEntry(State.ACTIVE, "infinitystones"));
    }

    @Test
    void offStatesKeepStonesInTheTensuraCategory() {
        for (State s : new State[] {State.OFF_MISSING, State.OFF_BROKEN, State.OFF_SETTING}) {
            assertFalse(StoneRules.hidesTensuraEntry(s, "infinity"), s + ": the stones stay plain Tensura skills");
        }
    }

    @Test
    void aSwitchStaysOpenAndShowsToggled() {
        assertEquals(PowerKind.TOGGLE, StoneRules.kind(true, false, false, false));
        assertEquals(PowerKind.TOGGLE, StoneRules.kind(true, true, true, true), "a switch is a switch before anything else");
    }

    @Test
    void aPowerThatNeedsAimStartsAimNotUse() {
        assertEquals(PowerKind.AIMED, StoneRules.kind(false, false, true, false));
        assertEquals(PowerKind.AIMED, StoneRules.kind(false, true, true, false), "a channel that needs aim is aimed first");
        assertEquals(PowerKind.TOGGLE, StoneRules.kind(false, true, true, true), "and once it runs, the same click stops it");
    }

    @Test
    void aChannelIsStartedAndStoppedByClicksAndIsOnWhileItRuns() {
        assertEquals(PowerKind.TOGGLE, StoneRules.kind(false, true, false, false));
        assertEquals(PowerKind.TOGGLE, StoneRules.kind(false, true, false, true));
        assertEquals(1, StoneRules.phase(false, true, true, false, false), "the second choice stops it");
        assertEquals(0, StoneRules.phase(false, true, false, false, false));
    }

    @Test
    void anythingElseIsUsedAtOnce() {
        assertEquals(PowerKind.INSTANT, StoneRules.kind(false, false, false, false));
    }

    @Test
    void shiftLeftOnADualSendsPhaseTwoAndOnAnythingElsePhaseZero() {
        assertEquals(2, StoneRules.phase(false, false, false, true, true));
        assertEquals(0, StoneRules.phase(false, false, false, true, false), "no second option: the plain use");
        assertEquals(0, StoneRules.phase(false, false, false, false, true));
        assertEquals(0, StoneRules.phase(true, false, false, true, true), "a switch has no second option");
    }

    @Test
    void aStateFromBeforeThisWorldWasJoinedIsNotUsed() {
        assertTrue(StoneRules.stale(1000, 2000));
        assertFalse(StoneRules.stale(2000, 2000));
        assertFalse(StoneRules.stale(5000, 2000), "age alone is not a reason: his server sends on change only");
    }

    @Test
    void soulControlEntryOnlyWhilePossessing() {
        assertFalse(StoneRules.soulEntryShown(State.ACTIVE, true, StoneRules.SOUL_NONE));
        assertTrue(StoneRules.soulEntryShown(State.ACTIVE, true, 1), "spectate");
        assertTrue(StoneRules.soulEntryShown(State.ACTIVE, true, 2), "control");
        assertTrue(StoneRules.soulEntryShown(State.ACTIVE, true, StoneRules.SOUL_MOB));
        assertFalse(StoneRules.soulEntryShown(State.OFF_BROKEN, true, 2));
        assertFalse(StoneRules.soulEntryShown(State.ACTIVE, false, 2));
    }

    @Test
    void soulControlSendsControlAndInMobModeRelease() {
        assertEquals(StoneRules.ACT_CONTROL, StoneRules.soulAction(2));
        assertEquals(StoneRules.ACT_CONTROL, StoneRules.soulAction(1));
        assertEquals(StoneRules.ACT_RELEASE, StoneRules.soulAction(StoneRules.SOUL_MOB), "the same as his key");
        assertEquals(-1, StoneRules.soulAction(StoneRules.SOUL_NONE));
        assertEquals(1, StoneRules.ACT_CONTROL);
        assertEquals(0, StoneRules.ACT_RELEASE);
    }

    @Test
    void theSoulKeyIsCoveredOnlyWhenTheWheelCanDoItsJob() {
        assertTrue(StoneRules.soulControlKeyCovered(State.ACTIVE, true));
        assertFalse(StoneRules.soulControlKeyCovered(State.ACTIVE, false), "no SoulNet.Action: a covered key would strand the player");
        for (State s : new State[] {State.OFF_MISSING, State.OFF_BROKEN, State.OFF_SETTING}) {
            assertFalse(StoneRules.soulControlKeyCovered(s, true), s.name());
        }
    }
}
