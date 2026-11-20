package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import org.junit.jupiter.api.Test;

/** Plan 17b panel: the words the players read (admin labels, the Guide chapter, the hints) say what the review asked them to say. */
class BossTextTest {
    private static JsonObject lang() throws Exception {
        try (var in = new InputStreamReader(BossTextTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static String label(String key) {
        return "supernaturalbloodlines.hub.admin.key." + BossesConfig.FILE + ":" + key;
    }

    @Test
    void everyBossSwitchIsLabelledBossColonName() throws Exception {
        JsonObject lang = lang();
        for (BossId b : BossId.values()) {
            String key = label(b.id() + "_enabled");
            assertTrue(lang.has(key), "no admin label for " + key);
            String name = lang.get("supernaturalbloodlines.boss.name." + b.id()).getAsString();
            assertEquals("Boss: " + name, lang.get(key).getAsString(), key);
        }
    }

    @Test
    void theStakeSwitchSaysWhatItNeeds() throws Exception {
        assertEquals("NPC Original stake needs a desiccated target", lang().get(label("npc_original_stake_needs_open")).getAsString());
    }

    @Test
    void theOriginalsSwitchIsLabelled() throws Exception {
        assertTrue(lang().has(label("mikael_stakes_originals")));
    }

    @Test
    void theGuideChapterSaysPlainlyThatYourOriginalsDeathIsYours() throws Exception {
        String body = lang().get("supernaturalbloodlines.hub.guide.bosses.body").getAsString();
        assertTrue(body.contains("If your Original dies to white oak, you die too"), body);
        assertTrue(body.contains("rung 3"), "the rung is not given");
        assertTrue(body.contains("\n\n"), "the chapter is not split into short paragraphs");
        assertTrue(lang().has("supernaturalbloodlines.hub.guide.bosses.link"), "no link to the Bloodline page");
    }

    @Test
    void theHomeLinesAreThereForAStandingAndAFallenBossNamedOrNot() throws Exception {
        JsonObject lang = lang();
        for (String k : new String[] {"boss_near", "boss_near_named", "boss_fallen", "boss_fallen_named"}) {
            assertTrue(lang.has("supernaturalbloodlines.hub.home." + k), k);
        }
        assertEquals("An Original lies fallen nearby.", lang.get("supernaturalbloodlines.hub.home.boss_fallen").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.hub.home.boss_near").getAsString().contains("128"), "the Home line does not say the real distance");
    }

    @Test
    void theHintsAndTheRisingAreWritten() throws Exception {
        JsonObject lang = lang();
        assertEquals("White oak only works on an Original who is down.", lang.get("supernaturalbloodlines.boss.white_oak_closed").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.boss.notice.mikael.hint").getAsString().contains("Mountain ash will hold him."));
        assertTrue(lang.get("supernaturalbloodlines.line.risen_named").getAsString().contains("Your line is safe again."));
        assertTrue(lang.get("supernaturalbloodlines.line.risen_hidden").getAsString().contains("Your line is safe again."));
        assertTrue(lang.get("supernaturalbloodlines.boss.bar.desiccated").getAsString().contains("desiccated"));
        assertTrue(lang.has("supernaturalbloodlines.boss.notice.mikael.stake"));
    }
}
