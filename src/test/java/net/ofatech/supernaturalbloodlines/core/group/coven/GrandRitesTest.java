package net.ofatech.supernaturalbloodlines.core.group.coven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GrandRitesTest {
    @Test
    void theThreeTiersAreFixed() {
        assertEquals(3, GrandRites.minCircle(GrandRites.FETE, 0, 3, 7, 13));
        assertEquals(7, GrandRites.minCircle(GrandRites.NIGHT_WATCH, 0, 3, 7, 13));
        assertEquals(13, GrandRites.minCircle(GrandRites.GREAT_HARVEST, 0, 3, 7, 13));
        assertEquals(1, GrandRites.tierOf(GrandRites.FETE));
        assertEquals(2, GrandRites.tierOf(GrandRites.NIGHT_WATCH));
        assertEquals(3, GrandRites.tierOf(GrandRites.GREAT_HARVEST));
        assertEquals(0, GrandRites.tierOf("ward_of_daylight"));
    }

    @Test
    void aFilesMinCircleOnlyRaises() {
        assertEquals(5, GrandRites.minCircle(GrandRites.FETE, 5, 3, 7, 13));
        assertEquals(7, GrandRites.minCircle(GrandRites.NIGHT_WATCH, 2, 3, 7, 13));
        assertEquals(9, GrandRites.minCircle(GrandRites.NIGHT_WATCH, 9, 3, 7, 13));
    }

    @Test
    void theTiersFollowTheConfigAsANonDecreasingTriple() {
        assertEquals(7, GrandRites.minCircle(GrandRites.NIGHT_WATCH, 0, 7, 3, 13));
        assertEquals(7, GrandRites.minCircle(GrandRites.GREAT_HARVEST, 0, 7, 3, 5));
    }

    @Test
    void cooldownArithmetic() {
        assertEquals(1000 + 12L * 3_600_000, GrandRites.cooldownUntil(1000, 12));
        assertEquals(1000, GrandRites.cooldownUntil(1000, 0));
    }
}