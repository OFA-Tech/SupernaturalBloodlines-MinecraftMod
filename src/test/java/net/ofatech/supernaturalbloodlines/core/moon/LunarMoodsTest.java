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
    void aSourceThatKeepsFailingIsDroppedForGood() {
        LunarSource broken = new LunarSource() {
            @Override
            public LunarMood now(net.minecraft.world.level.Level level) {
                throw new IllegalStateException("changed");
            }

            @Override
            public java.util.List<Upcoming> ahead(net.minecraft.world.level.Level level, int days) {
                return java.util.List.of();
            }
        };
        LunarMoods.use(broken, () -> LunarMoods.Tuning.OFF);
        try {
            for (int i = 0; i < LunarMoods.MAX_FAILURES - 1; i++) {
                assertEquals(LunarMood.NONE, LunarMoods.guarded(() -> broken.now(null), LunarMood.NONE));
            }
            assertEquals(broken, LunarMoods.source(), "dropped too early");
            assertEquals(LunarMood.NONE, LunarMoods.guarded(() -> broken.now(null), LunarMood.NONE));
            assertEquals(LunarSource.NONE, LunarMoods.source(), "still in use after repeated failures");
        } finally {
            LunarMoods.use(LunarSource.NONE, null);
        }
    }

    @Test
    void aGoodAnswerBetweenFailuresStartsTheCountAgain() {
        LunarSource flaky = new LunarSource() {
            @Override
            public LunarMood now(net.minecraft.world.level.Level level) {
                return LunarMood.NONE;
            }

            @Override
            public java.util.List<Upcoming> ahead(net.minecraft.world.level.Level level, int days) {
                return java.util.List.of();
            }
        };
        LunarMoods.use(flaky, () -> LunarMoods.Tuning.OFF);
        try {
            for (int round = 0; round < 3; round++) {
                for (int i = 0; i < LunarMoods.MAX_FAILURES - 1; i++) {
                    LunarMoods.guarded(() -> {
                        throw new IllegalStateException("blip");
                    }, 0);
                }
                LunarMoods.guarded(() -> 1, 0);
            }
            assertEquals(flaky, LunarMoods.source(), "dropped by failures that were not in a row");
        } finally {
            LunarMoods.use(LunarSource.NONE, null);
        }
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
