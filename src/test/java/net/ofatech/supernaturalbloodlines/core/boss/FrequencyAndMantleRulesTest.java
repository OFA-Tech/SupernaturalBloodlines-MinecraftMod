package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.FrequencyRules.Result;
import org.junit.jupiter.api.Test;

class FrequencyAndMantleRulesTest {
    private static Result act(BeastPhase p, int hosts) {
        return FrequencyRules.act(p, hosts, true, true, true);
    }

    @Test
    void theRiteBringsTheHostOnceAndMakesAWaitingHostChangeNow() {
        assertEquals(Result.MAKE_HOST, act(BeastPhase.NONE, 0));
        assertEquals(Result.MAKE_HOST, act(BeastPhase.FAILED, 1), "after the first Host died");
        assertEquals(Result.NO_MORE_HOSTS, act(BeastPhase.FAILED, 2));
        assertEquals(Result.FORCE_CHANGE, act(BeastPhase.HOST, 1));
        assertEquals(Result.FORCE_CHANGE, act(BeastPhase.RETURN_PENDING, 1));
        assertEquals(Result.BUSY, act(BeastPhase.BEAST, 1));
        assertEquals(Result.BUSY, act(BeastPhase.SPIRIT, 1));
        assertEquals(Result.OVER, act(BeastPhase.GONE, 1));
        assertEquals(Result.OVER, act(BeastPhase.KILLED, 1));
    }

    @Test
    void theRiteIsRefusedBySwitchWerewolvesAndMoonInThatOrder() {
        assertEquals(Result.DISABLED, FrequencyRules.act(BeastPhase.NONE, 0, false, false, false), "the switch comes first");
        assertEquals(Result.NEEDS_WEREWOLVES, FrequencyRules.act(BeastPhase.NONE, 0, false, true, false));
        assertEquals(Result.NOT_NIGHT, FrequencyRules.act(BeastPhase.NONE, 0, true, true, false));
        assertEquals(Result.NOT_NIGHT, FrequencyRules.act(BeastPhase.HOST, 1, true, true, false), "a waiting Host is not forced by day either");
    }

    @Test
    void everyResultHasItsOwnMessageKey() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Result r : Result.values()) {
            assertTrue(FrequencyRules.messageKey(r).startsWith("supernaturalbloodlines.rite.frequency."));
            keys.add(FrequencyRules.messageKey(r));
        }
        assertEquals(Result.values().length, keys.size());
    }

    @Test
    void theMantleWorksOnlyForAShiftedWerewolfTypeWhoWearsIt() {
        assertTrue(MantleRules.active(true, true, true));
        assertFalse(MantleRules.active(false, true, true), "not shifted");
        assertFalse(MantleRules.active(true, false, true), "not a werewolf type: a trophy");
        assertFalse(MantleRules.active(true, true, false), "not worn");
    }

    @Test
    void theWrapWaitsTwoMinutesAndReachesSixBlocksThatSpareTheWearerTheirPackAndTheUnhurtable() {
        assertTrue(MantleRules.wrapReady(100, 100));
        assertFalse(MantleRules.wrapReady(99, 100));
        assertEquals(10 + 2400, MantleRules.nextWrapAt(10));
        assertTrue(MantleRules.wraps(36.0, false, false, true), "6 blocks is in");
        assertFalse(MantleRules.wraps(36.1, false, false, true), "past 6 blocks");
        assertFalse(MantleRules.wraps(1.0, true, false, true), "the wearer");
        assertFalse(MantleRules.wraps(1.0, false, true, true), "a packmate");
        assertFalse(MantleRules.wraps(1.0, false, false, false), "somebody who cannot be hurt");
        assertEquals(20, MantleRules.WRAP_SECONDS);
        assertEquals(120, MantleRules.COOLDOWN_SECONDS);
    }
}
