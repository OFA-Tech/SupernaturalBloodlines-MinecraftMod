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
    }
}
