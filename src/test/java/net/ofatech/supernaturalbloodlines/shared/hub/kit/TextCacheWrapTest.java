package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The rule that a task which does not fit ends its last line with an ellipsis, held where the hub draws it from: {@code TextCache.wrap}. */
class TextCacheWrapTest {
    private static int width(String s) {
        return s.length() * 6;
    }

    private static final Object FONT = new Object();

    @Test
    void aTaskThatDoesNotFitEndsItsLastLineWithAnEllipsisAndEveryLineFits() {
        String task = "Feed on a living villager until the full moon has risen over the hills and then keep on going";
        List<String> lines = TextCache.wrap(FONT, task, 120, 2, TextCacheWrapTest::width);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("\u2026"), "cut silently: " + lines);
        for (String line : lines) {
            assertTrue(width(line) <= 120, line);
        }
        assertTrue(LabelFit.isCut(lines));
    }

    @Test
    void aTaskThatFitsIsLeftWholeAndAsksNoEllipsis() {
        List<String> lines = TextCache.wrap(FONT, "Feed 28 times", 120, 2, TextCacheWrapTest::width);
        assertEquals(List.of("Feed 28 times"), lines);
    }

    @Test
    void theSameTextTwiceIsWorkedOutOnce() {
        int before = TextCache.misses();
        List<String> a = TextCache.wrap(FONT, "A text nobody has asked for yet, long enough to wrap onto a second line", 100, 2, TextCacheWrapTest::width);
        int afterFirst = TextCache.misses();
        List<String> b = TextCache.wrap(FONT, "A text nobody has asked for yet, long enough to wrap onto a second line", 100, 2, TextCacheWrapTest::width);
        assertEquals(before + 1, afterFirst);
        assertEquals(afterFirst, TextCache.misses(), "the second ask was worked out again");
        assertSame(a, b);
    }
}
