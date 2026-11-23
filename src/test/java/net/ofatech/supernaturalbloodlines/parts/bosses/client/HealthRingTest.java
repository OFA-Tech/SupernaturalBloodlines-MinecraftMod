package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.HealthRing.Rect;
import org.junit.jupiter.api.Test;

class HealthRingTest {
    private static final int S = BossRow.TOKEN;
    private static final int T = 2;

    private static int area(List<Rect> rects) {
        return rects.stream().mapToInt(Rect::area).sum();
    }

    private static boolean[][] paint(List<Rect> rects) {
        boolean[][] px = new boolean[S][S];
        for (Rect r : rects) {
            assertTrue(r.x() >= 0 && r.y() >= 0 && r.x() + r.w() <= S && r.y() + r.h() <= S, "outside the square: " + r);
            for (int y = r.y(); y < r.y() + r.h(); y++) {
                for (int x = r.x(); x < r.x() + r.w(); x++) {
                    assertFalse(px[y][x], "painted twice at " + x + "," + y);
                    px[y][x] = true;
                }
            }
        }
        return px;
    }

    @Test
    void noHealthIsNoRingAndAnyHealthIsASliverOfOnePixelOfBand() {
        assertTrue(HealthRing.filled(S, T, 0f).isEmpty());
        assertTrue(HealthRing.filled(S, T, -1f).isEmpty());
        assertTrue(HealthRing.filled(S, T, Float.NaN).isEmpty());
        assertEquals(T, area(HealthRing.filled(S, T, 0.0001f)), "one pixel along a band 2 thick");
    }

    @Test
    void fullHealthFillsTheWholeBandAndExactlyTheTrack() {
        boolean[][] full = paint(HealthRing.filled(S, T, 1f));
        boolean[][] track = paint(HealthRing.track(S, T));
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                assertEquals(track[y][x], full[y][x], "differs at " + x + "," + y);
            }
        }
        assertEquals(HealthRing.length(S, T) * T, area(HealthRing.filled(S, T, 7f)));
    }

    @Test
    void theFilledPartIsProportionalAndNeverShrinksAsHealthGrows() {
        int last = 0;
        for (int pct = 1; pct <= 100; pct++) {
            List<Rect> rects = HealthRing.filled(S, T, pct / 100f);
            paint(rects);
            int a = area(rects);
            assertTrue(a >= last, pct + " %: " + a + " after " + last);
            assertEquals(Math.round(HealthRing.length(S, T) * pct / 100f) * T, a, pct + " %");
            last = a;
        }
    }

    @Test
    void itFillsClockwiseFromTheTopCentre() {
        boolean[][] quarter = paint(HealthRing.filled(S, T, 0.25f));
        assertTrue(quarter[0][S / 2 + 3], "the top right is first");
        assertTrue(quarter[T + 2][S - 1], "then down the right side");
        assertFalse(quarter[S - 1][S / 2], "the bottom is not yet");
        assertFalse(quarter[0][3], "and the top left is last");
        boolean[][] threeQuarters = paint(HealthRing.filled(S, T, 0.75f));
        assertTrue(threeQuarters[S - 1][S / 2], "the bottom is full by three quarters");
        assertTrue(threeQuarters[S - 6][0], "the left side is filling upwards");
        assertFalse(threeQuarters[0][3], "the top left is still empty");
    }

    @Test
    void theTrackIsFourBandsThatDoNotOverlap() {
        paint(HealthRing.track(S, T));
        assertEquals(HealthRing.length(S, T) * T, area(HealthRing.track(S, T)));
    }
}
