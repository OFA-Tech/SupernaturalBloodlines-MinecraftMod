package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.core.Config;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClientConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSyncConfig;
import org.junit.jupiter.api.Test;

/**
 * A config key, a game rule or a config class added later without a UI entry fails here, with its name in the message. The COMMON keys
 * must be in {@link AdminEntries#ALL} (the Admin page), the CLIENT keys in {@link PlayerSettings#ALL} (the Settings page).
 */
class AdminCoverageTest {
    private record Known(String file, ModConfigSpec spec, boolean common, String sourceFile) {}

    private static final List<Known> CONFIGS = List.of(
            new Known(Config.FILE, Config.COMMON, true, "Config.java"),
            new Known(VampireSyncConfig.FILE, VampireSyncConfig.SPEC, true, "VampireSyncConfig.java"),
            new Known(SupernaturalConfig.FILE, SupernaturalConfig.SPEC, true, "SupernaturalConfig.java"),
            new Known(HudConfig.FILE, HudConfig.SPEC, true, "HudConfig.java"),
            new Known(ArtifactsConfig.FILE, ArtifactsConfig.SPEC, true, "ArtifactsConfig.java"),
            new Known(SpeciesConfig.FILE, SpeciesConfig.SPEC, true, "SpeciesConfig.java"),
            new Known(SpeciesTensuraConfig.FILE, SpeciesTensuraConfig.SPEC, true, "SpeciesTensuraConfig.java"),
            new Known(CelestialsConfig.FILE, CelestialsConfig.SPEC, true, "CelestialsConfig.java"),
            new Known(BossesConfig.FILE, BossesConfig.SPEC, true, "BossesConfig.java"),
            new Known(HudClientConfig.FILE, HudClientConfig.SPEC, false, "HudClientConfig.java"),
            new Known(HubClientConfig.FILE, HubClientConfig.SPEC, false, "HubClientConfig.java"));

    private static void leaves(UnmodifiableConfig config, String file, Map<String, ModConfigSpec.ConfigValue<?>> out) {
        for (Object v : config.valueMap().values()) {
            if (v instanceof ModConfigSpec.ConfigValue<?> cv) {
                out.put(file + ":" + String.join(".", cv.getPath()), cv);
            } else if (v instanceof UnmodifiableConfig inner) {
                leaves(inner, file, out);
            }
        }
    }

    private static Map<String, ModConfigSpec.ConfigValue<?>> keysOf(Known k) {
        Map<String, ModConfigSpec.ConfigValue<?>> out = new java.util.LinkedHashMap<>();
        leaves(k.spec().getValues(), k.file(), out);
        return out;
    }

    /** The tests run in build/minecraft-junit: the sources are in the nearest parent that has src/main/java. */
    private static Path sourceRoot() {
        for (Path at = Path.of(System.getProperty("user.dir")).toAbsolutePath(); at != null; at = at.getParent()) {
            if (Files.isDirectory(at.resolve("src/main/java"))) {
                return at.resolve("src/main/java");
            }
        }
        throw new IllegalStateException("no src/main/java above " + System.getProperty("user.dir"));
    }

    @Test
    void everyConfigKeyHasAUiEntry() {
        Set<String> admin = new HashSet<>();
        AdminEntries.ALL.forEach(e -> admin.add(e.id()));
        Set<String> player = new HashSet<>(PlayerSettings.ALL);
        List<String> missing = new ArrayList<>();
        for (Known k : CONFIGS) {
            for (String id : keysOf(k).keySet()) {
                if (!(k.common() ? admin : player).contains(id)) {
                    missing.add(id + (k.common() ? " (no row in AdminEntries.ALL)" : " (not in PlayerSettings.ALL)"));
                }
            }
        }
        assertTrue(missing.isEmpty(), "config keys with no UI entry: " + missing);
    }

    @Test
    void everyEntryResolvesInASpecWithTheRightKind() {
        Map<String, ModConfigSpec.ConfigValue<?>> all = new java.util.HashMap<>();
        CONFIGS.stream().filter(Known::common).forEach(k -> all.putAll(keysOf(k)));
        List<String> problems = new ArrayList<>();
        for (SettingEntry e : AdminEntries.ALL) {
            ModConfigSpec.ConfigValue<?> v = all.get(e.id());
            if (v == null) {
                problems.add(e.id() + " is in no spec (renamed or removed?)");
                continue;
            }
            Object def = v.getDefault();
            boolean fits = switch (e.kind()) {
                case BOOL -> def instanceof Boolean;
                case INT -> def instanceof Integer;
                case DOUBLE -> def instanceof Double;
                case CHOICE, TEXT -> def instanceof String;
                case LIST -> def instanceof List<?>;
                default -> false;
            };
            if (!fits) {
                problems.add(e.id() + " is " + e.kind() + " but its default is " + (def == null ? "null" : def.getClass().getSimpleName()));
            }
        }
        for (String id : PlayerSettings.ALL) {
            if (CONFIGS.stream().filter(k -> !k.common()).noneMatch(k -> keysOf(k).containsKey(id))) {
                problems.add(id + " is in no client spec");
            }
        }
        assertTrue(problems.isEmpty(), "stale or mismatched entries: " + problems);
    }

    @Test
    void everySpecIsInTheKnownList() throws IOException {
        Set<String> known = new TreeSet<>();
        CONFIGS.forEach(k -> known.add(k.sourceFile()));
        List<String> unknown = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (Files.readString(f).contains("new ModConfigSpec.Builder()") && !known.contains(f.getFileName().toString())) {
                    unknown.add(f.getFileName() + " builds a config spec and is not in AdminCoverageTest.CONFIGS");
                }
            }
        }
        assertTrue(unknown.isEmpty(), unknown.toString());
    }

    @Test
    void aRegisteredGameRuleNeedsAnEntry() throws IOException {
        long registered = 0;
        try (Stream<Path> files = Files.walk(sourceRoot())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(f);
                int at = -1;
                while ((at = text.indexOf("GameRules.register(", at + 1)) >= 0) {
                    registered++;
                }
            }
        }
        assertEquals(AdminEntries.RULES.size(), registered, "GameRules.register( calls in src/main versus AdminEntries.RULES");
    }
}
