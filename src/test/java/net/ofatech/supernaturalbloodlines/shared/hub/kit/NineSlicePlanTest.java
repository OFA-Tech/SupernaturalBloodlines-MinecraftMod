package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NineSlicePlanTest {
    private static final int FILL = 0xFF336699;
    /** The big nine-slices (panel, inset, button) whose middle is not one colour. */
    private static final java.util.Set<String> TEXTURED = new java.util.TreeSet<>(List.of("bloodmoon/button_normal", "bloodmoon/button_primary_normal"));

    /** How often each pixel of the target is covered by a piece, and that every copied piece lies inside the sprite. */
    private static int[][] cover(List<NineSlicePlan.Op> ops, int sw, int sh, int x, int y, int w, int h) {
        int[][] hits = new int[h][w];
        for (NineSlicePlan.Op op : ops) {
            assertTrue(op.x() >= x && op.y() >= y && op.x() + op.w() <= x + w && op.y() + op.h() <= y + h, "piece outside the target: " + op);
            if (!op.fill()) {
                assertTrue(op.u() >= 0 && op.v() >= 0 && op.u() + op.w() <= sw && op.v() + op.h() <= sh, "piece outside the sprite: " + op);
            }
            for (int py = op.y(); py < op.y() + op.h(); py++) {
                for (int px = op.x(); px < op.x() + op.w(); px++) {
                    hits[py - y][px - x]++;
                }
            }
        }
        return hits;
    }

    @Test
    void everyPixelOfTheTargetIsDrawnExactlyOnce() {
        int[][] sprites = {{32, 32, 6, 6, 6, 6}, {50, 50, 13, 13, 13, 13}, {28, 20, 6, 6, 6, 6}, {16, 8, 4, 0, 4, 0}, {32, 16, 6, 4, 5, 4}, {24, 13, 16, 0, 0, 0}};
        int[][] targets = {{0, 0, 480, 270}, {5, 7, 100, 40}, {3, 3, 12, 12}, {0, 0, 7, 5}, {10, 10, 33, 31}, {0, 0, 1, 1}};
        for (int[] s : sprites) {
            for (int[] t : targets) {
                for (Integer middle : new Integer[] {FILL, null}) {
                    List<NineSlicePlan.Op> ops = NineSlicePlan.of(s[0], s[1], s[2], s[3], s[4], s[5], t[0], t[1], t[2], t[3], middle);
                    int[][] hits = cover(ops, s[0], s[1], t[0], t[1], t[2], t[3]);
                    for (int py = 0; py < t[3]; py++) {
                        for (int px = 0; px < t[2]; px++) {
                            assertEquals(1, hits[py][px], "sprite " + s[0] + "x" + s[1] + " target " + t[2] + "x" + t[3] + " pixel " + px + "," + py + " drawn " + hits[py][px] + " times");
                        }
                    }
                }
            }
        }
    }

    @Test
    void aSolidMiddleIsOneFillAndNotAThousandCopies() {
        List<NineSlicePlan.Op> solid = NineSlicePlan.of(32, 32, 6, 6, 6, 6, 0, 0, 480, 270, FILL);
        assertEquals(1, solid.stream().filter(NineSlicePlan.Op::fill).count());
        assertTrue(solid.size() < 100, "a large panel takes " + solid.size() + " pieces");
        List<NineSlicePlan.Op> tiled = NineSlicePlan.of(32, 32, 6, 6, 6, 6, 0, 0, 480, 270, null);
        assertTrue(tiled.size() > 4 * solid.size(), "tiling the middle takes " + tiled.size() + " against " + solid.size());
        assertEquals(0, tiled.stream().filter(NineSlicePlan.Op::fill).count());
    }

    @Test
    void anEmptyMiddleDrawsNothingThere() {
        List<NineSlicePlan.Op> ops = NineSlicePlan.of(32, 32, 6, 6, 6, 6, 0, 0, 100, 100, 0);
        assertEquals(0, ops.stream().filter(NineSlicePlan.Op::fill).count());
        int[][] hits = cover(ops, 32, 32, 0, 0, 100, 100);
        assertEquals(0, hits[50][50]);
        assertEquals(1, hits[2][2]);
    }

    @Test
    void theFillIsTheMiddleColourAndSitsInsideTheBorders() {
        NineSlicePlan.Op fill = NineSlicePlan.of(32, 32, 6, 8, 5, 7, 10, 20, 100, 60, FILL).stream().filter(NineSlicePlan.Op::fill).findFirst().orElseThrow();
        assertEquals(FILL, fill.color());
        assertEquals(10 + 6, fill.x());
        assertEquals(20 + 8, fill.y());
        assertEquals(100 - 6 - 5, fill.w());
        assertEquals(60 - 8 - 7, fill.h());
    }

    @Test
    void aTargetSmallerThanItsBordersKeepsHalfForEachSide() {
        List<NineSlicePlan.Op> ops = NineSlicePlan.of(32, 32, 6, 6, 6, 6, 0, 0, 8, 8, FILL);
        int[][] hits = cover(ops, 32, 32, 0, 0, 8, 8);
        for (int[] row : hits) {
            for (int n : row) {
                assertEquals(1, n);
            }
        }
    }

    @Test
    void aMiddleIsSolidOnlyWhenEveryPixelOfItIsTheSame() {
        int[] px = new int[8 * 8];
        java.util.Arrays.fill(px, 0xFF102030);
        assertEquals(0xFF102030, NineSlicePlan.solidMiddle(px, 8, 8, 2, 2, 2, 2));
        px[3 * 8 + 4] = 0xFF102031;
        assertNull(NineSlicePlan.solidMiddle(px, 8, 8, 2, 2, 2, 2));
        // a speck in the border does not matter
        px[3 * 8 + 4] = 0xFF102030;
        px[0] = 0xFFFFFFFF;
        assertNotNull(NineSlicePlan.solidMiddle(px, 8, 8, 2, 2, 2, 2));
        assertEquals(0, NineSlicePlan.solidMiddle(px, 8, 8, 4, 4, 4, 4), "no middle at all");
    }

    @Test
    void theRealPanelsOfTheThreeKitsHaveASolidMiddle() throws Exception {
        List<String> notSolid = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (KitSprites.Part part : new KitSprites.Part[] {KitSprites.Part.PANEL, KitSprites.Part.INSET, KitSprites.Part.BUTTON_NORMAL, KitSprites.Part.BUTTON_PRIMARY_NORMAL}) {
                BufferedImage img = UiKitSlicesTest.png(KitSprites.of(kit, part.sprite()));
                int[] b = UiKitSlicesTest.border(KitSprites.of(kit, part.sprite()));
                int[] argb = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
                if (NineSlicePlan.solidMiddle(argb, img.getWidth(), img.getHeight(), b[0], b[1], b[2], b[3]) == null) {
                    notSolid.add(kit + "/" + part.sprite());
                }
            }
        }
        // a middle that is not solid is still drawn right (tiled), but it costs a draw for every tile: the ones that do are named here, so a repaint that
        // makes another panel textured shows up as a failure to look at, not as a slow screen
        assertEquals(TEXTURED, new java.util.TreeSet<>(notSolid));
    }
}