package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RowFitTest {
    @Test
    void everyRowKeepsItsPlaceSoTheKeyboardReachesTheOnesBelowTheFold() {
        List<RowFit.Row> rows = RowFit.plan(100, 20, 21, 8, 160);
        assertEquals(8, rows.size(), "the fifth wolf is a row, drawn or not");
        assertEquals(100 + 4 * 21, rows.get(4).y());
    }

    @Test
    void aRowIsDrawnOnlyWhenWholeAboveTheClip() {
        List<RowFit.Row> rows = RowFit.plan(100, 20, 21, 5, 160);
        assertTrue(rows.get(0).drawn());
        assertTrue(rows.get(1).drawn());
        assertFalse(rows.get(2).drawn(), "142 to 162 crosses the clip at 160");
        assertFalse(rows.get(3).drawn());
    }
}