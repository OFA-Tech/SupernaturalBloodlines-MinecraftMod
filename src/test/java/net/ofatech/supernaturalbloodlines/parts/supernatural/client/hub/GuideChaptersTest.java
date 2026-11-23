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
        assertEquals(List.of("race_hybrid", "race_witch", "hub", "path", "skills", "quests", "rites", "bloodline", "pack", "trial", "loadout", "out_cold", "moon"), c);
    }

    @Test
    void aChapterThatDoesNotApplyIsLeftOut() {
        List<String> vampire = GuideChapters.forRaces(SupernaturalRace.VAMPIRE, Optional.empty(), true, false, false);
        assertFalse(vampire.contains("pack"), "a vampire has no pack");
        assertFalse(vampire.contains("trial"), "a vampire has no Alpha Trial");
        assertFalse(vampire.contains("rites"), "no Enchanted, no rites");
        assertFalse(vampire.contains("loadout"), "no HUD part, no loadout");
        assertTrue(vampire.contains("hub") && vampire.contains("moon"));
        assertFalse(GuideChapters.forRaces(SupernaturalRace.WEREWOLF, Optional.empty(), false, true, true).contains("pack"), "no Werewolves, no pack");
        assertFalse(GuideChapters.forRaces(SupernaturalRace.WEREWOLF, Optional.empty(), false, true, true).contains("trial"), "no Werewolves, no Alpha Trial");
        assertTrue(GuideChapters.forRaces(SupernaturalRace.WEREWOLF, Optional.empty(), true, true, true).contains("trial"), "a werewolf is told about the Alpha Trial");
    }

    @Test
    void theBossesChapterIsThereOnlyWithThePartAndComesLast() {
        List<String> on = GuideChapters.forRaces(SupernaturalRace.VAMPIRE, Optional.empty(), true, true, true, true);
        assertEquals("bosses", on.get(on.size() - 1));
        assertFalse(GuideChapters.forRaces(SupernaturalRace.VAMPIRE, Optional.empty(), true, true, true, false).contains("bosses"));
        assertFalse(GuideChapters.forRaces(SupernaturalRace.VAMPIRE, Optional.empty(), true, true, true).contains("bosses"), "the old overload must not show it");
    }

    @Test
    void everyChapterHasATitleAndATextInTheEnglishFile() throws Exception {
        JsonObject lang;
        try (var in = new InputStreamReader(GuideChaptersTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        for (SupernaturalRace r : SupernaturalRace.values()) {
            for (SupernaturalRace m : SupernaturalRace.values()) {
                for (String id : GuideChapters.forRaces(r, Optional.of(m), true, true, true, true)) {
                    assertTrue(lang.has(GuideChapters.titleKey(id)), "no title for " + id);
                    assertTrue(lang.has(GuideChapters.bodyKey(id)), "no text for " + id);
                }
            }
        }
    }

    @Test
    void noGuideChapterSendsAnyoneToARitesPage() throws Exception {
        JsonObject lang;
        try (var in = new InputStreamReader(GuideChaptersTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        for (var e : lang.entrySet()) {
            assertFalse(e.getValue().getAsString().toLowerCase().contains("rites page"), e.getKey() + ": " + e.getValue().getAsString());
        }
        assertTrue(lang.get(GuideChapters.bodyKey("rites")).getAsString().contains("The Art of Witchcraft"), "the Rites chapter does not name the book");
    }
}
