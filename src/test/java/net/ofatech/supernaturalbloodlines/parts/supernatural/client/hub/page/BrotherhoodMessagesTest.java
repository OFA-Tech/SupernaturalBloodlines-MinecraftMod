package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Locale;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/** Every brotherhood line rendered with sample arguments: the names and numbers it is given must reach the text. */
class BrotherhoodMessagesTest {
    private static final JsonObject LANG = JsonParser.parseString(SourceTree.read("src/main/resources/assets/supernaturalbloodlines/lang/en_us.json")).getAsJsonObject();

    private static String render(String key, Object... args) {
        String full = "supernaturalbloodlines." + key;
        assertTrue(LANG.has(full), "missing " + full);
        String text = String.format(Locale.ROOT, LANG.get(full).getAsString(), args);
        for (Object a : args) {
            assertTrue(text.contains(String.valueOf(a)), full + " does not show " + a + ": " + text);
        }
        return text;
    }

    @Test
    void theBreakerHearsTheNewMarkTheDayAndTheStrikes() {
        String text = render("brotherhood.code_you", 9, 24, 2, 3);
        assertTrue(text.contains("bonus") && text.contains("sight") && text.contains("lead"), text);
        String human = render("brotherhood.code_you.unmarked", 24, 1, 3);
        assertTrue(human.contains("bonus") && human.contains("sight") && human.contains("lead"), human);
    }

    @Test
    void theExpelledHearTheDays() {
        render("brotherhood.broke_code", "Rowan");
        render("brotherhood.broke_code.someone");
        render("brotherhood.code_out_days", 7);
    }

    @Test
    void theBrothersHearTheBreakersName() {
        render("brotherhood.code_broken", "Dev");
        render("brotherhood.code_expelled", "Dev");
        render("brotherhood.code_leader", "Dev");
    }

    @Test
    void aLeadChangeNamesTheNewLeaderOrSaysABrother() {
        render("brotherhood.lead_lost", "Rowan");
        render("brotherhood.lead_you", "Rowan");
        assertTrue(render("brotherhood.lead_lost.someone").contains("brother"));
        assertTrue(render("brotherhood.lead_you.someone").contains("brother"));
    }

    @Test
    void theCursedKillerHearsTheStartAndTheEnd() {
        assertTrue(render("brotherhood.curse_start", 48).contains("hours"));
        render("brotherhood.curse_lifted");
    }

    @Test
    void aBarredJoinerSaysWhatToDo() {
        String text = render("brotherhood.err.exiled_brings", "Ann", "Bob");
        assertTrue(text.contains("without") || text.contains("wait"), text);
    }

    @Test
    void theCardsShowTheirNumbers() {
        render("hub.brotherhood.mark", 8, 20);
        render("hub.brotherhood.map.own", 3, 6);
        render("hub.brotherhood.map.complete", 6, 6);
        render("hub.brotherhood.strikes", 2, 3, 7);
        render("hub.brotherhood.breaker", "12\u00A0h");
        render("hub.brotherhood.not_in");
    }

    @Test
    void theCostSaysHowMuchAndWhoIsInnocent() {
        for (String text : List.of(render("hub.brotherhood.code.cost", 40), render("hub.brotherhood.code.cost.quarter"), render("hub.brotherhood.code.cost.none"))) {
            assertTrue(text.contains("villager") && text.contains("not fighting you"), text);
        }
        assertTrue(render("hub.brotherhood.code.cost.quarter").contains("a quarter of your mark"));
    }

    @Test
    void theLeaveButtonNamesTheBrotherhood() {
        assertEquals("Leave the brotherhood", LANG.get("supernaturalbloodlines.hub.group.leave.brotherhood").getAsString());
    }
}
