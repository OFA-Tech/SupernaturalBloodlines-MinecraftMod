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

    private static java.awt.image.BufferedImage image(String sprite) throws Exception {
        try (java.io.InputStream in = ThemeContrast.class.getResourceAsStream(SPRITES + sprite + ".png")) {
            org.junit.jupiter.api.Assertions.assertNotNull(in, sprite + ".png is missing");
            return javax.imageio.ImageIO.read(in);
        }
    }

    /** The borders of a nine-slice picture (left, top, right, bottom), all 0 for one that is not. */
    private static int[] borders(String sprite) throws Exception {
        try (java.io.InputStream in = ThemeContrast.class.getResourceAsStream(SPRITES + sprite + ".png.mcmeta")) {
            if (in == null) {
                return new int[4];
            }
            var scaling = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("gui").getAsJsonObject("scaling");
            if (!scaling.has("border")) {
                return new int[4];
            }
            var b = scaling.getAsJsonObject("border");
            return new int[] {b.get("left").getAsInt(), b.get("top").getAsInt(), b.get("right").getAsInt(), b.get("bottom").getAsInt()};
        }
    }

    /**
     * The theme's text colours on EVERY pixel of the inside of the theme's own kit pictures (the panel the pages are drawn on, the inset of a well, and the
     * cover for a theme that draws its text straight on it), not on the paper colour the theme names: a repainted kit that no longer matches its theme, or a
     * speck or a dotted line in the middle of a panel, fails here. The focus ring must also stand out of the panel (3:1).
     */
    static void assertKitReadable(String name, HubTheme theme) {
        try {
            List<String> failed = new ArrayList<>();
            // the Grimoire's cover is leather with no text on it; the other two put their title, counters and stage text straight on theirs
            List<String> parts = theme.kit().equals("grimoire") ? List.of("panel", "inset") : List.of("panel", "inset", "cover");
            for (String part : parts) {
                String sprite = theme.kit() + "/" + part;
                var img = image(sprite);
                int[] b = borders(sprite);
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                for (int y = b[1]; y < img.getHeight() - b[3]; y++) {
                    for (int x = b[0]; x < img.getWidth() - b[2]; x++) {
                        int surface = img.getRGB(x, y);
                        if (!seen.add(surface)) {
                            continue;
                        }
                        for (HubTheme.TextPair p : List.of(new HubTheme.TextPair("ink", theme.ink(), surface), new HubTheme.TextPair("muted", theme.muted(), surface),
                                new HubTheme.TextPair("accent", theme.accent(), surface), new HubTheme.TextPair("good", theme.good(), surface))) {
                            double r = Contrast.ratio(p.fg(), p.bg());
                            if (r < 4.5) {
                                failed.add(String.format("%s on the kit %s pixel #%08x: #%06x on #%06x is %.2f:1", p.what(), part, surface, p.fg() & 0xFFFFFF, p.bg() & 0xFFFFFF, r));
                            }
                        }
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

    private static int pixel(String sprite, int x, int y) throws Exception {
        var img = image(sprite);
        return img.getRGB(x < 0 ? img.getWidth() / 2 : x, y < 0 ? img.getHeight() / 2 : y);
    }
}