package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LunarMoodsTest {
    @AfterEach
    void reset() {
        LunarMoods.use(LunarSource.NONE, null);
    }

    private static LunarSource source() {
        return new LunarSource() {
            @Override
            public LunarMood now(Level level) {
                return LunarMood.NONE;
            }

            @Override
            public List<Upcoming> ahead(Level level, int days) {
                return List.of();
            }
        };
    }

    private static void fail(LunarMoods.Call kind) {
        LunarMoods.guarded(kind, () -> {
            throw new IllegalStateException("changed");
        }, 0);
    }

    @Test
    void noWorldMeansTheNeutralAnswerNotAnException() {
        assertTrue(LunarMoods.now(null).isNone());
        assertTrue(LunarMoods.ahead(null, 6).isEmpty());
        assertEquals(1, LunarMoods.riteCost(null, true));
        assertEquals(1, LunarMoods.biteFactor(null));
        assertEquals(1, LunarMoods.packChance(null));
        assertFalse(LunarMoods.sparesForcedShift(null));
        assertFalse(LunarMoods.ecDrawsEclipse(null));
    }

    @Test
    void aSourceThatKeepsFailingIsDroppedForGood() {
        LunarSource broken = source();
        LunarMoods.use(broken, () -> LunarMoods.Tuning.OFF);
        for (int i = 0; i < LunarMoods.MAX_FAILURES - 1; i++) {
            fail(LunarMoods.Call.NOW);
        }
        assertEquals(broken, LunarMoods.source(), "dropped too early");
        assertTrue(LunarMoods.active());
        fail(LunarMoods.Call.NOW);
        assertEquals(LunarSource.NONE, LunarMoods.source(), "still in use after repeated failures");
        assertFalse(LunarMoods.active());
    }

    @Test
    void aGoodAnswerBetweenFailuresStartsTheCountAgain() {
        LunarSource flaky = source();
        LunarMoods.use(flaky, () -> LunarMoods.Tuning.OFF);
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < LunarMoods.MAX_FAILURES - 1; i++) {
                fail(LunarMoods.Call.NOW);
            }
            LunarMoods.guarded(LunarMoods.Call.NOW, () -> 1, 0);
        }
        assertEquals(flaky, LunarMoods.source(), "dropped by failures that were not in a row");
    }

    @Test
    void theKindsAreCountedApartSoOneGoodAnswerDoesNotHideAnothersFailures() {
        LunarSource broken = source();
        LunarMoods.use(broken, () -> LunarMoods.Tuning.OFF);
        // the server's mood is fine every tick, the client's schedule fails every second, the mirror too
        for (int i = 0; i < LunarMoods.MAX_FAILURES; i++) {
            LunarMoods.guarded(LunarMoods.Call.NOW, () -> 1, 0);
            fail(LunarMoods.Call.AHEAD);
        }
        assertEquals(LunarSource.NONE, LunarMoods.source(), "a good answer of one kind kept a failing kind alive");
        LunarMoods.use(broken, () -> LunarMoods.Tuning.OFF);
        for (int i = 0; i < LunarMoods.MAX_FAILURES; i++) {
            fail(LunarMoods.Call.MIRROR);
        }
        assertEquals(LunarSource.NONE, LunarMoods.source(), "a broken mirror is asked for ever");
    }

    @Test
    void theOffTuningIsNeutral() {
        LunarMoods.Tuning off = LunarMoods.Tuning.OFF;
        assertEquals(1, off.blueRiteCost());
        assertEquals(1, off.superRiteCost());
        assertEquals(1, off.bloodBite());
        assertEquals(1, off.bloodMoonPackChance());
        assertFalse(off.superBlueSkipsForcedShift());
    }
}
