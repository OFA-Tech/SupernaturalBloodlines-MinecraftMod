package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules.Size;
import org.junit.jupiter.api.Test;

/** R161: when he shifts, how long it takes, how big each body is. */
class ShiftRulesTest {
    @Test
    void aManShiftsAtHalfHealthOrLess() {
        assertFalse(ShiftRules.shouldShift(BossForm.HUMAN, 0.51F, false));
        assertTrue(ShiftRules.shouldShift(BossForm.HUMAN, 0.5F, false));
        assertTrue(ShiftRules.shouldShift(BossForm.HUMAN, 0.1F, false));
    }

    @Test
    void aManShiftsAgainstAWerewolfAtAnyHealth() {
        assertTrue(ShiftRules.shouldShift(BossForm.HUMAN, 1.0F, true));
        assertFalse(ShiftRules.shouldShift(BossForm.HUMAN, 1.0F, false));
    }

    @Test
    void aDeadManDoesNotShift() {
        assertFalse(ShiftRules.shouldShift(BossForm.HUMAN, 0F, true));
    }

    @Test
    void onlyTheHumanFormShiftsSoTheShiftIsOnceAndForever() {
        assertFalse(ShiftRules.shouldShift(BossForm.SHIFTING, 0.1F, true));
        assertFalse(ShiftRules.shouldShift(BossForm.BEAST, 0.1F, true));
    }

    @Test
    void theShiftEndsAfterFortyTicksAndOnlyThen() {
        assertEquals(BossForm.SHIFTING, ShiftRules.afterTick(BossForm.SHIFTING, ShiftRules.SHIFT_TICKS - 1));
        assertEquals(BossForm.BEAST, ShiftRules.afterTick(BossForm.SHIFTING, ShiftRules.SHIFT_TICKS));
        assertEquals(BossForm.HUMAN, ShiftRules.afterTick(BossForm.HUMAN, 999));
        assertEquals(BossForm.BEAST, ShiftRules.afterTick(BossForm.BEAST, 0));
    }

    @Test
    void theWolfModelShowsFromHalfWayThrough() {
        assertFalse(ShiftRules.showsBeast(BossForm.HUMAN, 999));
        assertFalse(ShiftRules.showsBeast(BossForm.SHIFTING, ShiftRules.SHIFT_TICKS / 2 - 1));
        assertTrue(ShiftRules.showsBeast(BossForm.SHIFTING, ShiftRules.SHIFT_TICKS / 2));
        assertTrue(ShiftRules.showsBeast(BossForm.BEAST, 0));
    }

    @Test
    void progressIsKeptBetweenZeroAndOne() {
        assertEquals(0F, ShiftRules.progress(-5));
        assertEquals(0.5F, ShiftRules.progress(ShiftRules.SHIFT_TICKS / 2));
        assertEquals(1F, ShiftRules.progress(ShiftRules.SHIFT_TICKS * 3));
    }

    @Test
    void theWolfIsSmallerThanTheManButBroader() {
        Size man = ShiftRules.dimensionsFor(BossForm.HUMAN);
        Size wolf = ShiftRules.dimensionsFor(BossForm.BEAST);
        assertEquals(new Size(0.6F, 1.95F), man);
        assertEquals(new Size(0.9F, 1.5F), wolf);
        assertEquals(man, ShiftRules.dimensionsFor(BossForm.SHIFTING));
    }

    @Test
    void theHowlPlaysFromTheStartOfTheShiftForItsLength() {
        assertFalse(ShiftRules.howling(BossForm.HUMAN, 0));
        assertTrue(ShiftRules.howling(BossForm.SHIFTING, 0));
        assertTrue(ShiftRules.howling(BossForm.BEAST, ShiftRules.HOWL_TICKS - 1));
        assertFalse(ShiftRules.howling(BossForm.BEAST, ShiftRules.HOWL_TICKS));
    }

    @Test
    void aFormFromTheWireOrASaveThatIsOutOfRangeIsHuman() {
        assertEquals(BossForm.BEAST, BossForm.of(BossForm.BEAST.ordinal()));
        assertEquals(BossForm.HUMAN, BossForm.of(-1));
        assertEquals(BossForm.HUMAN, BossForm.of(3));
    }
}
