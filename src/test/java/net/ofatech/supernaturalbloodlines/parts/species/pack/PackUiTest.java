package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            for (String key : new String[] {"pack.duel.disconnected", "pack.duel.resumed_you", "pack.duel.forfeit_won_now", "pack.duel.forfeit_won_same", "pack.duel_no_kick", "pack.duel.resumed", "pack.grace_no_spark", "pack.grace_no_spark_you", "pack.duel.off.range", "pack.duel.forfeit_lost_now", "pack.duel.forfeit_lost_same", "pack.duel_called_off", "pack.spark_lost_stacked", "pack.duel.forfeit", "pack.absent_leader", "pack.absent_you", "pack.absent_lead_now", "pack.spark_gained", "pack.spark_capped", "pack.spark_lost", "pack.spark_reset", "pack.duel.off.died", "hub.pack.duel_accept_for", "hub.pack.duel_decline_for", "hub.pack.duel_asked", "pack.party_refused", "pack.emissary_not_member", "pack.duel_cooldown", "pack.duel.off.timeout", "pack.duel.off.declined", "hub.pack.challenge_btn", "hub.pack.duel_accept", "hub.pack.duel_decline", "hub.bloodline.sparks", "pack.dissolved", "pack.now_follows", "pack.invite_merge", "pack.left_party", "pack.kicked"}) {
                assertTrue(lang.has("supernaturalbloodlines." + key), key);
            }
        }
    }

    @Test
    void theCooldownMessagesTakeTheMinutesLeft() throws Exception {
        try (var in = PackUiTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(lang.get("supernaturalbloodlines.pack.duel_cooldown").getAsString().contains("%s minute(s)"));
            assertTrue(lang.get("supernaturalbloodlines.pack.duel_retry").getAsString().contains("%s minute(s)"));
            assertTrue(lang.get("supernaturalbloodlines.pack.spark_gained").getAsString().split("%s", -1).length == 3, "spark_gained takes the count and the cap");
        }
    }

    @Test
    void theNameGivesWayBeforeTheRankAndNeverBelowAFewLetters() {
        assertEquals(110, BookPackPage.nameRoom(200, 80));
        assertEquals(16, BookPackPage.nameRoom(60, 80), "a narrow row still shows a few letters of the name");
        assertEquals(16, BookPackPage.nameRoom(0, 200));
    }

    @Test
    void declineSitsBesideAcceptWhenItFitsAndOnTheNextLineWhenItDoesNot() {
        assertEquals(68, BookPackPage.declineX(0, 60, 40, 200), "8 pixels after Accept");
        assertEquals(-1, BookPackPage.declineX(0, 100, 60, 150), "108 + 60 does not fit in 150");
        assertEquals(108, BookPackPage.declineX(0, 100, 42, 150), "108 + 42 fits exactly");
        assertEquals(-1, BookPackPage.declineX(20, 100, 43, 150), "the x of the page counts");
    }

    @Test
    void aRowTooNarrowForTheNameAndTheWholeRankPutsTheRankOnItsOwnLine() {
        assertFalse(BookPackPage.rankOnOwnLine(200, 50), "plenty of room");
        assertFalse(BookPackPage.rankOnOwnLine(100, 66), "24 pixels left for the name is the least");
        assertTrue(BookPackPage.rankOnOwnLine(100, 67), "23 left is too few");
        assertTrue(BookPackPage.rankOnOwnLine(60, 60), "a rank as wide as the row");
    }

    @Test
    void theCrownStaysOnAStackedRowUnlessTheRankStillDoesNotFitBesideIt() {
        assertFalse(BookPackPage.dropCrown(100, 60), "the rank fits on its own line beside the crown");
        assertFalse(BookPackPage.dropCrown(100, 83), "83 + 8 + 9 = 100 fits exactly");
        assertTrue(BookPackPage.dropCrown(100, 84), "one pixel more does not");
    }
}
