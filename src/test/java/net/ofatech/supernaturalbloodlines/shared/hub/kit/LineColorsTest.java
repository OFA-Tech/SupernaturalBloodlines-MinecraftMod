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
    void overIsTheColourSeenAtItsOpacity() {
        assertEquals(0xFF7F7F7F, LineColors.over(0xFFFFFFFF, 0.5f, 0xFF000000));
        assertEquals(0xFF123456, LineColors.over(0xFF123456, 1f, 0xFF000000), "opaque is the colour");
        assertEquals(0xFF654321, LineColors.over(0xFF123456, 0f, 0xFF654321), "invisible is the background");
        assertEquals(0xFF123456, LineColors.over(0xFF123456, 7f, 0xFF654321), "an opacity past 1 is 1");
        assertEquals(0xFF654321, LineColors.over(0xFF123456, -1f, 0xFF654321), "an opacity below 0 is 0");
    }

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
