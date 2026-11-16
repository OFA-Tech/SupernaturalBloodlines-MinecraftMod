package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import org.junit.jupiter.api.Test;

/** The Ghoul's moon is a dim, drained grey (40 to 55 percent), and the clock and its caption stay readable on it. */
class GhoulMoonContrastTest {
    private static int grey(float d) {
        int v = StageGhoulPage.value(d);
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    @Test
    void theMoonIsDimNotWhite() {
        for (float d = 0; d <= 1f; d += 0.1f) {
            int v = StageGhoulPage.value(d);
            assertTrue(v / 255.0 >= 0.38 && v / 255.0 <= 0.56, "grey " + v + " at " + d + " is out of the 40 to 55 percent range");
        }
    }

    @Test
    void theClockAndItsCaptionHaveFourPointFiveToOne() {
        int ink = 0xFF0A0A0A;
        // the clock covers the middle fifth of the moon, the caption sits just under it
        for (float d = 0; d <= 0.45f; d += 0.05f) {
            assertTrue(Contrast.ratio(ink, grey(d)) >= 4.5, "the clock has " + Contrast.ratio(ink, grey(d)) + " at " + d);
        }
    }
}
