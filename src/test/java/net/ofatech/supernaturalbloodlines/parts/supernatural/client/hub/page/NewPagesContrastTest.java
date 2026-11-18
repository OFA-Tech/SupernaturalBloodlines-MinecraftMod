package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestToast;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import org.junit.jupiter.api.Test;

/** Every colour pair the new pages add: the warning red on each paper, the button text on the accent and the lit colour, the badge and the toast. */
class NewPagesContrastTest {
    private static final double TEXT = 4.5;

    private static List<HubTheme> themes() {
        return List.of(new GrimoireTheme(), new BloodMoonTheme(), new TensuraHubTheme());
    }

    @Test
    void theWarningRedReadsOnEveryPaper() {
        for (HubTheme t : themes()) {
            double ratio = Contrast.ratio(PageWidgets.warn(t), 0xFF000000 | t.paper());
            assertTrue(ratio >= TEXT, t.getClass().getSimpleName() + ": the warning red on its paper is " + ratio);
        }
    }

    @Test
    void buttonTextReadsOnTheAccentAndOnTheLitColour() {
        for (HubTheme t : themes()) {
            for (int fill : new int[] {t.accent(), t.good(), t.muted(), t.paper()}) {
                double ratio = Contrast.ratio(Contrast.bestText(fill), 0xFF000000 | fill);
                assertTrue(ratio >= TEXT, t.getClass().getSimpleName() + ": text on " + Integer.toHexString(fill) + " is " + ratio);
            }
            // a plain button: the ink on the paper, a disabled one: the muted text on the paper
            assertTrue(Contrast.ratio(t.ink(), 0xFF000000 | t.paper()) >= TEXT, t.getClass().getSimpleName() + " ink");
            assertTrue(Contrast.ratio(t.muted(), 0xFF000000 | t.paper()) >= TEXT, t.getClass().getSimpleName() + " muted");
        }
    }

    @Test
    void theBadgeAndTheToastAreReadable() {
        assertTrue(Contrast.ratio(HubScreen.BADGE_TEXT, HubScreen.BADGE) >= TEXT, "the number on the red badge");
        int ground = 0xFF181018; // the toast's ground (it is drawn at 88% over the game, never lighter than this)
        assertTrue(Contrast.ratio(RequestToast.INK, ground) >= TEXT, "the toast's first line");
        assertTrue(Contrast.ratio(RequestToast.MUTED, ground) >= TEXT, "the toast's second and third lines");
    }

    @Test
    void theFreedLinePillAndTheNavTabsAreReadable() {
        assertTrue(Contrast.ratio(net.ofatech.supernaturalbloodlines.shared.hub.kit.LineColors.FREE_PILL_INK, net.ofatech.supernaturalbloodlines.shared.hub.kit.LineColors.FREE_PILL) >= TEXT, "the text on the green pill");
        // every Grimoire index tab colour carries its icon (3:1 for a graphic), chosen or not
        for (net.ofatech.supernaturalbloodlines.shared.hub.HubPage.Group g : net.ofatech.supernaturalbloodlines.shared.hub.HubPage.Group.values()) {
            assertTrue(Contrast.ratio(0xFFE8B0B0, GrimoireTheme.tabColor(g, false)) >= 3.0, g + " tab, resting");
            assertTrue(Contrast.ratio(0xFFFFE3A0, GrimoireTheme.tabColor(g, true)) >= 3.0, g + " tab, chosen");
        }
        // the Blood Moon's gold divider and the Tensura heading text on its panel
        assertTrue(Contrast.ratio(0xFF8fb2c4, 0xFF0b1e2e) >= 3.0, "the Tensura heading on its panel");
    }
}
