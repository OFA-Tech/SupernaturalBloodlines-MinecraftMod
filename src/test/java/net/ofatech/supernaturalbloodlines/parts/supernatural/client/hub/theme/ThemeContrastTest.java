package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.ThemeContrast;
import org.junit.jupiter.api.Test;

/** The supernatural part's themes, checked on the objects themselves (Tensura's theme has its own test in its part). */
class ThemeContrastTest {
    @Test
    void grimoireTextIsReadable() {
        ThemeContrast.assertReadable("grimoire", new GrimoireTheme());
    }

    @Test
    void bloodMoonTextIsReadable() {
        ThemeContrast.assertReadable("blood moon", new BloodMoonTheme());
    }

    @Test
    void thePackPerkLineIsReadableOnEveryTheme() {
        for (var theme : java.util.List.<net.ofatech.supernaturalbloodlines.shared.hub.HubTheme>of(new GrimoireTheme(), new BloodMoonTheme())) {
            double ratio = net.ofatech.supernaturalbloodlines.shared.hub.Contrast.ratio(
                    net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage.perkColor(theme), theme.paper());
            org.junit.jupiter.api.Assertions.assertTrue(ratio >= 4.5, theme.getClass().getSimpleName() + " perk line is " + ratio + ":1 on its paper");
        }
    }
}
