package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import org.junit.jupiter.api.Test;

class GuideChaptersTest {
    @Test
    void whatYouAreComesFirstThenHowTheHubWorks() {
        List<String> c = GuideChapters.forRaces(SupernaturalRace.HYBRID, Optional.of(SupernaturalRace.WITCH), true, true, true);
        assertEquals(List.of("race_hybrid", "race_witch", "hub", "path", "skills", "quests", "rites", "bloodline", "pack", "loadout", "moon"), c);
    }

    @Test
    void aChapterThatDoesNotApplyIsLeftOut() {
        List<String> vampire = GuideChapters.forRaces(SupernaturalRace.VAMPIRE, Optional.empty(), true, false, false);
        assertFalse(vampire.contains("pack"), "a vampire has no pack");
        assertFalse(vampire.contains("rites"), "no Enchanted, no rites");
        assertFalse(vampire.contains("loadout"), "no HUD part, no loadout");
        assertTrue(vampire.contains("hub") && vampire.contains("moon"));
        assertFalse(GuideChapters.forRaces(SupernaturalRace.WEREWOLF, Optional.empty(), false, true, true).contains("pack"), "no Werewolves, no pack");
    }

    @Test
    void everyChapterHasATitleAndATextInTheEnglishFile() throws Exception {
        JsonObject lang;
        try (var in = new InputStreamReader(GuideChaptersTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        for (SupernaturalRace r : SupernaturalRace.values()) {
            for (SupernaturalRace m : SupernaturalRace.values()) {
                for (String id : GuideChapters.forRaces(r, Optional.of(m), true, true, true)) {
                    assertTrue(lang.has(GuideChapters.titleKey(id)), "no title for " + id);
                    assertTrue(lang.has(GuideChapters.bodyKey(id)), "no text for " + id);
                }
            }
        }
    }
}
