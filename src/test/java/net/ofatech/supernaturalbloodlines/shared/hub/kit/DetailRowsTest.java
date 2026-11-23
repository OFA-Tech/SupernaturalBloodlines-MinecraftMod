package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.DetailRows.Plan;
import org.junit.jupiter.api.Test;

class DetailRowsTest {
    private static final int[] FULL = {36, 36, 26, 36};
    private static final int[] COMPACT = {26, 26, 26, 26};

    @Test
    void everyRowIsDrawnAtItsOwnSizeWhenThereIsRoom() {
        assertEquals(new Plan(4, false, 0, false), DetailRows.plan(FULL, COMPACT, 200, 4, 10));
    }

    @Test
    void whenTheyDoNotFitTheyAllShrinkToOneLineBeforeAnyIsDropped() {
        // 4 x (26 + 4) = 120 fits in 125; at their own size they need 148
        assertEquals(new Plan(4, true, 0, false), DetailRows.plan(FULL, COMPACT, 125, 4, 10));
    }

    @Test
    void aShortPaneShowsAsManyAsFitAndSaysHowManyAreLeft() {
        Plan p = DetailRows.plan(FULL, COMPACT, 80, 4, 10);
        assertEquals(2, p.shown(), "two rows and the line fit in 80: 2 x 30 + 10");
        assertEquals(2, p.more());
        assertTrue(p.compact());
        assertTrue(p.shown() * 30 + 10 <= 80, "the line has its room");
    }

    @Test
    void theMoreLineAlwaysHasRoomAndNoRowIsHalfDrawn() {
        for (int room = 0; room <= 200; room++) {
            Plan p = DetailRows.plan(FULL, COMPACT, room, 4, 10);
            int used = p.shown() * 30 + (p.more() > 0 ? 10 : 0);
            if (p.more() > 0) {
                assertTrue(used <= room || p.shown() == 0, "room " + room + ": " + p);
            }
            assertEquals(4, p.shown() + p.more(), "no task is lost from the count at room " + room);
        }
    }

    @Test
    void aPaneWithNoRoomAtAllStillCountsTheTasks() {
        Plan p = DetailRows.plan(FULL, COMPACT, 5, 4, 10);
        assertEquals(0, p.shown());
        assertEquals(4, p.more());
    }

    @Test
    void theMoreLineIsDrawnOnlyWhereItHasRoomNeverOverTheButtons() {
        assertTrue(!DetailRows.plan(FULL, COMPACT, 5, 4, 10).moreLine(), "no room for a row and none for the line either");
        assertTrue(!DetailRows.plan(FULL, COMPACT, 9, 4, 10).moreLine());
        assertTrue(DetailRows.plan(FULL, COMPACT, 10, 4, 10).moreLine(), "room for the line alone: it says how many there are");
        assertTrue(DetailRows.plan(FULL, COMPACT, 80, 4, 10).moreLine());
        assertTrue(!DetailRows.plan(FULL, COMPACT, 200, 4, 10).moreLine(), "everything fits: no line");
        for (int room = 0; room <= 130; room++) {
            Plan p = DetailRows.plan(FULL, COMPACT, room, 4, 10);
            if (p.moreLine()) {
                assertTrue(p.shown() * 30 + 10 <= room, "room " + room + ": the line is inside it");
            }
        }
    }

    @Test
    void noRowsIsNothingToPlan() {
        assertEquals(new Plan(0, false, 0, false), DetailRows.plan(new int[0], new int[0], 50, 4, 10));
        assertFalse(DetailRows.plan(new int[0], new int[0], 0, 4, 10).compact());
    }
}
