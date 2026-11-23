package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.ThemeContrast;
import org.junit.jupiter.api.Test;

class TensuraHubThemeTest {
    @Test
    void tensuraTextIsReadableIncludingTheLabelsOnGrey() {
        ThemeContrast.assertReadable("tensura", new TensuraHubTheme());
    }

    @Test
    void thePackPerkLineIsReadable() {
        var theme = new TensuraHubTheme();
        double ratio = net.ofatech.supernaturalbloodlines.shared.hub.Contrast.ratio(
                net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage.perkColor(theme), theme.paper());
        assertTrue(ratio >= 4.5, "the perk line is " + ratio + ":1 on tensura's paper");
    }

    @Test
    void aLabelThatDoesNotFitIsCutWithAnEllipsis() {
        // a fake font: every character 6 px wide, the ellipsis too
        java.util.function.ToIntFunction<String> six = s -> s.length() * 6;
        assertEquals("Path", TensuraHubLayout.fit("Path", 46, six));
        assertEquals("Bloodl\u2026", TensuraHubLayout.fit("Bloodline", 46, six)); // 40 px left after the ellipsis: 6 letters
        assertTrue(six.applyAsInt(TensuraHubLayout.fit("Werewolves", 46, six)) <= 46);
        assertEquals("", TensuraHubLayout.fit("Werewolves", 4, six));
    }
}
