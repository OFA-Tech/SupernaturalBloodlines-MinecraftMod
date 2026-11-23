package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TensuraRowLayoutTest {
    private static final Object FONT = new Object();

    private static int width(String s) {
        return s.length() * 6;
    }

    private static List<Detail.Row> rows(int n, String text) {
        List<Detail.Row> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new Detail.Row(Component.literal(text + " " + i), null, i, 5));
        }
        return rows;
    }

    private static TensuraRowLayout.Laid lay(List<Detail.Row> rows, int w, int room) {
        return TensuraRowLayout.of(FONT, rows, w, room, TensuraRowLayoutTest::width, tag -> 20);
    }

    @BeforeEach
    void fresh() {
        TensuraRowLayout.clear();
    }

    @Test
    void theSameRowsBuiltAgainEveryFrameAreLaidOutOnce() {
        TensuraRowLayout.Laid first = lay(rows(3, "Feed on villagers"), 150, 200);
        // the page builds its detail again every frame: equal rows, new objects
        TensuraRowLayout.Laid second = lay(rows(3, "Feed on villagers"), 150, 200);
        assertSame(first, second, "a second frame with the same inputs worked everything out again");
    }

    @Test
    void aChangeOfRowsWidthOrRoomIsLaidOutAgain() {
        TensuraRowLayout.Laid base = lay(rows(3, "Feed on villagers"), 150, 200);
        assertNotSame(base, lay(rows(3, "Feed on villagers and hunt"), 150, 200), "other text");
        TensuraRowLayout.Laid wide = lay(rows(3, "Feed on villagers"), 220, 200);
        assertNotSame(base, wide, "another width");
        TensuraRowLayout.Laid tight = lay(rows(3, "Feed on villagers"), 220, 60);
        assertNotSame(wide, tight, "another room");
        List<Detail.Row> moved = rows(3, "Feed on villagers");
        moved.set(1, new Detail.Row(Component.literal("Feed on villagers 1"), null, 4, 5));
        assertNotSame(tight, lay(moved, 220, 60), "other progress");
    }

    @Test
    void everyShownRowHasItsLinesAndCounterAndAShortPaneSaysHowManyAreLeft() {
        TensuraRowLayout.Laid roomy = lay(rows(3, "Feed"), 150, 200);
        assertEquals(3, roomy.plan().shown());
        assertEquals(3, roomy.lines().size());
        assertEquals(List.of("0/5", "1/5", "2/5"), roomy.counts());
        assertNull(roomy.more());
        TensuraRowLayout.Laid tight = lay(rows(5, "Feed"), 150, 70);
        assertTrue(tight.plan().shown() < 5 && tight.plan().more() == 5 - tight.plan().shown());
        assertNotNull(tight.more(), "the line that counts the rest");
        assertEquals(tight.plan().shown(), tight.lines().size());
    }

    @Test
    void aPaneWithNoRoomForTheMoreLineDrawsNoneAndNoRow() {
        TensuraRowLayout.Laid none = lay(rows(4, "Feed"), 150, 6);
        assertEquals(0, none.plan().shown());
        assertNull(none.more(), "the line would stand over the buttons");
    }
}
