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
}
