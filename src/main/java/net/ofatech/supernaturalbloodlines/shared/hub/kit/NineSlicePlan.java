package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A nine-slice sprite drawn as few draw calls as it can be. Vanilla repeats the middle of a nine-slice once for every tile of the sprite's own middle
 * (a 480 x 270 panel of 8 px borders is over 1,000 blits). Here the corners and the edges are tiled as vanilla does, and a middle that is one solid
 * colour in the picture is one fill. Pure: the plan is a list of fills and 1:1 copies of rectangles of the sprite, which the client draws.
 */
public final class NineSlicePlan {
    /**
     * One piece: a {@code fill} of {@code color}, or a copy of ({@code u}, {@code v}) of the sprite at ({@code x}, {@code y}); {@code w} and {@code h} are the
     * size of both the target and the part copied.
     */
    public record Op(boolean fill, int x, int y, int w, int h, int u, int v, int color) {
    }

    private NineSlicePlan() {
    }

    /**
     * Whether the picture is the size its .mcmeta nine-slice says it is. A resource pack that repaints a sprite at twice the size keeps the old .mcmeta: the borders
     * would then be cut from the wrong place, so such a sprite is left to vanilla's own drawing, which scales the whole picture.
     */
    public static boolean sizesMatch(int pictureW, int pictureH, int sliceW, int sliceH) {
        return pictureW == sliceW && pictureH == sliceH;
    }

    /**
     * The pieces of a sprite of {@code sw x sh} with borders {@code l, t, r, b} drawn over ({@code x}, {@code y}, {@code w}, {@code h}).
     *
     * @param middle the one colour (ARGB) the middle of the sprite is, 0 when it is empty, or null when it is not one colour (it is then tiled like vanilla)
     */
    public static List<Op> of(int sw, int sh, int l, int t, int r, int b, int x, int y, int w, int h, @Nullable Integer middle) {
        List<Op> ops = new ArrayList<>();
        if (w <= 0 || h <= 0) {
            return ops;
        }
        // a target smaller than its borders keeps half of the width and height for each side, as vanilla does
        int dl = Math.min(l, w / 2);
        int dr = Math.min(r, w / 2);
        int dt = Math.min(t, h / 2);
        int db = Math.min(b, h / 2);
        int cw = sw - l - r;
        int ch = sh - t - b;
        ops.add(new Op(false, x, y, dl, dt, 0, 0, 0));
        ops.add(new Op(false, x + w - dr, y, dr, dt, sw - dr, 0, 0));
        ops.add(new Op(false, x, y + h - db, dl, db, 0, sh - db, 0));
        ops.add(new Op(false, x + w - dr, y + h - db, dr, db, sw - dr, sh - db, 0));
        int innerW = w - dl - dr;
        int innerH = h - dt - db;
        if (innerW > 0 && cw > 0) {
            tileRow(ops, x + dl, y, innerW, dt, l, 0, cw);
            tileRow(ops, x + dl, y + h - db, innerW, db, l, sh - db, cw);
        }
        if (innerH > 0 && ch > 0) {
            tileColumn(ops, x, y + dt, dl, innerH, 0, t, ch);
            tileColumn(ops, x + w - dr, y + dt, dr, innerH, sw - dr, t, ch);
        }
        if (innerW > 0 && innerH > 0) {
            if (middle == null) {
                if (cw > 0 && ch > 0) {
                    for (int py = 0; py < innerH; py += ch) {
                        for (int px = 0; px < innerW; px += cw) {
                            ops.add(new Op(false, x + dl + px, y + dt + py, Math.min(cw, innerW - px), Math.min(ch, innerH - py), l, t, 0));
                        }
                    }
                }
            } else if ((middle >>> 24) != 0) {
                ops.add(new Op(true, x + dl, y + dt, innerW, innerH, 0, 0, middle));
            }
        }
        ops.removeIf(o -> o.w() <= 0 || o.h() <= 0);
        return ops;
    }

    private static void tileRow(List<Op> ops, int x, int y, int len, int h, int u, int v, int cw) {
        if (h <= 0) {
            return;
        }
        for (int px = 0; px < len; px += cw) {
            ops.add(new Op(false, x + px, y, Math.min(cw, len - px), h, u, v, 0));
        }
    }

    private static void tileColumn(List<Op> ops, int x, int y, int w, int len, int u, int v, int ch) {
        if (w <= 0) {
            return;
        }
        for (int py = 0; py < len; py += ch) {
            ops.add(new Op(false, x, y + py, w, Math.min(ch, len - py), u, v, 0));
        }
    }

    /**
     * The one colour the middle of a sprite is: the pixel at ({@code l}, {@code t}) when every pixel from there to the far borders is the same (alpha
     * included), else null. {@code argb} is {@code sw x sh} pixels, row by row.
     */
    @Nullable
    public static Integer solidMiddle(int[] argb, int sw, int sh, int l, int t, int r, int b) {
        if (sw - l - r <= 0 || sh - t - b <= 0) {
            return 0;
        }
        int first = argb[t * sw + l];
        for (int py = t; py < sh - b; py++) {
            for (int px = l; px < sw - r; px++) {
                if (argb[py * sw + px] != first) {
                    return null;
                }
            }
        }
        return first;
    }
}
