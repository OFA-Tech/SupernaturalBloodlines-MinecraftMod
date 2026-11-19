package net.ofatech.supernaturalbloodlines.parts.artifacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** One test per row of the table in art/items/RING_RENDER.md, so a typo in an offset or a file name fails by name. */
class RingPoseTest {
    @Test
    void classicRight() {
        assertEquals(-1.0f, RingPose.offsetX(false, false));
        assertEquals("curio/sun_ring", RingPose.modelFile("sun_ring", false, false));
    }

    @Test
    void classicLeft() {
        assertEquals(1.0f, RingPose.offsetX(false, true));
        assertEquals("curio/sun_ring_left", RingPose.modelFile("sun_ring", false, true));
    }

    @Test
    void slimRight() {
        assertEquals(-0.5f, RingPose.offsetX(true, false));
        assertEquals("curio/moon_ring_slim", RingPose.modelFile("moon_ring", true, false));
    }

    @Test
    void slimLeft() {
        assertEquals(0.5f, RingPose.offsetX(true, true));
        assertEquals("curio/resurrection_ring_slim_left", RingPose.modelFile("resurrection_ring", true, true));
    }

    @Test
    void theBandIsTheLowestPixelOfTheArm() {
        assertEquals(9.5f, RingPose.Y);
    }

    @Test
    void firstSlotIsTheRightArmSecondIsTheLeftAndASingleRingGoesRight() {
        assertFalse(RingPose.left(0));
        assertTrue(RingPose.left(1));
    }

    @Test
    void everyVariantOfEveryRingShips() {
        for (String ring : RingPose.RINGS) {
            for (int v = 0; v < 4; v++) {
                String path = "/assets/supernaturalbloodlines/models/" + RingPose.modelFile(ring, (v & 1) != 0, (v & 2) != 0) + ".json";
                assertNotNull(RingPoseTest.class.getResource(path), "missing worn model " + path);
            }
        }
    }
}
