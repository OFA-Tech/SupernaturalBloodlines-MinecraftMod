package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.function.IntBinaryOperator;

/**
 * The pixels of the pictures that are too big to draw with a fill per row every frame: the Blood Moon with its halo, the glow behind it, and the Ghoul's grey moon.
 * Each is an {@code (x, y) -> ARGB} painter over a square; the client bakes it once into a texture ({@code BakedTexture}) and draws it with one blit,
 * so a new texture is made only when the radius or the progress step changes. Pure, so the painting is tested without a screen.
 */
public final class Bake {
    /** How far the halo reaches beyond the Blood Moon's disc. */
    public static final int HALO = 22;
    /** The moon's progress is baked in this many steps. */
    public static final int STEPS = 48;
    private static final int[][] CRATERS_PERCENT = {{-45, -40, 16}, {30, -15, 11}, {-60, 25, 10}, {20, 50, 14}, {55, 30, 8}};

    private Bake() {
    }

    /** The side of the Blood Moon's square for a disc of radius {@code r}. */
    public static int moonSize(int r) {
        return 2 * (r + HALO + 2);
    }

    /** The progress rounded to a step, so a moon that changes by a hair does not make a new texture. */
    public static int step(float progress) {
        return Math.round(Math.max(0f, Math.min(1f, progress)) * STEPS);
    }

    /** The Blood Moon: a soft red halo, a disc lit from the bottom up to the progress (pale in the middle, red at the rim), and five craters. */
    public static IntBinaryOperator moon(int r, int step) {
        int size = moonSize(r);
        float fill = Math.max(0.1f, step / (float) STEPS);
        int line = r - Math.round(2 * r * fill);
        return (x, y) -> {
            double dx = x + 0.5 - size / 2.0;
            double dy = y + 0.5 - size / 2.0;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > r) {
                return halo(dist - r);
            }
            int color;
            if (dy < line) {
                color = mix(0xFF4a1520, 0xFF2a0b12, (float) ((dy + r) / (2.0 * r)));
            } else {
                double d = dist / r;
                color = mix(0xFFffd2c4, 0xFF8a1220, (float) Math.min(1.0, d * d));
            }
            for (int[] c : CRATERS_PERCENT) {
                double cdx = dx - r * c[0] / 100.0;
                double cdy = dy - r * c[1] / 100.0;
                double cr = Math.max(1, r * c[2] / 100.0);
                if (cdx * cdx + cdy * cdy <= cr * cr) {
                    color = mix(color, 0xFF000000, 34 / 255f);
                }
            }
            return color;
        };
    }

    /** The halo past the rim: one faint red ring for every two pixels it reaches, added up (up to eleven). */
    static int halo(double beyondRim) {
        int minH = Math.max(2, (int) Math.ceil(beyondRim / 2.0) * 2);
        if (minH > HALO) {
            return 0;
        }
        int rings = (HALO - minH) / 2 + 1;
        return withAlpha(0xe2493f, 1f - (float) Math.pow(1.0 - 10 / 255.0, rings));
    }

    /** The glow behind the stage: sixteen faint discs of one red, added up toward the middle. */
    public static IntBinaryOperator glow(int radius) {
        return (x, y) -> {
            double dx = x + 0.5 - radius;
            double dy = y + 0.5 - radius;
            double dist = Math.sqrt(dx * dx + dy * dy);
            int rings = 0;
            for (int i = 16; i >= 1; i--) {
                if (radius * i / 16 >= dist) {
                    rings++;
                }
            }
            return rings == 0 ? 0 : withAlpha(0xA0202A, 1f - (float) Math.pow(1.0 - 7 / 255.0, rings));
        };
    }

    /** The grey of a drained moon at {@code d} (0 in the middle, 1 at the rim). */
    public static int greyValue(float d) {
        return 0x8C - (int) (0x2A * Math.min(1f, d * d));
    }

    public static IntBinaryOperator greyMoon(int r) {
        return (x, y) -> {
            double dx = x + 0.5 - r;
            double dy = y + 0.5 - r;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > r) {
                return 0;
            }
            int v = greyValue((float) (dist / r));
            return 0xFF000000 | v << 16 | v << 8 | v;
        };
    }

    public static int withAlpha(int rgb, float alpha) {
        return Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24 | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
