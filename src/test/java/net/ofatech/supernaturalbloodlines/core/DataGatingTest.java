package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Data that names something a disabled part never registers must not load without the part: a loot table with an unknown item or component is an ERROR
 * at every start, and a tag with a missing required entry is dropped whole. Both showed up in a pack without Vampirism (Holy Packs audit).
 */
class DataGatingTest {
    private static final Path ROOT = root();
    private static final Path DATA = ROOT.resolve("src/main/resources/data");
    private static final Pattern REGISTERED = Pattern.compile("(?:ITEMS|BLOCKS|COMPONENTS)\\s*\\.\\s*register\\w*\\(\\s*\"(\\w+)\"");
    private static final Pattern OURS = Pattern.compile("supernaturalbloodlines:([a-z0-9_]+)(?![a-z0-9_/])");

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    private static List<Path> json(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    /** Registered item, block and component names, to the parts whose package registers them. */
    private static Map<String, Set<Part>> owners() throws IOException {
        Map<String, Set<Part>> owners = new HashMap<>();
        for (Part part : Part.values()) {
            String pkg = part.initClass().substring(0, part.initClass().lastIndexOf('.'));
            Path dir = ROOT.resolve("src/main/java").resolve(pkg.replace('.', '/'));
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("gametest")).toList()) {
                    Matcher m = REGISTERED.matcher(Files.readString(p));
                    while (m.find()) {
                        owners.computeIfAbsent(m.group(1), k -> new HashSet<>()).add(part);
                    }
                }
            }
        }
        return owners;
    }

    private static Set<Part> gatedBy(JsonObject file) {
        Set<Part> gated = new HashSet<>();
        JsonElement conds = file.get("neoforge:conditions");
        if (conds != null) {
            for (JsonElement c : conds.getAsJsonArray()) {
                JsonObject o = c.getAsJsonObject();
                if (o.get("type").getAsString().equals("supernaturalbloodlines:part_enabled")) {
                    for (Part p : Part.values()) {
                        if (p.configKey().equals(o.get("part").getAsString())) {
                            gated.add(p);
                        }
                    }
                }
            }
        }
        return gated;
    }

    /** A condition on {@code gate} also holds the owner when the gate's part cannot load without it. */
    private static boolean covers(Part gate, Part owner) {
        if (gate == owner) {
            return true;
        }
        for (Part required : gate.requiredParts()) {
            if (covers(required, owner)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void everyLootFileThatNamesAPartsContentCarriesThatPartsCondition() throws IOException {
        Map<String, Set<Part>> owners = owners();
        assertTrue(owners.containsKey("rite_lore") && owners.containsKey("rite_page"), "the owner scan found nothing: " + owners.keySet());
        List<String> bad = new ArrayList<>();
        for (String dir : List.of("loot_table", "loot_modifiers")) {
            for (Path file : json(DATA.resolve("supernaturalbloodlines").resolve(dir))) {
                String text = Files.readString(file);
                Set<Part> gated = gatedBy(JsonParser.parseString(text).getAsJsonObject());
                Matcher m = OURS.matcher(text);
                while (m.find()) {
                    Set<Part> own = owners.get(m.group(1));
                    if (own != null && own.stream().noneMatch(o -> gated.stream().anyMatch(g -> covers(g, o)))) {
                        bad.add(DATA.relativize(file) + " names " + m.group(1) + " (part " + own + ") without that part's condition");
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyTagEntryOutsideVanillaIsOptional() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> ns = Files.list(DATA)) {
            for (Path n : ns.toList()) {
                for (Path file : json(n.resolve("tags"))) {
                    for (JsonElement v : JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("values")) {
                        boolean optional = v.isJsonObject() && v.getAsJsonObject().has("required") && !v.getAsJsonObject().get("required").getAsBoolean();
                        String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                        if (!optional && !id.replaceFirst("^#", "").startsWith("minecraft:")) {
                            bad.add(DATA.relativize(file) + ": " + id + " must be {\"id\":..., \"required\": false}");
                        }
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
