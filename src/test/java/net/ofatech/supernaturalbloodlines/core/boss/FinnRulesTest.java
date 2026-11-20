package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FinnRulesTest {
    @Test
    void heComesUnderAnEclipseForAWitchTypeOrVampireInTheOverworld() {
        assertTrue(FinnRules.eligible(new FinnRules.Facts(true, true, true, false)));
        assertFalse(FinnRules.eligible(new FinnRules.Facts(false, true, true, false)), "no eclipse");
        assertFalse(FinnRules.eligible(new FinnRules.Facts(true, false, true, false)), "not the overworld");
        assertFalse(FinnRules.eligible(new FinnRules.Facts(true, true, false, false)), "not a witch type or a vampire");
        assertFalse(FinnRules.eligible(new FinnRules.Facts(true, true, true, true)), "a spectator");
    }

    @Test
    void oneRollANightAndAFinishedRitualPushesTheNextRollAWeekOut() {
        assertTrue(FinnRules.mayRoll(9, 10));
        assertFalse(FinnRules.mayRoll(10, 10), "already rolled today");
        assertFalse(FinnRules.mayRoll(12, 10));
        long last = FinnRules.afterSuccess(10);
        assertEquals(16, last);
        assertFalse(FinnRules.mayRoll(last, 16), "day 16 is the sixth day after");
        assertTrue(FinnRules.mayRoll(last, 17), "a week later he may roll again");
        assertEquals(7, FinnRules.COOLDOWN_DAYS);
    }

    @Test
    void theRitualRunsUntilItsTimeUnlessHurtOrPulledAwayOrTheEclipseEnds() {
        int total = FinnRules.totalTicks(120);
        assertEquals(2400, total);
        assertEquals(FinnRules.Step.CHANNELING, FinnRules.step(10, total, 0F, 100F, 0, true));
        assertEquals(FinnRules.Step.DONE, FinnRules.step(total, total, 0F, 100F, 0, true));
        assertEquals(FinnRules.Step.DONE, FinnRules.step(total + 50, total, 0F, 100F, 0, true));
        assertEquals(FinnRules.Step.ENDED, FinnRules.step(10, total, 0F, 100F, 0, false), "the eclipse is over");
        assertEquals(FinnRules.Step.DONE, FinnRules.step(total, total, 0F, 100F, 0, false), "done wins over the eclipse ending on the last tick");
    }

    @Test
    void moreThanAFifthOfHisHealthOrMoreThanTwentyFourBlocksBreaksItEvenOnTheLastTick() {
        int total = FinnRules.totalTicks(120);
        assertEquals(FinnRules.Step.CHANNELING, FinnRules.step(10, total, 20F, 100F, 0, true), "exactly a fifth");
        assertEquals(FinnRules.Step.INTERRUPTED, FinnRules.step(10, total, 20.1F, 100F, 0, true));
        assertEquals(FinnRules.Step.CHANNELING, FinnRules.step(10, total, 0F, 100F, FinnRules.LEASH, true), "exactly at the leash");
        assertEquals(FinnRules.Step.INTERRUPTED, FinnRules.step(10, total, 0F, 100F, FinnRules.LEASH + 0.1, true));
        assertEquals(FinnRules.Step.INTERRUPTED, FinnRules.step(total, total, 50F, 100F, 0, true), "hurt on the last tick still breaks it");
        assertEquals(FinnRules.Step.INTERRUPTED, FinnRules.step(10, total, 50F, 100F, 0, false), "hurt wins over the eclipse ending");
    }

    @Test
    void aSettingOfZeroSecondsStillTakesATick() {
        assertEquals(20, FinnRules.totalTicks(0));
        assertEquals(20, FinnRules.totalTicks(-4));
    }
}