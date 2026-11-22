package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.RingLabels.Anchor;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.RingLabels.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextCacheTest {
    private static final Object FONT = new Object();

    private static int width(String s) {
        return s.length() * 6;
    }

    @BeforeEach
    void fresh() {
        TextCache.clear();
    }

    @Test
    void aSecondFrameWithTheSameInputsReusesTheNamesTheFirstOneWorkedOut() {
        List<Request> asked = List.of(new Request(100, 50, 40, 9, Anchor.CENTER), new Request(120, 50, 40, 9, Anchor.CENTER));
        List<Rect> blocked = List.of(new Rect(90, 70, 20, 20));
        Rect page = new Rect(0, 0, 400, 300);
        List<Rect> first = TextCache.ring(asked, blocked, page, 12, 2);
        int after = TextCache.misses();
        // a new frame builds new lists with the same content
        List<Rect> second = TextCache.ring(List.of(new Request(100, 50, 40, 9, Anchor.CENTER), new Request(120, 50, 40, 9, Anchor.CENTER)), List.of(new Rect(90, 70, 20, 20)), page, 12, 2);
        assertEquals(after, TextCache.misses(), "the second frame worked it out again");
        assertEquals(first, second);
        for (int i = 0; i < 3; i++) {
            TextCache.wrap(FONT, "Silver Tongue of the Pack", 60, 2, TextCacheTest::width);
            TextCache.cut(FONT, "Silver Tongue of the Pack", 60, TextCacheTest::width);
            TextCache.dropCap(FONT, "ampire and werewolf", 100, 13, 2, TextCacheTest::width);
        }
        assertEquals(after + 3, TextCache.misses(), "one answer for each, however many frames");
    }

    @Test
    void aChangedSizeOrTextIsAnotherKeySoNothingStaleIsShown() {
        int base = TextCache.misses();
        String a = TextCache.cut(FONT, "Silver Tongue", 40, TextCacheTest::width);
        String b = TextCache.cut(FONT, "Silver Tongue", 90, TextCacheTest::width);
        String c = TextCache.cut(FONT, "Lengua de plata", 90, TextCacheTest::width);
        assertNotEquals(a, b);
        assertEquals("Lengua de plata", c);
        assertEquals(base + 3, TextCache.misses());
    }

    @Test
    void clearForgetsWhatWasWorkedOut() {
        int base = TextCache.misses();
        TextCache.cut(FONT, "Venom Fang", 30, TextCacheTest::width);
        TextCache.clear();
        TextCache.cut(FONT, "Venom Fang", 30, TextCacheTest::width);
        assertEquals(base + 2, TextCache.misses());
    }
}