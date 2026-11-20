package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Controls shows the lang entry of each key: a key with none shows the raw "key.supernaturalbloodlines.slot_1". */
class KeyNamesTest {
    private static final Pattern KEY = Pattern.compile("\"(key\\.(?:categories\\.)?supernaturalbloodlines\\.[a-z0-9_]*[a-z0-9])\"");

    private static JsonObject lang() throws IOException {
        try (var in = KeyNamesTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            return JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The project folder (the tests may run in a folder below it, like run/): the first folder from the working directory up that has a build.gradle. */
    private static Path project() throws Exception {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("build.gradle"))) {
            p = p.getParent();
        }
        return p;
    }

    @Test
    void everyDefaultKeyHasAName() throws IOException {
        JsonObject lang = lang();
        for (HudKeyDefaults.Key k : HudKeyDefaults.ALL) {
            assertTrue(lang.has(k.id()), "no lang entry for " + k.id());
            assertFalse(lang.get(k.id()).getAsString().startsWith("key."), k.id() + " is named with its own key");
        }
    }

    @Test
    void theHudKeysAreNamedForThePlayer() throws IOException {
        JsonObject lang = lang();
        assertEquals("HUD: switch mode", lang.get(HudKeyDefaults.HUD_MODE.id()).getAsString());
        for (int i = 0; i < HudKeyDefaults.SLOTS.size(); i++) {
            assertEquals("HUD slot " + (i + 1), lang.get(HudKeyDefaults.SLOTS.get(i).id()).getAsString());
        }
    }

    /** Every key or category our source names in a string, found by reading the sources, so a key added later is checked without being listed here. */
    @Test
    void everyKeyMappingNamedInTheSourcesHasALangEntry() throws Exception {
        JsonObject lang = lang();
        TreeSet<String> missing = new TreeSet<>();
        int found = 0;
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(project().resolve("src/main/java"))) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        }
        for (Path file : files) {
            Matcher m = KEY.matcher(Files.readString(file));
            while (m.find()) {
                found++;
                if (!lang.has(m.group(1))) {
                    missing.add(m.group(1) + " (" + file.getFileName() + ")");
                }
            }
        }
        assertTrue(found > 5, "the scan found nothing: it is not looking at the sources");
        assertTrue(missing.isEmpty(), "keys with no lang entry: " + missing);
    }
}
