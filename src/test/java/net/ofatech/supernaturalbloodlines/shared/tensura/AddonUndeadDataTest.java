package net.ofatech.supernaturalbloodlines.shared.tensura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Plan 19d: the undead races of other addons follow our undead rules through our own tag; every entry is optional so the file loads without the addon. */
class AddonUndeadDataTest {
    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    @Test
    void theTagListsTheFourAscensionUndeadAllOptional() throws IOException {
        Path root = root();
        JsonObject tag = JsonParser.parseString(Files.readString(root.resolve("src/main/resources/data/supernaturalbloodlines/tags/manascore_race/races/addon_undead.json")))
                .getAsJsonObject();
        assertFalse(tag.get("replace").getAsBoolean());
        Set<String> ids = new HashSet<>();
        for (JsonElement e : tag.getAsJsonArray("values")) {
            JsonObject entry = e.getAsJsonObject();
            assertFalse(entry.get("required").getAsBoolean(), entry + " must be optional: without Ascension the datapack must still load");
            ids.add(entry.get("id").getAsString());
        }
        assertEquals(Set.of("ascension:lich", "ascension:elder_lich", "ascension:lich_king", "ascension:undead_skeleton"), ids);
        Set<String> ascension = new HashSet<>();
        for (String line : Files.readAllLines(root.resolve("docs/superpowers/research/tensura-addons/ids/ascension_races.txt"))) {
            ascension.add(line.split("\\|")[0].trim());
        }
        assertTrue(ascension.containsAll(ids), "every id is a race Ascension registers");
    }
}
