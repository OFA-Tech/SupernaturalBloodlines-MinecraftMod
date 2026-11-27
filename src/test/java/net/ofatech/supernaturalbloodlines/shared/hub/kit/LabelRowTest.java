package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/** The moon calendar's day labels never run into each other or out of the row (the Tensura theme's narrow column at gui scale 4). */
class LabelRowTest {
    private static final int COLS = 8;

    /** "Now", "+1" .. "+15" at the vanilla font's widths. */
    private static int width(int offset) {
        return offset == 0 ? 19 : offset < 10 ? 11 : 17;
    }

    @Test
    void noTwoDrawnLabelsTouchAndNoneLeavesTheRow() {
        for (int cell = 11; cell <= 40; cell++) {
            for (int marked = 0; marked < COLS; marked++) {
            for (int rowStart : new int[] {0, COLS}) {
                int widest = 0;
                for (int col = 0; col < COLS; col++) {
                    widest = Math.max(widest, width(rowStart + col));
                }
                boolean thin = LabelRow.thin(widest, cell);
                int lastRight = Integer.MIN_VALUE / 2;
                for (int col = 0; col < COLS; col++) {
                    if (!LabelRow.shown(col, thin, marked)) {
                        assertTrue(col != marked, "the full moon lost its label");
                        continue;
                    }
                    int w = width(rowStart + col);
                    int x = LabelRow.x(100, 100 + COLS * cell, col, cell, w);
                    assertTrue(x >= 100 && x + w <= 100 + COLS * cell, "label " + (rowStart + col) + " leaves the row at cell " + cell);
                    assertTrue(x >= lastRight + LabelRow.GAP, "label " + (rowStart + col) + " touches the one before at cell " + cell);
                    lastRight = x + w;
                }
            }
            }
        }
    }

    @Test
    void aWideColumnShowsEveryLabel() {
        assertFalse(LabelRow.thin(17, 30));
        assertTrue(LabelRow.thin(17, 18));
    }

    @Test
    void theMoonPageUsesIt() {
        String src = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/MoonPage.java");
        assertTrue(src.contains("int cell = r.w() / COLUMNS;") && src.contains("LabelRow.x(r.x(), r.right(),"), "the calendar is wider than its column");
        assertTrue(src.contains("LabelRow.thin(") && src.contains("LabelRow.shown(col, thin[row], MoonCalendar.daysToFull(phase) % COLUMNS)") && src.contains("LabelRow.x("), "MoonPage places its day labels itself");
    }
}
