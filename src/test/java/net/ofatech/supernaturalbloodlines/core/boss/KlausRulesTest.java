package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.KlausRules.Target;
import org.junit.jupiter.api.Test;

/** R162 and R163: whom Klaus hunts, the warning, the roll, and the bite. */
class KlausRulesTest {
    private static Target player(boolean gene, boolean overworld, boolean spectator) {
        return new Target(true, gene, overworld, spectator, true);
    }

    private static Target villager(boolean doppelganger, boolean near) {
        return new Target(false, doppelganger, true, false, near);
    }

    @Test
    void aPlayerWithTheGeneIsHuntedOnlyWhenTheConfigSaysSo() {
        assertTrue(KlausRules.eligible(player(true, true, false), true));
        assertFalse(KlausRules.eligible(player(true, true, false), false));
    }

    @Test
    void aPlayerWithoutTheGeneIsNeverHunted() {
        assertFalse(KlausRules.eligible(player(false, true, false), true));
    }

    @Test
    void aPlayerOutsideTheOverworldOrInSpectatorIsLeftAlone() {
        assertFalse(KlausRules.eligible(player(true, false, false), true));
        assertFalse(KlausRules.eligible(player(true, true, true), true));
    }

    @Test
    void aDoppelgangerVillagerNearAPlayerIsHuntedWhateverTheConfigSaysAboutPlayers() {
        assertTrue(KlausRules.eligible(villager(true, true), false));
        assertTrue(KlausRules.eligible(villager(true, true), true));
    }

    @Test
    void aVillagerFarFromEveryPlayerOrWithoutTheRollIsNot() {
        assertFalse(KlausRules.eligible(villager(true, false), true));
        assertFalse(KlausRules.eligible(villager(false, true), true));
    }

    @Test
    void aVillagerInAnotherDimensionIsNot() {
        assertFalse(KlausRules.eligible(new Target(false, true, false, false, true), true));
    }

    @Test
    void theRollNeedsAFullMoonNightATargetAndAChanceAboveTheRoll() {
        assertTrue(KlausRules.picks(true, true, 0.35, 0.34));
        assertFalse(KlausRules.picks(true, true, 0.35, 0.35), "a roll of exactly the chance does not pass");
        assertFalse(KlausRules.picks(false, true, 1.0, 0.0), "not a full moon night");
        assertFalse(KlausRules.picks(true, false, 1.0, 0.0), "nobody to hunt");
    }

    @Test
    void aPlayerIsWarnedForTheConfiguredSecondsAndAVillagerNotAtAll() {
        assertEquals(1800L, KlausRules.warningTicks(true, 90));
        assertEquals(0L, KlausRules.warningTicks(false, 90));
        assertEquals(0L, KlausRules.warningTicks(true, 0));
        assertEquals(0L, KlausRules.warningTicks(true, -5));
    }

    @Test
    void theArrivalIsDueAtTheMomentAndOnlyIfOneWaits() {
        assertFalse(KlausRules.arrivalDue(99, 100));
        assertTrue(KlausRules.arrivalDue(100, 100));
        assertFalse(KlausRules.arrivalDue(10_000, -1));
        assertTrue(KlausRules.arrivalDue(5, 0));
    }

    @Test
    void heIsStrandedOnlyWithNoPreyAndNobodyToFight() {
        assertTrue(KlausRules.stranded(false, false));
        assertFalse(KlausRules.stranded(true, false));
        assertFalse(KlausRules.stranded(false, true));
    }

    @Test
    void heWithdrawsAfterAMinuteStrandedAndNotBefore() {
        assertFalse(KlausRules.withdraws(KlausRules.WITHDRAW_AFTER - 10));
        assertTrue(KlausRules.withdraws(KlausRules.WITHDRAW_AFTER));
    }

    @Test
    void dawnIsOutsideTheFullMoonNightWindow() {
        long day = 24000L * 20;
        assertTrue(KlausRules.dawn(day + 6000), "noon");
        assertTrue(KlausRules.dawn(day + net.ofatech.supernaturalbloodlines.core.moon.MoonRule.NIGHT_START), "the night window is open at its start, not before");
        assertFalse(KlausRules.dawn(day + net.ofatech.supernaturalbloodlines.core.moon.MoonRule.NIGHT_START + 1));
        assertFalse(KlausRules.dawn(day + 18000));
        assertFalse(KlausRules.dawn(day + net.ofatech.supernaturalbloodlines.core.moon.MoonRule.NIGHT_END - 1));
        assertTrue(KlausRules.dawn(day + net.ofatech.supernaturalbloodlines.core.moon.MoonRule.NIGHT_END));
        assertTrue(KlausRules.dawn(-1), "a negative time of day wraps");
    }

    @Test
    void heGivesUpAfterFourMinutesWithoutAHitAndNotBefore() {
        assertEquals(4800, KlausRules.GIVE_UP_AFTER);
        assertFalse(KlausRules.givesUp(KlausRules.GIVE_UP_AFTER - 10));
        assertTrue(KlausRules.givesUp(KlausRules.GIVE_UP_AFTER));
    }

    @Test
    void theRemindersComeAtThirtyAndTenSecondsOnceEach() {
        long total = 90 * 20L;
        assertEquals(0, KlausRules.reminder(31 * 20L, total, 0));
        assertEquals(30, KlausRules.reminder(30 * 20L, total, 0));
        assertEquals(0, KlausRules.reminder(25 * 20L, total, 30), "30 is not shown twice");
        assertEquals(10, KlausRules.reminder(10 * 20L, total, 30));
        assertEquals(0, KlausRules.reminder(5 * 20L, total, 10), "10 is not shown twice, and 30 does not come back");
    }

    @Test
    void aLateLookShowsOnlyTheNearestReminder() {
        assertEquals(10, KlausRules.reminder(8 * 20L, 90 * 20L, 0));
        assertEquals(0, KlausRules.reminder(8 * 20L, 90 * 20L, 10));
    }

    @Test
    void aShortWarningIsNotNaggedAtTheSecondsItIsNotLongerThan() {
        assertEquals(0, KlausRules.reminder(30 * 20L, 30 * 20L, 0), "a 30 s warning has no 30 s reminder: it is the warning itself");
        assertEquals(10, KlausRules.reminder(10 * 20L, 30 * 20L, 0));
        assertEquals(0, KlausRules.reminder(5 * 20L, 10 * 20L, 0));
        assertEquals(0, KlausRules.reminder(0L, 0L, 0));
    }

    @Test
    void theWolfPlaysDeathWhenDyingOrDesiccatedThenHowlThenByItsSpeed() {
        assertEquals("death", KlausRules.animation(true, false, false, false, 0));
        assertEquals("death", KlausRules.animation(false, true, true, true, 1), "a desiccated wolf holds death whatever else is true");
        assertEquals("howl", KlausRules.animation(false, false, true, true, 1));
        assertEquals("run", KlausRules.animation(false, false, false, true, KlausRules.RUN_SPEED + 0.01));
        assertEquals("walk", KlausRules.animation(false, false, false, true, KlausRules.RUN_SPEED));
        assertEquals("idle", KlausRules.animation(false, false, false, false, 0.5));
    }

    @Test
    void onlyTheWolfBitesWithVenomAndOnlyAVampireNotAlreadyPoisoned() {
        assertTrue(KlausRules.venomBite(BossForm.BEAST, true, false));
        assertFalse(KlausRules.venomBite(BossForm.HUMAN, true, false));
        assertFalse(KlausRules.venomBite(BossForm.SHIFTING, true, false));
        assertFalse(KlausRules.venomBite(BossForm.BEAST, false, false));
        assertFalse(KlausRules.venomBite(BossForm.BEAST, true, true));
    }
}
