package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LunarMoodsTest {
    @Test
    void noWorldMeansTheNeutralAnswerNotAnException() {
        assertTrue(LunarMoods.now(null).isNone());
        assertTrue(LunarMoods.ahead(null, 6).isEmpty());
        assertEquals(1, LunarMoods.riteCost(null, true));
        assertEquals(1, LunarMoods.biteFactor(null));
        assertEquals(1, LunarMoods.packChanceFactor(null));
        assertEquals(1, LunarMoods.bossWeight(null));
        assertFalse(LunarMoods.sparesForcedShift(null));
        assertFalse(LunarMoods.ecDrawsEclipse(null));
    }

    @Test
    void theOffTuningIsNeutral() {
        LunarMoods.Tuning off = LunarMoods.Tuning.OFF;
        assertEquals(1, off.blueRiteCost());
        assertEquals(1, off.superRiteCost());
        assertEquals(1, off.bloodBite());
        assertEquals(1, off.bloodPackChance());
        assertEquals(1, off.superBossWeight());
        assertFalse(off.superBlueSkipsForcedShift());
    }
}
