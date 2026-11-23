package net.ofatech.supernaturalbloodlines.core.bloodline;

import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.DEATH;
import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.MILD;
import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.SICK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VenomStagesTest {
    private static final int TOTAL = 12000;
    private static final int SICK_AFTER = 4000;

    @Test
    void theFirstThirdIsMild() {
        assertEquals(MILD, VenomStages.of(12000, TOTAL, SICK_AFTER));
        assertEquals(MILD, VenomStages.of(8001, TOTAL, SICK_AFTER));
    }

    @Test
    void sickFromTheThresholdUntilTheLastTick() {
        assertEquals(SICK, VenomStages.of(8000, TOTAL, SICK_AFTER));
        assertEquals(SICK, VenomStages.of(2, TOTAL, SICK_AFTER));
    }

    @Test
    void theLastTickIsDeath() {
        assertEquals(DEATH, VenomStages.of(1, TOTAL, SICK_AFTER));
        assertEquals(DEATH, VenomStages.of(0, TOTAL, SICK_AFTER));
    }

    @Test
    void aThresholdOfZeroIsSickFromTheStart() {
        assertEquals(SICK, VenomStages.of(12000, TOTAL, 0));
        assertEquals(DEATH, VenomStages.of(1, TOTAL, 0));
    }

    @Test
    void aThresholdBeyondTheVenomNeverMakesItSick() {
        assertEquals(MILD, VenomStages.of(2, TOTAL, 12000));
        assertEquals(DEATH, VenomStages.of(1, TOTAL, 12000));
    }

    @Test
    void theSickMessageIsForTheFirstPulseOfTheStageOnly() {
        // pulses come every 40 ticks: 8040 is the last mild one, 8000 the first sick one
        assertFalse(VenomStages.justFellSick(8040, TOTAL, SICK_AFTER, 40));
        assertTrue(VenomStages.justFellSick(8000, TOTAL, SICK_AFTER, 40));
        assertFalse(VenomStages.justFellSick(7960, TOTAL, SICK_AFTER, 40));
        // a threshold that is not a multiple of the pulse still has exactly one first sick pulse
        assertFalse(VenomStages.justFellSick(8040, TOTAL, 3990, 40));
        assertTrue(VenomStages.justFellSick(8000, TOTAL, 3990, 40));
        assertFalse(VenomStages.justFellSick(7960, TOTAL, 3990, 40));
        // sick from the start: the very first pulse is the first
        assertTrue(VenomStages.justFellSick(12000, TOTAL, 0, 40));
        assertFalse(VenomStages.justFellSick(11960, TOTAL, 0, 40));
        // death is not a sick message
        assertFalse(VenomStages.justFellSick(1, TOTAL, SICK_AFTER, 40));
    }
}
