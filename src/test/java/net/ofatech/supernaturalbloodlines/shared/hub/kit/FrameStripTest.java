package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.ofatech.supernaturalbloodlines.core.hud.PieSweep;
import org.junit.jupiter.api.Test;

class FrameStripTest {
    @Test
    void theSweepShowsASliverForAnyTimeLeftAndNeverTheEmptyFrameWhileItRuns() {
        assertEquals(0, FrameStrip.sweepFrame(0f));
        assertEquals(0, FrameStrip.sweepFrame(-1f));
        assertEquals(1, FrameStrip.sweepFrame(0.001f), "a sliver, not nothing");
        assertEquals(16, FrameStrip.sweepFrame(0.5f));
        assertEquals(32, FrameStrip.sweepFrame(1f));
        assertEquals(32, FrameStrip.sweepFrame(7f), "never past the whole pie");
    }

    @Test
    void theRingGrowsFromTheDimOneToTheWholeAndStopsThere() {
        assertEquals(0, FrameStrip.ringFrame(0f));
        assertEquals(12, FrameStrip.ringFrame(0.5f));
        assertEquals(24, FrameStrip.ringFrame(1f));
        assertEquals(24, FrameStrip.ringFrame(3f));
        assertEquals(0, FrameStrip.ringFrame(-0.4f));
    }

    @Test
    void aFrameIsFoundByItsNumberTimesItsSize() {
        assertEquals(0, FrameStrip.top(0, 16));
        assertEquals(16 * 32, FrameStrip.top(32, 16));
        assertEquals(32 * 7, FrameStrip.top(7, 32));
    }

    @Test
    void aStripOfAnotherSizeIsNotTakenForTheKitsOwn() {
        assertTrue(FrameStrip.holds(16, 16 * 33, 16, 33));
        assertFalse(FrameStrip.holds(32, 32 * 33, 16, 33), "a pack that repainted it at twice the size");
        assertFalse(FrameStrip.holds(16, 16 * 32, 16, 33), "a frame short");
    }

    @Test
    void theRealStripsHoldTheFramesTheCodeAsksFor() throws Exception {
        BufferedImage sweep = png("cooldown_sweep");
        BufferedImage ring = png("blood_ring");
        assertTrue(FrameStrip.holds(sweep.getWidth(), sweep.getHeight(), FrameStrip.SWEEP_SIZE, FrameStrip.SWEEP_STEPS + 1));
        assertTrue(FrameStrip.holds(ring.getWidth(), ring.getHeight(), FrameStrip.RING_SIZE, FrameStrip.RING_STEPS + 1));
    }

    @Test
    void theSweepCoversTheSameShareOfTheDiscAsThePieRuleSays() throws Exception {
        BufferedImage sweep = png("cooldown_sweep");
        for (int step : new int[] {4, 8, 16, 24, 31}) {
            int top = FrameStrip.top(step, FrameStrip.SWEEP_SIZE);
            int agree = 0;
            int total = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double dx = x + 0.5 - 8;
                    double dy = y + 0.5 - 8;
                    if (dx * dx + dy * dy > 9 * 9) {
                        continue; // inside the disc, clear of its edge
                    }
                    total++;
                    boolean drawn = (sweep.getRGB(x, top + y) >>> 24) > 0;
                    if (drawn == PieSweep.covered(dx, dy, step / 32f)) {
                        agree++;
                    }
                }
            }
            assertTrue(agree >= total * 0.95, "step " + step + ": the picture and PieSweep agree on " + agree + " of " + total + " pixels");
        }
    }

    private static BufferedImage png(String name) throws Exception {
        try (InputStream in = FrameStripTest.class.getResourceAsStream("/assets/supernaturalbloodlines/textures/gui/sprites/kit/shared/" + name + ".png")) {
            return ImageIO.read(in);
        }
    }
}