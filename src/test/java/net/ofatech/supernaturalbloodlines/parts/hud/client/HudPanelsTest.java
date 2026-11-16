package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import org.junit.jupiter.api.Test;

/** The mode panel stays in the right half of the screen at every GUI size, so the left column's tooltips (the left half) never run into it. */
class HudPanelsTest {
    @Test
    void thePreviewPanelStaysInTheRightHalf() {
        for (int[] s : new int[][] {{480, 270}, {640, 360}, {960, 540}, {1280, 720}}) {
            HudLayout.Box[] boxes = HudLayout.slots(s[0], s[1]);
            int x = Math.max(4, Math.min(boxes[3].right() - HudChrome.PREVIEW_W, s[0] - HudChrome.PREVIEW_W - 4));
            assertTrue(x >= s[0] / 2, s[0] + "x" + s[1] + ": the panel starts at " + x + ", left of the middle " + s[0] / 2);
            assertTrue(x + HudChrome.PREVIEW_W <= s[0], "the panel leaves the screen");
        }
    }
}
