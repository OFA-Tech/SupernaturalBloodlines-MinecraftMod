package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules.Facts;
import org.junit.jupiter.api.Test;

class MikaelRulesTest {
    private static Facts ok() {
        return new Facts(true, 4, 3, true, true, true, false);
    }

    @Test
    void aHighVampireOnTheGroundAtNightIsATarget() {
        assertTrue(MikaelRules.eligible(ok()));
    }

    @Test
    void aNonVampireIsNot() {
        assertFalse(MikaelRules.eligible(new Facts(false, 4, 3, true, true, true, false)));
    }

    @Test
    void aVampireBelowTheRungIsNotAndOneAtTheRungIs() {
        assertFalse(MikaelRules.eligible(new Facts(true, 2, 3, true, true, true, false)));
        assertTrue(MikaelRules.eligible(new Facts(true, 3, 3, true, true, true, false)));
    }

    @Test
    void daytimeIsNot() {
        assertFalse(MikaelRules.eligible(new Facts(true, 4, 3, false, true, true, false)));
    }

    @Test
    void anotherDimensionIsNot() {
        assertFalse(MikaelRules.eligible(new Facts(true, 4, 3, true, false, true, false)));
    }

    @Test
    void aBodyInTheAirIsNot() {
        assertFalse(MikaelRules.eligible(new Facts(true, 4, 3, true, true, false, false)));
    }

    @Test
    void insideAMountainAshRingIsNot() {
        assertFalse(MikaelRules.eligible(new Facts(true, 4, 3, true, true, true, true)));
    }

    @Test
    void theRollMustBeBelowTheChance() {
        assertTrue(MikaelRules.picks(ok(), 0.15, 0.14));
        assertFalse(MikaelRules.picks(ok(), 0.15, 0.15));
        assertFalse(MikaelRules.picks(new Facts(false, 4, 3, true, true, true, false), 1.0, 0.0));
    }

    @Test
    void aHitOnAVampireHealsAQuarterOfItAndAHitOnAnythingElseHealsNothing() {
        assertEquals(2.0F, MikaelRules.feedHeal(8.0F, true));
        assertEquals(0.0F, MikaelRules.feedHeal(8.0F, false));
        assertEquals(0.0F, MikaelRules.feedHeal(0.0F, true));
    }

    @Test
    void nightIsThirteenThousandToTwentyThreeThousandOfTheDay() {
        assertFalse(MikaelRules.isNight(12999));
        assertTrue(MikaelRules.isNight(13000));
        assertTrue(MikaelRules.isNight(22999));
        assertFalse(MikaelRules.isNight(23000));
        assertTrue(MikaelRules.isNight(24000L * 7 + 15000));
        assertFalse(MikaelRules.isNight(-1000L));
    }

    @Test
    void theChanceIsTheBaseTimesTheWeight() {
        assertEquals(0.3, BossWeight.chance(0.15, 2.0), 1e-9);
        assertEquals(1.0, BossWeight.chance(0.6, 4.0));
        assertEquals(0.0, BossWeight.chance(0.5, -1.0));
    }
}
