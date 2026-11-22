package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final Pattern REGISTER_CALL = Pattern.compile("\\b(?:ITEMS|BLOCKS|COMPONENTS)\\s*\\.\\s*register(?!SimpleBlockItem)\\w*\\((?!\\s*\\w*[bB]us\\s*\\))");
    private static final Pattern IS_LOADED = Pattern.compile("isLoaded\\(\"(\\w+)\"\\)");
    private static final Pattern OURS = Pattern.compile("supernaturalbloodlines:([a-z0-9_]+)(?![a-z0-9_/])");

    /** One place that registers a name: the part, and the mods that must be loaded for that registration to run. */
    private record Owner(Part part, Set<String> mods) {
    }

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

    /** Every {@code data/<namespace>/<kind>} folder. */
    private static List<Path> dataDirs(String kind) throws IOException {
        try (Stream<Path> ns = Files.list(DATA)) {
            return ns.map(n -> n.resolve(kind)).toList();
        }
    }

    /**
     * Registered item, block and component names, to where they are registered. A registration under a {@code werewolves} package, or right after
     * {@code isLoaded("mod")}, also needs that mod. Fails when a registration call has no literal name, since the scan would miss it.
     */
    private static Map<String, List<Owner>> owners() throws IOException {
        Map<String, List<Owner>> owners = new HashMap<>();
        for (Part part : Part.values()) {
            String pkg = part.initClass().substring(0, part.initClass().lastIndexOf('.'));
            Path dir = ROOT.resolve("src/main/java").resolve(pkg.replace('.', '/'));
            assertTrue(Files.isDirectory(dir), part + ": no source folder " + dir);
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("gametest")).toList()) {
                    String text = Files.readString(p);
                    int calls = (int) REGISTER_CALL.matcher(text).results().count();
                    Matcher m = REGISTERED.matcher(text);
                    int named = 0;
                    while (m.find()) {
                        named++;
                        Set<String> mods = new HashSet<>();
                        if (p.toString().replace('\\', '/').contains("/werewolves/")) {
                            mods.add("werewolves");
                        }
                        Matcher loaded = IS_LOADED.matcher(text.substring(Math.max(0, m.start() - 160), m.start()));
                        while (loaded.find()) {
                            mods.add(loaded.group(1));
                        }
                        owners.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(new Owner(part, mods));
                    }
                    assertEquals(calls, named, ROOT.relativize(p) + ": ITEMS/BLOCKS/COMPONENTS registrations, and those with a literal name");
                }
            }
        }
        return owners;
    }

    private static List<JsonObject> conditions(JsonObject file) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement conds = file.get("neoforge:conditions");
        if (conds != null) {
            conds.getAsJsonArray().forEach(c -> out.add(c.getAsJsonObject()));
        }
        return out;
    }

    private static Set<Part> gatedBy(JsonObject file) {
        Set<Part> gated = new HashSet<>();
        for (JsonObject o : conditions(file)) {
            if (o.get("type").getAsString().equals("supernaturalbloodlines:part_enabled")) {
                for (Part p : Part.values()) {
                    if (p.configKey().equals(o.get("part").getAsString())) {
                        gated.add(p);
                    }
                }
            }
        }
        return gated;
    }

    private static Set<String> modsRequiredBy(JsonObject file) {
        Set<String> mods = new HashSet<>();
        for (JsonObject o : conditions(file)) {
            if (o.get("type").getAsString().equals("neoforge:mod_loaded")) {
                mods.add(o.get("modid").getAsString());
            }
        }
        return mods;
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
    void everyLootFileThatNamesAPartsContentCarriesThatPartsConditions() throws IOException {
        Map<String, List<Owner>> owners = owners();
        assertTrue(owners.containsKey("rite_lore") && owners.containsKey("rite_page") && owners.containsKey("moon_ring"), "the owner scan found nothing: " + owners.keySet());
        assertTrue(owners.get("wolf_lichen").stream().anyMatch(o -> o.mods().contains("werewolves")), "the scan did not see that wolf_lichen needs werewolves");
        Set<String> modifiers = new HashSet<>();
        for (Path dir : dataDirs("loot_modifiers")) {
            json(dir).forEach(p -> modifiers.add(p.getFileName().toString().replace(".json", "")));
        }
        List<String> bad = new ArrayList<>();
        int files = 0;
        for (String kind : List.of("loot_table", "loot_modifiers")) {
            for (Path dir : dataDirs(kind)) {
                for (Path file : json(dir)) {
                    files++;
                    String text = Files.readString(file);
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    Set<Part> gated = gatedBy(json);
                    Set<String> mods = modsRequiredBy(json);
                    json.remove("neoforge:conditions"); // the condition types are ours too, and are not content
                    Matcher m = OURS.matcher(json.toString());
                    while (m.find()) {
                        List<Owner> own = owners.get(m.group(1));
                        if (own == null && modifiers.contains(m.group(1))) {
                            continue; // global_loot_modifiers.json lists our modifier files
                        }
                        if (own == null) {
                            bad.add(DATA.relativize(file) + " names " + m.group(1) + ", which no part registers (is the registration scan missing it?)");
                        } else if (own.stream().noneMatch(o -> gated.stream().anyMatch(g -> covers(g, o.part())) && mods.containsAll(o.mods()))) {
                            bad.add(DATA.relativize(file) + " names " + m.group(1) + " (" + own + ") without that part's part_enabled and mod_loaded conditions");
                        }
                    }
                }
            }
        }
        assertTrue(files > 10, "only " + files + " loot files scanned");
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyTagEntryOutsideVanillaIsOptional() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path tags : dataDirs("tags")) {
            for (Path file : json(tags)) {
                for (JsonElement v : JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("values")) {
                    boolean optional = v.isJsonObject() && v.getAsJsonObject().has("required") && !v.getAsJsonObject().get("required").getAsBoolean();
                    String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                    if (!optional && !id.replaceFirst("^#", "").startsWith("minecraft:")) {
                        bad.add(DATA.relativize(file) + ": " + id + " must be {\"id\":..., \"required\": false}");
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
