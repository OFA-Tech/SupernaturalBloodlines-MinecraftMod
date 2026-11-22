package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

/** The readability check every theme's test runs on the theme object itself: each text colour on what it is drawn on, 4.5:1 or more. */
public final class ThemeContrast {
    static final List<String> SOURCES = List.of("species", "vampirism", "werewolves", "enchanted", "tensura");

    private ThemeContrast() {
    }

    public static void assertReadable(String name, HubTheme theme) {
        List<HubTheme.TextPair> pairs = new ArrayList<>(theme.textPairs());
        int paper = theme.paper();
        pairs.add(new HubTheme.TextPair("ink", theme.ink(), paper));
        pairs.add(new HubTheme.TextPair("muted", theme.muted(), paper));
        pairs.add(new HubTheme.TextPair("accent", theme.accent(), paper));
        pairs.add(new HubTheme.TextPair("good", theme.good(), paper));
        SOURCES.forEach(s -> pairs.add(new HubTheme.TextPair("source tag " + s, theme.sourceColor(s), paper)));
        List<String> failed = new ArrayList<>();
        for (HubTheme.TextPair p : pairs) {
            double r = Contrast.ratio(p.fg(), p.bg());
            if (r < 4.5) {
                failed.add(String.format("%s: #%06x on #%06x is %.2f:1", p.what(), p.fg() & 0xFFFFFF, p.bg() & 0xFFFFFF, r));
            }
        }
        assertTrue(failed.isEmpty(), name + " has text below 4.5:1: " + failed);
        assertTrue(Contrast.ratio(theme.focusColor(), paper) >= 3.0, name + " focus ring is below 3:1 on its paper");
        // a button's icon is accent on paper at rest and paper on accent when hovered (the same ratio): 3:1 or more for a graphic
        assertTrue(Contrast.ratio(theme.accent(), paper) >= 3.0, name + " button icon (rest and hovered) is below 3:1");
        assertKitReadable(name, theme);
    }

    private static final String SPRITES = "/assets/supernaturalbloodlines/textures/gui/sprites/kit/";

    private static int pixel(String sprite, int x, int y) throws Exception {
        try (java.io.InputStream in = ThemeContrast.class.getResourceAsStream(SPRITES + sprite + ".png")) {
            org.junit.jupiter.api.Assertions.assertNotNull(in, sprite + ".png is missing");
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            return img.getRGB(x < 0 ? img.getWidth() / 2 : x, y < 0 ? img.getHeight() / 2 : y);
        }
    }

    /**
     * The theme's text colours on the middle of the theme's own kit pictures (the panel the pages are drawn on, the inset of a well), not on the paper colour
     * the theme names: a repainted kit that no longer matches its theme fails here. The focus ring must also stand out of the panel (3:1).
     */
    static void assertKitReadable(String name, HubTheme theme) {
        try {
            List<String> failed = new ArrayList<>();
            for (String part : List.of("panel", "inset")) {
                int surface = pixel(theme.kit() + "/" + part, -1, -1);
                for (HubTheme.TextPair p : List.of(new HubTheme.TextPair("ink", theme.ink(), surface), new HubTheme.TextPair("muted", theme.muted(), surface),
                        new HubTheme.TextPair("accent", theme.accent(), surface), new HubTheme.TextPair("good", theme.good(), surface))) {
                    double r = Contrast.ratio(p.fg(), p.bg());
                    if (r < 4.5) {
                        failed.add(String.format("%s on the kit %s: #%06x on #%06x is %.2f:1", p.what(), part, p.fg() & 0xFFFFFF, p.bg() & 0xFFFFFF, r));
                    }
                }
            }
            assertTrue(failed.isEmpty(), name + " text below 4.5:1 on its kit: " + failed);
            int panel = pixel(theme.kit() + "/panel", -1, -1);
            // the focus ring is a dark line and a light line side by side: one of them must stand out of the panel
            double ring = Math.max(Contrast.ratio(pixel(theme.kit() + "/focus", 0, 6), panel), Contrast.ratio(pixel(theme.kit() + "/focus", 1, 6), panel));
            assertTrue(ring >= 3.0, name + " kit focus ring is " + ring + ":1 on its panel");
        } catch (Exception e) {
            throw new AssertionError(name + ": " + e, e);
        }
    }
}
