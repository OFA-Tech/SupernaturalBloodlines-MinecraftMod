package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.moon.LunarMood.Kind;
import org.junit.jupiter.api.Test;

class LunarMoodTest {
    private static final String DLE = LunarMood.EC_DEFAULT_NAMESPACE;

    @Test
    void theDefaultEventsMapToTheirMoods() {
        assertEquals(new LunarMood(Kind.BLOOD, false), LunarMood.ofEvent(DLE, "blood_moon"));
        assertEquals(new LunarMood(Kind.BLUE, false), LunarMood.ofEvent(DLE, "blue_moon"));
        assertEquals(new LunarMood(Kind.HARVEST, false), LunarMood.ofEvent(DLE, "harvest_moon"));
        assertEquals(new LunarMood(Kind.BLOOD, true), LunarMood.ofEvent(DLE, "super_blood_moon"));
        assertEquals(new LunarMood(Kind.BLUE, true), LunarMood.ofEvent(DLE, "super_blue_moon"));
        assertEquals(new LunarMood(Kind.HARVEST, true), LunarMood.ofEvent(DLE, "super_harvest_moon"));
        assertEquals(new LunarMood(Kind.NONE, true), LunarMood.ofEvent(DLE, "super_moon"));
    }

    @Test
    void ourEclipseIsTheEclipseKind() {
        assertEquals(new LunarMood(Kind.ECLIPSE, false), LunarMood.ofEvent("supernaturalbloodlines", "lunar_eclipse"));
        assertTrue(LunarMood.ofEvent("supernaturalbloodlines", "something_else").isNone());
    }

    @Test
    void anythingElseIsNoMood() {
        assertTrue(LunarMood.ofEvent("enhancedcelestials2core", "default").isNone());
        assertTrue(LunarMood.ofEvent(DLE, "horde_moon").isNone());
        assertTrue(LunarMood.ofEvent("someaddon", "blood_moon").isNone(), "another add-on's event with the same name");
        assertTrue(LunarMood.NONE.isNone());
        assertFalse(new LunarMood(Kind.NONE, true).isNone(), "a plain super moon is still a super moon");
    }

    @Test
    void withoutAMoodEveryFactorIsOne() {
        LunarMood none = LunarMood.NONE;
        assertEquals(1, none.riteCost(true, 0.8, 0.75));
        assertEquals(1, none.riteCost(false, 0.8, 0.75));
        assertEquals(1, none.biteFactor(1.25));
        assertFalse(none.sparesForcedShift(true));
    }

    @Test
    void aBlueMoonCutsEveryRiteAndNeverAllowsOneOnAnotherNight() {
        LunarMood blue = new LunarMood(Kind.BLUE, false);
        assertEquals(0.8, blue.riteCost(false, 0.8, 0.75));
        assertEquals(0.8, blue.riteCost(true, 0.8, 0.75), "a plain Blue Moon is not a super moon");
        assertEquals(1, blue.biteFactor(1.25));
    }

    @Test
    void aSuperMoonCutsOnlyTheFullMoonRitesOnTopOfTheBlueOne() {
        assertEquals(0.75, new LunarMood(Kind.NONE, true).riteCost(true, 0.8, 0.75));
        assertEquals(1, new LunarMood(Kind.NONE, true).riteCost(false, 0.8, 0.75));
        assertEquals(0.8 * 0.75, new LunarMood(Kind.BLUE, true).riteCost(true, 0.8, 0.75));
        assertEquals(0.8, new LunarMood(Kind.BLUE, true).riteCost(false, 0.8, 0.75));
    }

    @Test
    void aBloodMoonIsForBitesAndPacksOnly() {
        LunarMood blood = new LunarMood(Kind.BLOOD, false);
        assertEquals(1.25, blood.biteFactor(1.25));
        assertEquals(1, blood.riteCost(true, 0.8, 0.75));
        assertFalse(blood.sparesForcedShift(true));
    }

    @Test
    void onlyASuperBlueMoonSparesTheForcedShiftAndOnlyWithTheSwitch() {
        assertTrue(new LunarMood(Kind.BLUE, true).sparesForcedShift(true));
        assertFalse(new LunarMood(Kind.BLUE, true).sparesForcedShift(false));
        assertFalse(new LunarMood(Kind.BLUE, false).sparesForcedShift(true));
        assertFalse(new LunarMood(Kind.BLOOD, true).sparesForcedShift(true));
        assertFalse(new LunarMood(Kind.NONE, true).sparesForcedShift(true));
        assertFalse(new LunarMood(Kind.HARVEST, true).sparesForcedShift(true));
    }

    @Test
    void scaledRoundsHalfUpAndOneLeavesAnAmountAlone() {
        assertEquals(5, LunarMood.scaled(4, 1.25));
        assertEquals(10, LunarMood.scaled(8, 1.25));
        assertEquals(1, LunarMood.scaled(1, 1.25), "a bite of 1 does not grow by a quarter");
        assertEquals(3, LunarMood.scaled(2, 1.25));
        assertEquals(0, LunarMood.scaled(0, 1.25), "an emptied bite stays empty");
        assertEquals(7, LunarMood.scaled(7, 1));
    }
}
