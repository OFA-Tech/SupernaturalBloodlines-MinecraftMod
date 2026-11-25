package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupTextTest;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.ClanPlan.Button;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineColors;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PackRing;
import org.junit.jupiter.api.Test;

/** What the clan cards say while they wait, and why a button is grey: in plain view, not in a tooltip. */
class ClanUiHonestyTest {
    private static Button button(boolean enabled, String why) {
        return new Button("pack:challenge", "C11", "challenge", "challenge.what", enabled, true, why, java.util.List.of());
    }

    @Test
    void aGreyButtonShowsItsReasonAsALine() {
        assertEquals(Optional.of(ClanPlan.KEY + "why.too_new"), ClanCards.reasonLine(button(false, "why.too_new")));
        assertEquals(Optional.empty(), ClanCards.reasonLine(button(true, "")), "a live button needs no reason");
        assertEquals(Optional.empty(), ClanCards.reasonLine(button(true, "why.too_new")), "a live button shows no stale reason");
        assertEquals(Optional.empty(), ClanCards.reasonLine(button(false, "")), "no reason known, none shown");
    }

    @Test
    void theCardsWaitWhileNoViewHasCome() {
        assertTrue(ClanCards.loading(ClanView.NONE));
        assertFalse(ClanCards.loading(new ClanView(true, 1, -1, ClanView.Charter.NONE, ClanView.Contest.NONE, false, 0, 0)));
    }

    @Test
    void theLoadingLineHasItsWords() throws Exception {
        assertTrue(GroupTextTest.lang().has(ClanPlan.KEY + "loading"));
    }

    @Test
    void everyRankRingMeetsThreeToOneOnEveryPaper() {
        int[] papers = {new GrimoireTheme().paper(), new TensuraHubTheme().paper(), LineColors.MOON_SKY_TOP, LineColors.MOON_SKY_BOTTOM};
        for (PackRing.Role role : PackRing.Role.values()) {
            for (int paper : papers) {
                int shown = BookPackPage.ringColor(role.color(), paper);
                assertTrue(Contrast.ratio(shown, paper) >= 3.0, role + " ring " + Integer.toHexString(shown) + " on " + Integer.toHexString(paper) + ": " + Contrast.ratio(shown, paper));
                assertEquals(0xFF, shown >>> 24, "a ring colour is opaque");
            }
        }
    }

    @Test
    void aThemedTitleNeedsTheRoomOfItsFont() throws Exception {
        java.nio.file.Path at = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        String kit = java.nio.file.Files.readString(at.resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/PageKit.java"));
        int from = kit.indexOf("static int title(HubScreen.Frame f, Component c, int x, int y, int w, int color) {");
        assertTrue(from > 0 && kit.substring(from, kit.indexOf("}", from)).contains("TitleFont.HEIGHT"), "a themed title is whole only with the room of its own font");
    }

    @Test
    void aRingThatAlreadyReadsIsLeftAlone() {
        assertEquals(0xFF000000, BookPackPage.ringColor(0x000000, 0xFFFFFFFF));
    }
}
