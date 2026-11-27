package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class MarksTest {
    @Test
    void aHunterOfTheFiveHasItsKillsCappedAtTwenty() {
        assertEquals(7, Marks.of(Species.HUNTER_OF_THE_FIVE, 7, 20));
        assertEquals(20, Marks.of(Species.HUNTER_OF_THE_FIVE, 35, 20));
        assertEquals(0, Marks.of(Species.HUNTER_OF_THE_FIVE, -3, 20));
    }

    @Test
    void everyoneElseHasNoMark() {
        for (Species s : Species.values()) {
            if (s != Species.HUNTER_OF_THE_FIVE) {
                assertEquals(0, Marks.of(s, 9, 20), s.name());
            }
        }
    }

    @Test
    void theSenseNeedsFiveKills() {
        assertFalse(Marks.hasSense(Species.HUNTER_OF_THE_FIVE, 4, 5));
        assertTrue(Marks.hasSense(Species.HUNTER_OF_THE_FIVE, 5, 5));
        assertFalse(Marks.hasSense(Species.POTENTIAL, 9, 5));
        assertFalse(Marks.hasSense(Species.NONE, 9, 5));
    }

    @Test
    void lossTable() {
        int[][] rows = {{20, 5}, {12, 3}, {7, 1}, {4, 1}, {3, 1}, {1, 1}, {0, 0}};
        for (int[] r : rows) {
            assertEquals(r[1], Marks.loss(r[0], 25), r[0] + " kills");
        }
    }

    @Test
    void theLossNeverExceedsTheKills() {
        assertEquals(4, Marks.loss(4, 100));
        assertEquals(4, Marks.loss(4, 1000));
        assertEquals(1, Marks.loss(1, 100));
    }

    @Test
    void aPercentOfZeroLosesNothing() {
        assertEquals(0, Marks.loss(20, 0));
        assertEquals(0, Marks.loss(1, 0));
        assertEquals(0, Marks.loss(20, -5));
    }

    @Test
    void afterIsTheKillsMinusTheLoss() {
        assertEquals(15, Marks.after(20, 25));
        assertEquals(0, Marks.after(1, 25));
        assertEquals(0, Marks.after(0, 25));
        assertEquals(20, Marks.after(20, 0));
    }
}