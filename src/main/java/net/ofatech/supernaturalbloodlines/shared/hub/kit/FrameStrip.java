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

    /**
     * The ring of blood when a resource pack has changed the strip's size: the same share as the lit part of a one pixel outline of a {@code w x h} box, running
     * clockwise from the top middle. The pieces are {@code {x, y, w, h}} rectangles, each a straight run.
     */
    public static java.util.List<int[]> outlineSegments(int w, int h, float share) {
        int total = 2 * w + 2 * h - 4;
        int lit = Math.max(0, Math.min(total, Math.round(share * total)));
        java.util.List<int[]> out = new java.util.ArrayList<>();
        int[][] path = new int[total][];
        int n = 0;
        for (int x = w / 2; x < w; x++) {
            path[n++] = new int[] {x, 0};
        }
        for (int y = 1; y < h; y++) {
            path[n++] = new int[] {w - 1, y};
        }
        for (int x = w - 2; x >= 0; x--) {
            path[n++] = new int[] {x, h - 1};
        }
        for (int y = h - 2; y >= 0; y--) {
            path[n++] = new int[] {0, y};
        }
        for (int x = 1; x < w / 2; x++) {
            path[n++] = new int[] {x, 0};
        }
        int[] run = null;
        for (int i = 0; i < lit; i++) {
            int[] p = path[i];
            if (run != null && p[0] == run[0] + run[2] && p[1] == run[1] && run[3] == 1) {
                run[2]++;
            } else if (run != null && p[0] == run[0] - 1 && p[1] == run[1] && run[3] == 1) {
                run[0]--;
                run[2]++;
            } else if (run != null && p[0] == run[0] && run[2] == 1 && (p[1] == run[1] + run[3] || p[1] == run[1] - 1)) {
                if (p[1] < run[1]) {
                    run[1]--;
                }
                run[3]++;
            } else {
                run = new int[] {p[0], p[1], 1, 1};
                out.add(run);
            }
        }
        return out;
    }
}