package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.List;

/** Where a worn ring sits on the arm and which baked model draws it (art/items/RING_RENDER.md). No game classes, so a test can read every row. */
public final class RingPose {
    /** The three rings that have worn models; the Moon Ring's exists only when Werewolves is loaded, its models ship anyway. */
    public static final List<String> RINGS = List.of("sun_ring", "moon_ring", "resurrection_ring");
    /** Arm-local y of the ring centre in 1/16 blocks: the band is the lowest pixel of the arm (the arm cube ends at y = 10). */
    public static final float Y = 9.5f;

    private RingPose() {
    }

    /** Arm-local x of the ring centre in 1/16 blocks: the middle of the arm cube, which is 4 px wide (classic) or 3 px (slim). */
    public static float offsetX(boolean slim, boolean left) {
        float mid = slim ? 0.5f : 1.0f;
        return left ? mid : -mid;
    }

    /** Model path under {@code models/}, without {@code .json}. */
    public static String modelFile(String ring, boolean slim, boolean left) {
        return "curio/" + ring + (slim ? "_slim" : "") + (left ? "_left" : "");
    }

    /** Curios slot index 0 is the right arm, 1 the left; a single ring goes on the right. */
    public static boolean left(int slotIndex) {
        return slotIndex == 1;
    }
}
