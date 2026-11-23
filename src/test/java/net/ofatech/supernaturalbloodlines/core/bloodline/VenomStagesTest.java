package net.ofatech.supernaturalbloodlines.core.bloodline;

import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.DEATH;
import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.MILD;
import static net.ofatech.supernaturalbloodlines.core.bloodline.VenomStages.Stage.SICK;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
