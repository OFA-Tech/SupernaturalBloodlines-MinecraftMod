package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PeterRulesTest {
    @Test
    void aHouseGeneratesOnlyWhenTheSwitchIsOnAndTheChanceBeatsTheRoll() {
        assertTrue(PeterRules.houseGenerates(true, 0.5, 0.49));
        assertFalse(PeterRules.houseGenerates(true, 0.5, 0.5), "the roll must be under the chance");
        assertFalse(PeterRules.houseGenerates(false, 1.0, 0.0), "the switch is off");
        assertFalse(PeterRules.houseGenerates(true, 0.0, 0.0), "a chance of 0 never generates");
        assertTrue(PeterRules.houseGenerates(true, 7.0, 0.999), "a chance above 1 is 1");
        assertFalse(PeterRules.houseGenerates(true, -3.0, 0.0), "a chance below 0 is 0");
    }

    @Test
    void aPlayerCallsHimOnlyInTheOverworldNearAHouseAndNotAsASpectator() {
        assertTrue(PeterRules.calledBy(true, false, true));
        assertFalse(PeterRules.calledBy(false, false, true));
        assertFalse(PeterRules.calledBy(true, true, true));
        assertFalse(PeterRules.calledBy(true, false, false));
    }

    @Test
    void aHouseIsNearWithinTwentyFourBlocksAndNotOneMore() {
        assertTrue(PeterRules.near(24.0 * 24.0));
        assertFalse(PeterRules.near(24.0 * 24.0 + 1));
        assertTrue(PeterRules.near(0));
    }

    @Test
    void theScanGridCoversTheRadiusInStepsThatAHouseCannotSlipThrough() {
        assertArrayEquals(new int[] {-20, -15, -10, -5, 0, 5, 10, 15, 20}, PeterRules.scanOffsets());
        assertTrue(PeterRules.SCAN_STEP <= 5, "a piece is 5 or more blocks across");
    }

    @Test
    void heShiftsOnlyAsAManAliveAtHalfHealthOrOnAFullMoonNightWhenTheSettingLetsHim() {
        assertTrue(PeterRules.shouldShift(BossForm.HUMAN, 0.5F, false, true));
        assertFalse(PeterRules.shouldShift(BossForm.HUMAN, 0.51F, false, true), "above half and no moon");
        assertTrue(PeterRules.shouldShift(BossForm.HUMAN, 1.0F, true, true), "a full moon night");
        assertFalse(PeterRules.shouldShift(BossForm.HUMAN, 1.0F, true, false), "the feral setting is off");
        assertTrue(PeterRules.shouldShift(BossForm.HUMAN, 0.2F, false, false), "half health still shifts him with the setting off");
        assertFalse(PeterRules.shouldShift(BossForm.BEAST, 0.1F, true, true), "he does not change back and forth");
        assertFalse(PeterRules.shouldShift(BossForm.SHIFTING, 0.1F, true, true));
        assertFalse(PeterRules.shouldShift(BossForm.HUMAN, 0F, true, true), "the dead do not shift");
    }

    @Test
    void heIsFeralOnlyAsTheWolfOnAFullMoonNightWithTheSettingOn() {
        assertTrue(PeterRules.feral(BossForm.BEAST, true, true));
        assertFalse(PeterRules.feral(BossForm.HUMAN, true, true));
        assertFalse(PeterRules.feral(BossForm.SHIFTING, true, true));
        assertFalse(PeterRules.feral(BossForm.BEAST, false, true));
        assertFalse(PeterRules.feral(BossForm.BEAST, true, false));
    }

    @Test
    void aFeralPeterTakesAnythingAliveButHimselfBossesAndPlayersWhoCannotBeHurt() {
        assertTrue(PeterRules.mayTarget(false, false, true, false));
        assertFalse(PeterRules.mayTarget(true, false, true, false), "himself");
        assertFalse(PeterRules.mayTarget(false, true, true, false), "a boss");
        assertFalse(PeterRules.mayTarget(false, false, false, false), "the dead");
        assertFalse(PeterRules.mayTarget(false, false, true, true), "a player in creative or spectator");
    }

    @Test
    void hisSkinIsBurnedUntilHeHasMetAFullMoonAndWholeAfter() {
        assertEquals("peter_hale_burned", PeterRules.skin(false));
        assertEquals("peter_hale", PeterRules.skin(true));
        assertTrue(PeterRules.metFullMoon(false, true));
        assertTrue(PeterRules.metFullMoon(true, false), "once met, it stays met");
        assertFalse(PeterRules.metFullMoon(false, false));
    }

    @Test
    void theWolfIsLongAndLowFromTheSwapTickAndTheManIsAMan() {
        ShiftRules.Size man = PeterRules.dimensionsFor(BossForm.HUMAN, 0);
        assertEquals(0.6F, man.width());
        assertEquals(1.95F, man.height());
        ShiftRules.Size wolf = PeterRules.dimensionsFor(BossForm.BEAST, ShiftRules.SHIFT_TICKS);
        assertEquals(1.4F, wolf.width());
        assertEquals(1.7F, wolf.height());
        assertEquals(1.4F, PeterRules.dimensionsFor(BossForm.SHIFTING, ShiftRules.SWAP_TICK).width(), "the hitbox changes on the swap tick");
        assertEquals(0.6F, PeterRules.dimensionsFor(BossForm.SHIFTING, ShiftRules.SWAP_TICK - 1).width(), "and not before it");
    }

    @Test
    void heLeapsFromThreeToFourPointThreeBlocksOnTheGroundAfterThePause() {
        assertTrue(PeterRules.pounces(BossForm.BEAST, 3.0 * 3.0, true, 0));
        assertTrue(PeterRules.pounces(BossForm.BEAST, 4.3 * 4.3, true, 0));
        assertFalse(PeterRules.pounces(BossForm.BEAST, 2.9 * 2.9, true, 0), "too near");
        assertFalse(PeterRules.pounces(BossForm.BEAST, 4.4 * 4.4, true, 0), "too far");
        assertTrue(PeterRules.POUNCE_FARTHEST <= PeterRules.POUNCE_DISTANCE + PeterRules.POUNCE_REACH, "a pounce that starts must be able to land");
        assertFalse(PeterRules.pounces(BossForm.BEAST, 16.0, false, 0), "in the air");
        assertFalse(PeterRules.pounces(BossForm.BEAST, 16.0, true, 1), "still paused");
        assertFalse(PeterRules.pounces(BossForm.HUMAN, 16.0, true, 0), "the man does not pounce");
    }

    @Test
    void aPounceLandsWhenTheTargetIsWithinReachOfWhereTheAnimationEnds() {
        assertTrue(PeterRules.pounceLands(2.5 * 2.5));
        assertFalse(PeterRules.pounceLands(2.5 * 2.5 + 0.01));
        assertEquals(30.0 / 16.0, PeterRules.POUNCE_DISTANCE, 1e-9, "the root bone of the pounce moves the model 30 pixels");
        assertTrue(PeterRules.POUNCE_LAND_TICK < PeterRules.POUNCE_TICKS);
    }

    @Test
    void theRespawnNeedsTheSwitchADayWrittenDownAndEnoughDays() {
        assertTrue(PeterRules.mayRespawn(true, 12, 5, 7));
        assertFalse(PeterRules.mayRespawn(true, 11, 5, 7), "one day short");
        assertFalse(PeterRules.mayRespawn(false, 99, 5, 7), "the switch is off");
        assertFalse(PeterRules.mayRespawn(true, 99, -1, 7), "no death day written down");
        assertTrue(PeterRules.mayRespawn(true, 6, 5, 0), "zero days is one day");
        assertFalse(PeterRules.mayRespawn(true, 5, 5, 0), "zero days is one day, so the day he died is too early");
        assertFalse(PeterRules.mayRespawn(true, 5, 5, -3), "a negative number of days is one day");
    }

    @Test
    void theHouseFloorAndTheSpawnSpotAgree() {
        assertEquals(PeterRules.FLOOR_LAYER + 1, PeterRules.SPAWN_OFFSET);
        assertEquals(24, PeterRules.HOUSE_RADIUS);
    }
}