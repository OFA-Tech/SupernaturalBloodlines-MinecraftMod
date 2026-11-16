package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import org.junit.jupiter.api.Test;

/** The Pack page's crown and the wording its actions need (plan 14 review). */
class PackUiTest {
    private static double lum(int c) {
        double[] v = new double[3];
        int[] ch = {(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
        for (int i = 0; i < 3; i++) {
            double s = ch[i] / 255.0;
            v[i] = s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    private static double ratio(int a, int b) {
        double x = lum(a);
        double y = lum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    @Test
    void theCrownReadsOnTheGrimoiresPaperAndOnTheDarkThemes() {
        int paper = 0xFFE9DCBC;   // GrimoireTheme.paper()
        int blood = 0xFF1A0A0E;   // the dark themes' ground, about
        assertEquals(BookPackPage.CROWN_ON_PAPER, BookPackPage.crownColor(paper));
        assertEquals(BookPackPage.CROWN_ON_DARK, BookPackPage.crownColor(blood));
        assertTrue(ratio(BookPackPage.CROWN_ON_PAPER, paper) >= 3.0, "the crown is lost on the paper: " + ratio(BookPackPage.CROWN_ON_PAPER, paper));
        assertTrue(ratio(BookPackPage.CROWN_ON_DARK, blood) >= 3.0, "the crown is lost on the dark: " + ratio(BookPackPage.CROWN_ON_DARK, blood));
    }

    @Test
    void theActionsNameWhomTheyAreFor() throws Exception {
        try (var in = PackUiTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("Remove %s from the pack", lang.get("supernaturalbloodlines.hub.pack.kick_for").getAsString());
            assertTrue(lang.get("supernaturalbloodlines.hub.pack.ally_end_for").getAsString().contains("%s"));
            assertEquals("They leave the pack. A werewolf Beta becomes an Omega.", lang.get("supernaturalbloodlines.hub.pack.kick_body").getAsString());
            for (String key : new String[] {"pack.absent_leader", "pack.absent_you", "pack.party_refused", "pack.emissary_not_member", "pack.duel_cooldown", "pack.duel.off.timeout", "pack.duel.off.declined", "hub.pack.challenge_btn", "hub.pack.duel_accept", "hub.pack.duel_decline", "hub.bloodline.sparks", "pack.dissolved", "pack.now_follows", "pack.invite_merge", "pack.left_party", "pack.kicked"}) {
                assertTrue(lang.has("supernaturalbloodlines." + key), key);
            }
        }
    }
}