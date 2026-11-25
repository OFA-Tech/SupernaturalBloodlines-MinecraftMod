package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The Charter is one item with a model and a name, in no tab, no recipe and no loot, stacking to one, carrying two components. */
class CharterItemRegistryTest {
    private static Path root() {
        Path at = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return at;
    }

    private static String read(String relative) throws Exception {
        return Files.readString(root().resolve(relative));
    }

    private static final String REG = "src/main/java/net/ofatech/supernaturalbloodlines/parts/species/SpeciesRegistries.java";

    @Test
    void itIsOneItemWithAModelAndAName() throws Exception {
        assertTrue(Files.exists(root().resolve("src/main/resources/assets/supernaturalbloodlines/models/item/clan_charter.json")));
        assertTrue(Files.exists(root().resolve("src/main/resources/assets/supernaturalbloodlines/textures/item/clan_charter.png")));
        JsonObject lang = GroupTextTest.lang();
        assertTrue(lang.has("item.supernaturalbloodlines.clan_charter"));
        assertTrue(lang.has("item.supernaturalbloodlines.clan_charter.tooltip"));
        String reg = read(REG);
        assertEquals(1, reg.split("ITEMS\\.register\\(\"clan_charter\"", -1).length - 1);
    }

    @Test
    void itIsInNoCreativeTab() throws Exception {
        try (Stream<Path> files = Files.walk(root().resolve("src/main/java"))) {
            List<String> bad = new ArrayList<>();
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String text = Files.readString(p);
                if (text.contains("BuildCreativeModeTabContentsEvent") && java.util.regex.Pattern.compile("accept\\(\\s*[\\w.]*CLAN_CHARTER").matcher(text).find()) {
                    bad.add(p.getFileName().toString());
                }
            }
            assertEquals(List.of(), bad, "a class that fills a creative tab names the Charter");
        }
    }

    @Test
    void noRecipeOrLootNamesIt() throws Exception {
        Path data = root().resolve("src/main/resources/data");
        if (!Files.isDirectory(data)) {
            return;
        }
        try (Stream<Path> files = Files.walk(data)) {
            List<String> bad = new ArrayList<>();
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                if (Files.readString(p).contains("clan_charter")) {
                    bad.add(data.relativize(p).toString());
                }
            }
            assertEquals(List.of(), bad, "a recipe, loot table or tag names the Charter");
        }
    }

    @Test
    void itStacksToOne() throws Exception {
        String reg = read(REG);
        int at = reg.indexOf("ITEMS.register(\"clan_charter\"");
        assertTrue(reg.substring(at, reg.indexOf(';', at)).contains("stacksTo(1)"));
    }

    @Test
    void theTwoComponentsAreRegistered() throws Exception {
        String reg = read(REG);
        assertTrue(reg.contains("registerComponentType(\"charter_id\"") && reg.contains("registerComponentType(\"charter_group\""));
        assertEquals(2, reg.split("UUIDUtil\\.STREAM_CODEC", -1).length - 1 - (reg.split("UUIDUtil\\.STREAM_CODEC", -1).length - 1 - 2), "both components are network synchronised");
    }
}
