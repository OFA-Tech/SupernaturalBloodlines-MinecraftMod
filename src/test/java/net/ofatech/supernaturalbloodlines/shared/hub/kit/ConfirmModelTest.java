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
            for (int lines : new int[] {1, 3, 12, 80}) {
                ConfirmLayout l = ConfirmLayout.of(s[0], s[1], lines);
                String at = s[0] + "x" + s[1] + " " + lines + " lines";
                Rect screen = new Rect(0, 0, s[0], s[1]);
                assertTrue(l.panel().inside(screen), at);
                assertTrue(l.safe().inside(l.panel()) && l.confirm().inside(l.panel()), at);
                assertFalse(l.safe().overlaps(l.confirm()), at);
                assertTrue(l.safe().x() < l.confirm().x(), at + ": the safe button is on the left");
                assertTrue(l.text().inside(l.panel()) && l.maxLines() >= 1, at);
                assertFalse(l.text().overlaps(l.safe()), at);
                assertTrue(l.panel().h() <= Math.max(ConfirmLayout.MIN_H, s[1] * 7 / 10), at + ": more than 70% of the screen");
            }
        }
    }

    @Test
    void theDialogGrowsWithTheBodyUpToTheLimit() {
        int one = ConfirmLayout.of(480, 270, 1).panel().h();
        int six = ConfirmLayout.of(480, 270, 6).panel().h();
        int huge = ConfirmLayout.of(480, 270, 200).panel().h();
        assertTrue(six > one, "taller for six lines");
        assertEquals(270 * 7 / 10, huge, "stops at 70% of the screen");
        assertTrue(ConfirmLayout.of(480, 270, 6).maxLines() >= 6, "room for the six lines");
        assertTrue(ConfirmLayout.textWidth(320) > 100);
    }
}
