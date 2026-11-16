package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class DetailLayoutTest {
    @Test
    void theActionsStackFromTheBottomWithoutTouching() {
        Rect r = new Rect(10, 20, 120, 100);
        List<Rect> stack = DetailLayout.stack(r, 3);
        assertEquals(3, stack.size());
        assertEquals(r.bottom(), stack.get(2).bottom(), "the last sits on the bottom edge");
        for (int i = 0; i < 3; i++) {
            assertTrue(stack.get(i).inside(r));
            if (i > 0) {
                assertFalse(stack.get(i).overlaps(stack.get(i - 1)));
                assertTrue(stack.get(i).y() > stack.get(i - 1).y(), "the first action is the topmost");
            }
        }
    }

    @Test
    void theTextAreaStopsAboveTheActionsSoNothingRunsUnderThem() {
        Rect r = new Rect(0, 0, 100, 100);
        for (int n = 0; n <= 3; n++) {
            Rect text = DetailLayout.textArea(r, n);
            for (Rect b : DetailLayout.stack(r, n)) {
                assertFalse(text.overlaps(b), n + " actions");
            }
            assertEquals(r.h() - DetailLayout.actionsHeight(n), text.h());
        }
        assertEquals(0, DetailLayout.actionsHeight(0));
        assertEquals(0, DetailLayout.textArea(new Rect(0, 0, 50, 10), 3).h(), "never negative");
    }
}
