package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MorphRulesTest {
    @Test
    void cuesFireOnceAtTheirTicks() {
        int total = 0;
        for (int t = 0; t <= ShiftRules.END_TICK; t++) {
            total += Integer.bitCount(MorphRules.cues(BossForm.SHIFTING, t));
        }
        assertEquals(8, total, "growl 1, dust 4, crack 2, land 1");
        assertEquals(MorphRules.GROWL, MorphRules.cues(BossForm.SHIFTING, 0));
        for (int t : new int[] {4, 8, 12, 16}) {
            assertEquals(MorphRules.DUST, MorphRules.cues(BossForm.SHIFTING, t), "dust at " + t);
        }
        assertEquals(MorphRules.CRACK, MorphRules.cues(BossForm.SHIFTING, 7));
        assertEquals(MorphRules.CRACK, MorphRules.cues(BossForm.SHIFTING, 14));
        assertEquals(MorphRules.LAND, MorphRules.cues(BossForm.SHIFTING, ShiftRules.SWAP_TICK + 12));
        for (BossForm other : new BossForm[] {BossForm.HUMAN, BossForm.BEAST}) {
            for (int t = 0; t <= 200; t++) {
                assertEquals(0, MorphRules.cues(other, t), other + " at " + t);
            }
        }
    }

    @Test
    void noCueAfterTheShiftIsOverOrBeforeItStarts() {
        assertEquals(0, MorphRules.cues(BossForm.SHIFTING, -1));
        assertEquals(0, MorphRules.cues(BossForm.SHIFTING, ShiftRules.SHIFT_TICKS));
        assertEquals(0, MorphRules.cues(BossForm.SHIFTING, 500));
    }

    @Test
    void halvesMeetAtTheSwap() {
        assertTrue(MorphRules.manHalf(BossForm.SHIFTING, 19));
        assertFalse(MorphRules.manHalf(BossForm.SHIFTING, 20));
        assertFalse(MorphRules.arriving(BossForm.SHIFTING, 19));
        assertTrue(MorphRules.arriving(BossForm.SHIFTING, 20));
        assertTrue(MorphRules.arriving(BossForm.SHIFTING, 39));
        assertFalse(MorphRules.arriving(BossForm.SHIFTING, 40));
        for (int t = 0; t < ShiftRules.SHIFT_TICKS; t++) {
            assertEquals(!ShiftRules.showsBeast(BossForm.SHIFTING, t), MorphRules.manHalf(BossForm.SHIFTING, t), "tick " + t);
        }
    }

    @Test
    void lateJoinAnyTick() {
        for (BossForm form : BossForm.values()) {
            for (int t = -5; t <= 200; t++) {
                int holding = (MorphRules.manHalf(form, t) ? 1 : 0) + (MorphRules.arriving(form, t) ? 1 : 0);
                assertTrue(holding <= 1, form + " " + t + ": the man and the arrival cannot both hold");
                if (form != BossForm.SHIFTING) {
                    assertEquals(0, holding, form + " " + t + ": only a shift has halves");
                }
            }
        }
    }

    @Test
    void manProgressIsMonotoneAndCapped() {
        assertEquals(0.0F, MorphRules.manProgress(0, 0.0F), 1e-6);
        float last = -1.0F;
        for (int t = 0; t < ShiftRules.SWAP_TICK; t++) {
            float p = MorphRules.manProgress(t, 0.5F);
            assertTrue(p >= last, "tick " + t);
            assertTrue(p >= 0.0F && p <= 1.0F);
            last = p;
        }
        assertEquals(1.0F, MorphRules.manProgress(19, 0.99F), 0.06F);
        assertEquals(1.0F, MorphRules.manProgress(25, 0.0F), 1e-6);
        assertTrue(MorphRules.manProgress(5, 5.0F) <= (5 + 1) / (float) ShiftRules.SWAP_TICK + 1e-6, "a partial tick over 1 counts as 1");
        assertEquals(0.0F, MorphRules.manProgress(-3, 0.5F), 1e-6);
        assertEquals(0.0F, MorphRules.manProgress(0, Float.NaN), 1e-6);
    }

    @Test
    void deathBeatsArrival() {
        assertEquals("death", KlausRules.animation(true, false, true, true, true, 0.0));
        assertEquals("death", KlausRules.animation(false, true, true, true, true, 0.0));
    }

    @Test
    void arrivalBeatsHowl() {
        // ticks 20 to 39: the wolf arrives although ShiftRules.howling is already true; from 40 the howl clip plays
        for (int t = ShiftRules.SWAP_TICK; t < ShiftRules.SHIFT_TICKS; t++) {
            assertTrue(ShiftRules.howling(BossForm.SHIFTING, t));
            boolean arriving = MorphRules.arriving(BossForm.SHIFTING, t);
            assertEquals("shift_in", KlausRules.animation(false, false, arriving, ShiftRules.howling(BossForm.SHIFTING, t), false, 0.0), "tick " + t);
        }
        BossForm form = BossForm.of(BossForm.BEAST.ordinal());
        assertEquals("howl", KlausRules.animation(false, false, MorphRules.arriving(form, 40), ShiftRules.howling(form, 40), false, 0.0));
        assertEquals("howl", KlausRules.animation(false, false, MorphRules.arriving(BossForm.SHIFTING, 40), ShiftRules.howling(BossForm.SHIFTING, 40), false, 0.0));
    }

    @Test
    void theArrivalStartsAtOnceAndEverythingElseBlendsAsItAlwaysDid() {
        assertEquals(0, MorphRules.blendTicks(true), "a blend would show the standing wolf at the cut");
        assertEquals(4, MorphRules.blendTicks(false));
        assertEquals(MorphRules.BLEND_TICKS, MorphRules.blendTicks(false));
    }

    @Test
    void theOldAnimationChoiceIsUnchanged() {
        assertEquals("death", KlausRules.animation(true, false, false, false, 0.0));
        assertEquals("howl", KlausRules.animation(false, false, true, false, 0.0));
        assertEquals("run", KlausRules.animation(false, false, false, true, 0.5));
        assertEquals("walk", KlausRules.animation(false, false, false, true, 0.1));
        assertEquals("idle", KlausRules.animation(false, false, false, false, 0.0));
    }
}
