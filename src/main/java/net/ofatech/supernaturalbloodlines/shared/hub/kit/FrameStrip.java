package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * The strips of the kit that hold one picture per step, one under the other: the cooldown pie ({@code kit/shared/cooldown_sweep}, 33 frames of 16 x 16, the
 * first empty) and the ring of blood round Bite ({@code blood_ring}, 25 frames of 32 x 32). Which frame, and which rectangle of the strip it is. Pure.
 */
public final class FrameStrip {
    public static final String SWEEP = KitSprites.SHARED + "cooldown_sweep";
    public static final String RING = KitSprites.SHARED + "blood_ring";
    public static final int SWEEP_SIZE = 16;
    public static final int RING_SIZE = 32;
    /** The sweep's last frame is a whole pie; its first, 0, is empty. */
    public static final int SWEEP_STEPS = 32;
    public static final int RING_STEPS = 24;

    private FrameStrip() {
    }

    /** The pie for {@code left} (0 to 1) of the cooldown still to run: a sliver for any time left at all, never frame 0 while it runs. */
    public static int sweepFrame(float left) {
        if (left <= 0f) {
            return 0;
        }
        return Math.max(1, Math.min(SWEEP_STEPS, Math.round(left * SWEEP_STEPS)));
    }

    /** The ring for a share (0 to 1) of blood: frame 0 is the dim ring only, 24 the whole ring lit. */
    public static int ringFrame(float share) {
        return Math.max(0, Math.min(RING_STEPS, Math.round(share * RING_STEPS)));
    }

    /** The top of a frame in the strip, in pixels. */
    public static int top(int frame, int frameSize) {
        return frame * frameSize;
    }

    /** Whether a picture of {@code stripHeight} holds {@code frames} frames of {@code frameSize}: a hi-res pack's strip does not, and the HUD then draws nothing of it. */
    public static boolean holds(int stripWidth, int stripHeight, int frameSize, int frames) {
        return stripWidth == frameSize && stripHeight == frameSize * frames;
    }
}