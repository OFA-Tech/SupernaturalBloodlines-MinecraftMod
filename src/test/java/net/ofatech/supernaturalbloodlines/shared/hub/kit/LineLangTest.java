package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The words of the Bloodline page and of the line messages: the mockup's state cards and every key the code asks for. */
class LineLangTest {
    private static JsonObject lang() throws IOException {
        try (Reader r = new java.io.InputStreamReader(LineLangTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void theNotAVampireCardHasTheMockupsWords() throws IOException {
        JsonObject l = lang();
        assertEquals("No bloodline yet.", l.get("supernaturalbloodlines.hub.empty.bloodline.title").getAsString());
        assertEquals("Only vampires, hybrids and werewolves have a family tree.", l.get("supernaturalbloodlines.hub.empty.bloodline.body").getAsString());
        assertEquals("No tree yet", l.get("supernaturalbloodlines.hub.empty.bloodline.caption").getAsString());
        assertEquals("Open Guide", l.get("supernaturalbloodlines.hub.empty.bloodline.button").getAsString());
    }

    @Test
    void everyKeyTheLineCodeAsksForExists() throws IOException {
        JsonObject l = lang();
        List<String> keys = List.of(
                "supernaturalbloodlines.line.death", "supernaturalbloodlines.line.death_login", "supernaturalbloodlines.line.freed",
                "supernaturalbloodlines.line.unknown_original", "supernaturalbloodlines.line.warning_desiccated_named",
                "supernaturalbloodlines.line.warning_desiccated_hidden", "supernaturalbloodlines.line.warning_open_named",
                "supernaturalbloodlines.line.warning_open_hidden", "supernaturalbloodlines.refusal.line_fails",
                "supernaturalbloodlines.refusal.line_escaped", "supernaturalbloodlines.refusal.line_ring_broken",
                "supernaturalbloodlines.refusal.line_target_away", "supernaturalbloodlines.hub.bloodline.narr.sire_free_unknown",
                "supernaturalbloodlines.hub.bloodline.narr.you_free_unknown", "supernaturalbloodlines.hub.bloodline.act.revelation");
        for (String k : keys) {
            assertTrue(l.has(k), "no text for " + k);
        }
        for (String gone : List.of("supernaturalbloodlines.refusal.line_wrong_blood", "supernaturalbloodlines.refusal.line_wrong_original",
                "supernaturalbloodlines.refusal.line_ring_open", "supernaturalbloodlines.line.warning_named")) {
            assertTrue(!l.has(gone), gone + " is a separate answer that tells a performer too much, or is no longer used");
        }
    }

    @Test
    void theWordsOfTheLineMessages() throws IOException {
        JsonObject l = lang();
        assertEquals("%s has fallen to white oak. Everyone in their line dies with them.", l.get("supernaturalbloodlines.line.death").getAsString());
        assertEquals("The rite fails.", l.get("supernaturalbloodlines.refusal.line_fails").getAsString());
        assertEquals("The target must be online and in this world.", l.get("supernaturalbloodlines.refusal.line_target_away").getAsString());
        assertEquals("The Original escaped the circle. The rite fails.", l.get("supernaturalbloodlines.refusal.line_escaped").getAsString());
        assertTrue(l.get("supernaturalbloodlines.line.warning_open_named").getAsString().contains("lies open to white oak"));
        assertTrue(!l.get("supernaturalbloodlines.line.warning_open_named").getAsString().contains("desiccated"));
        assertTrue(l.get("supernaturalbloodlines.line.warning_desiccated_named").getAsString().contains("lies desiccated"));
        assertTrue(!l.get("supernaturalbloodlines.rite.imprisoned_done").getAsString().contains("or yours"));
    }
}
