package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import org.junit.jupiter.api.Test;

class LineColorsTest {
    @Test
    void atLeastKeepsAColourThatAlreadyReadsAndFixesOneThatDoesNot() {
        assertEquals(0xFF000000, LineColors.atLeast(0xFF000000, 0xFFFFFFFF, 3.0));
        int fixed = LineColors.atLeast(0xFFE9DCBC, 0xFFE9DCBC, 3.0);
        assertTrue(Contrast.ratio(fixed, 0xFFE9DCBC) >= 3.0);
    }

    @Test
    void theCrownReadsOnEveryThemesPaper() {
        for (HubTheme t : new HubTheme[] {new GrimoireTheme(), new BloodMoonTheme(), new TensuraHubTheme()}) {
            assertTrue(Contrast.ratio(LineColors.gold(t), t.paper()) >= 3.0, t.getClass().getSimpleName() + " crown is " + Contrast.ratio(LineColors.gold(t), t.paper()));
        }
    }

    @Test
    void theGrimoiresRawGoldDoesNotReadOnParchmentSoItIsDarkened() {
        HubTheme g = new GrimoireTheme();
        assertTrue(Contrast.ratio(LineColors.GOLD, g.paper()) < 3.0, "if the raw gold reads, this check is moot");
        assertTrue(LineColors.gold(g) != LineColors.GOLD);
    }

    @Test
    void theMoonsThreadsAndRingsReadOnBothEndsOfTheSky() {
        for (int colour : new int[] {LineColors.MOON_THREAD, LineColors.MOON_RING, LineColors.MOON_RED}) {
            int shown = LineColors.atLeast(colour, LineColors.MOON_SKY_TOP, LineColors.MIN_RATIO);
            assertTrue(Contrast.ratio(shown, LineColors.MOON_SKY_TOP) >= 3.0, Integer.toHexString(colour) + " on the top of the sky");
            assertTrue(Contrast.ratio(shown, LineColors.MOON_SKY_BOTTOM) >= 3.0, Integer.toHexString(colour) + " on the bottom of the sky");
        }
    }
}
