package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** A boss bar and a death message show the entity's lang name: one with none shows the raw "entity.supernaturalbloodlines.deucalion". */
class EntityNamesTest {
    private static final Pattern ENTITY = Pattern.compile("ENTITY_TYPES\\.register\\(\\s*\"([a-z0-9_]+)\"");

    private static Path project() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("build.gradle"))) {
            p = p.getParent();
        }
        return p;
    }

    @Test
    void everyEntityTypeWeRegisterHasALangName() throws Exception {
        JsonObject lang;
        try (var in = EntityNamesTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            lang = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(project().resolve("src/main/java"))) {
            walk.filter(f -> f.toString().endsWith(".java")).forEach(files::add);
        }
        TreeSet<String> missing = new TreeSet<>();
        int found = 0;
        for (Path file : files) {
            Matcher m = ENTITY.matcher(Files.readString(file));
            while (m.find()) {
                found++;
                String key = "entity.supernaturalbloodlines." + m.group(1);
                if (!lang.has(key)) {
                    missing.add(key + " (" + file.getFileName() + ")");
                }
            }
        }
        assertTrue(found > 5, "the scan found nothing: it is not looking at the sources");
        assertTrue(missing.isEmpty(), "entity types with no lang name: " + missing);
    }
}
