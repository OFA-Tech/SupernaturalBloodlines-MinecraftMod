package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every theme's text colours on its paper, at 4.5:1 or more (WCAG AA for text). The colours are the themes' own constants, copied here
 * because the themes are client classes; a theme change that breaks this must change both.
 */
class ThemeContrastTest {
    private record Palette(String name, int paper, int ink, int muted, int accent, int good, List<Integer> sources) {
    }

    private static final List<Palette> THEMES = List.of(
            new Palette("grimoire", 0xFFe9dcbc, 0xFF3b2414, 0xFF5e4228, 0xFF7a1218, 0xFF245a3a,
                    List.of(0xFF9a1820, 0xFF7a4508, 0xFF245a3a, 0xFF16566a, 0xFF5a2f9a)),
            new Palette("blood_moon", 0xFF1a0a10, 0xFFf0e0e0, 0xFFc9a0a8, 0xFFffd36b, 0xFF7fe0a0,
                    List.of(0xFFff8a8a, 0xFFf0a43a, 0xFF6fe08a, 0xFF6fd6ea, 0xFFc7a6ff)),
            new Palette("tensura", 0xFF041d2c, 0xFFeef5ff, 0xFF9fc0cc, 0xFFffd36b, 0xFF7ee08f,
                    List.of(0xFFff8a8a, 0xFFffbe5c, 0xFF7ee08f, 0xFF5fd3e6, 0xFFc9a8ff)));

    @Test
    void everyTextColourIsReadableOnItsPaper() {
        for (Palette p : THEMES) {
            List<Integer> text = new java.util.ArrayList<>(List.of(p.ink(), p.muted(), p.accent(), p.good()));
            text.addAll(p.sources());
            for (int c : text) {
                double r = Contrast.ratio(c, p.paper());
                assertTrue(r >= 4.5, p.name() + String.format(": #%06x on #%06x is %.2f:1", c & 0xFFFFFF, p.paper() & 0xFFFFFF, r));
            }
        }
    }

    @Test
    void theRatioIsWcags() {
        assertTrue(Math.abs(Contrast.ratio(0xFF000000, 0xFFFFFFFF) - 21) < 0.01);
        assertTrue(Math.abs(Contrast.ratio(0xFF777777, 0xFF777777) - 1) < 0.01);
    }
}
