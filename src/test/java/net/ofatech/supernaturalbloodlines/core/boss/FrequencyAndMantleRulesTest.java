package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.FrequencyRules.Result;
import org.junit.jupiter.api.Test;

class FrequencyAndMantleRulesTest {
    private static Result act(BeastPhase p, int hosts) {
        return FrequencyRules.act(p, hosts, true, true, true, true, true);
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
        assertEquals(Result.DISABLED, FrequencyRules.act(BeastPhase.NONE, 0, false, false, false, false, false), "the switch comes first");
        assertEquals(Result.NEEDS_WEREWOLVES, FrequencyRules.act(BeastPhase.NONE, 0, false, true, false, false, false));
        assertEquals(Result.NOT_WEREWOLF, FrequencyRules.act(BeastPhase.NONE, 0, true, true, false, false, false), "somebody who is no werewolf is refused before the place and the night");
        assertEquals(Result.WRONG_PLACE, FrequencyRules.act(BeastPhase.NONE, 0, true, true, true, false, false), "outside the overworld comes before the night");
        assertEquals(Result.NOT_NIGHT, FrequencyRules.act(BeastPhase.NONE, 0, true, true, true, true, false));
        assertEquals(Result.NOT_NIGHT, FrequencyRules.act(BeastPhase.HOST, 1, true, true, true, true, false), "a waiting Host is not forced by day either");
        assertEquals(Result.NOT_WEREWOLF, FrequencyRules.act(BeastPhase.HOST, 1, true, true, false, true, true), "a waiting Host is not forced by a caster who is no werewolf");
    }

    @Test
    void everyReasonHasItsOwnMessageButTheThreeThatTellOfTheStoryAreOneSilence() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Result r : Result.values()) {
            assertTrue(FrequencyRules.messageKey(r).startsWith("supernaturalbloodlines.rite.frequency."));
            keys.add(FrequencyRules.messageKey(r));
        }
        assertEquals(Result.values().length - 2, keys.size(), "BUSY, OVER and NO_MORE_HOSTS are one answer, the rest are not");
        assertEquals(FrequencyRules.messageKey(Result.BUSY), FrequencyRules.messageKey(Result.OVER));
        assertEquals(FrequencyRules.messageKey(Result.BUSY), FrequencyRules.messageKey(Result.NO_MORE_HOSTS));
        assertEquals("supernaturalbloodlines.rite.frequency.silent", FrequencyRules.messageKey(Result.BUSY));
        assertTrue(FrequencyRules.silent(Result.BUSY) && FrequencyRules.silent(Result.OVER) && FrequencyRules.silent(Result.NO_MORE_HOSTS));
        for (Result r : new Result[] {Result.MAKE_HOST, Result.FORCE_CHANGE, Result.DISABLED, Result.NEEDS_WEREWOLVES, Result.NOT_WEREWOLF, Result.WRONG_PLACE, Result.NOT_NIGHT, Result.NO_GROUND}) {
            assertFalse(FrequencyRules.silent(r), r + " is silent");
        }
    }

    @Test
    void theWrapSpendsItsWaitAndSaysSoOnlyWhenItWrappedSomebodyAndHurtsPlayersOnlyWherePvpAndNoTeamAllowIt() {
        assertTrue(MantleRules.spends(1));
        assertTrue(MantleRules.spends(7));
        assertFalse(MantleRules.spends(0), "a Mantle that found no enemy must stay ready");
        assertTrue(MantleRules.playerCanBeHurt(true, true, false));
        assertFalse(MantleRules.playerCanBeHurt(false, true, false), "PvP is off");
        assertFalse(MantleRules.playerCanBeHurt(true, false, false), "a teammate");
        assertFalse(MantleRules.playerCanBeHurt(true, true, true), "a creative or spectating player");
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
