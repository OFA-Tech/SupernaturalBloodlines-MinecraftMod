package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class ConfirmModelTest {
    @Test
    void escapeAlwaysCancelsAndEnterAloneNeverConfirms() {
        assertEquals(ConfirmModel.Result.CANCEL, new ConfirmModel().press(ConfirmModel.Key.ESCAPE));
        ConfirmModel m = new ConfirmModel();
        assertFalse(m.confirmFocused(), "the safe button has the focus first");
        assertEquals(ConfirmModel.Result.CANCEL, m.press(ConfirmModel.Key.ENTER));
        ConfirmModel moved = new ConfirmModel();
        moved.press(ConfirmModel.Key.TAB);
        assertTrue(moved.confirmFocused());
        assertEquals(ConfirmModel.Result.CANCEL, moved.press(ConfirmModel.Key.ESCAPE), "even with the confirm focused");
    }

    @Test
    void aDeliberateMoveToConfirmThenEnterConfirms() {
        ConfirmModel m = new ConfirmModel();
        assertEquals(ConfirmModel.Result.NONE, m.press(ConfirmModel.Key.TAB));
        assertTrue(m.confirmFocused());
        assertEquals(ConfirmModel.Result.CONFIRM, m.press(ConfirmModel.Key.ENTER));
        ConfirmModel arrows = new ConfirmModel();
        arrows.press(ConfirmModel.Key.RIGHT);
        assertTrue(arrows.confirmFocused());
        arrows.press(ConfirmModel.Key.LEFT);
        assertFalse(arrows.confirmFocused());
        arrows.press(ConfirmModel.Key.TAB);
        arrows.press(ConfirmModel.Key.TAB);
        assertFalse(arrows.confirmFocused());
    }

    @Test
    void aClickConfirmsOnlyOnTheConfirmButton() {
        assertEquals(ConfirmModel.Result.CONFIRM, ConfirmModel.click(true));
        assertEquals(ConfirmModel.Result.CANCEL, ConfirmModel.click(false));
    }

    @Test
    void theDialogFitsEveryScreenWithTwoSeparateButtons() {
        for (int[] s : new int[][] {{320, 240}, {480, 270}, {640, 360}, {960, 540}}) {
            ConfirmLayout l = ConfirmLayout.of(s[0], s[1]);
            Rect screen = new Rect(0, 0, s[0], s[1]);
            assertTrue(l.panel().inside(screen));
            assertTrue(l.cancel().inside(l.panel()) && l.confirm().inside(l.panel()));
            assertFalse(l.cancel().overlaps(l.confirm()));
            assertTrue(l.text().inside(l.panel()) && l.text().h() >= 18, "room for two lines of text at " + s[0]);
            assertFalse(l.text().overlaps(l.cancel()));
        }
    }
}
