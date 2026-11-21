package net.ofatech.supernaturalbloodlines;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every {@code models/item/<id>.json} belongs to an item the code names, or is on the short list of items that a later plan 17 phase
 * registers. The other direction (a registered item has a model and a lang key) needs the registry, see {@code ItemModelCoverageGameTests}.
 * Per-state models ({@code <id>_3d}, the 32 compass frames) are not items.
 */
class ItemModelCoverageTest {
    private static final Path ROOT = projectRoot();
    private static final Path MODELS = ROOT.resolve("src/main/resources/assets/supernaturalbloodlines/models/item");
    private static final Path SOURCES = ROOT.resolve("src/main/java");

    /** The tests run in build/minecraft-junit: the sources are in the nearest parent that has src/main/java. */
    private static Path projectRoot() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    /** Phases 17b to 17h register these; the five spark models are hub icons only (plan 17, open question 4). */
    static final Set<String> NOT_REGISTERED_YET = Set.of("alpha_spark", "true_alpha_spark", "npc_alpha_spark_0",
            "npc_alpha_spark_1", "npc_alpha_spark_2");

    static boolean isItemModel(String id) {
        return !id.endsWith("_3d") && !id.matches("vampire_compass_\\d+");
    }

    static List<String> modelIds() throws Exception {
        try (Stream<Path> files = Files.list(MODELS)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5))
                    .filter(ItemModelCoverageTest::isItemModel).sorted().toList();
        }
    }

    @Test
    void everyItemModelHasAnItemOrIsOnTheList() throws Exception {
        StringBuilder code = new StringBuilder();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                code.append(Files.readString(p));
            }
        }
        String src = code.toString();
        List<String> orphans = new ArrayList<>();
        for (String id : modelIds()) {
            if (!NOT_REGISTERED_YET.contains(id) && !src.contains("\"" + id + "\"")) {
                orphans.add(id);
            }
        }
        assertTrue(orphans.isEmpty(), "item models with no registered item (register it or add it to NOT_REGISTERED_YET): " + orphans);
    }

    @Test
    void theListHoldsOnlyRealModelsSoItCannotRotInto_aSilentExemption() throws Exception {
        List<String> ids = modelIds();
        List<String> stale = NOT_REGISTERED_YET.stream().filter(id -> !ids.contains(id)).sorted().toList();
        assertTrue(stale.isEmpty(), "on the not-registered list but no such model: " + stale);
    }

    @Test
    void anItemOnTheListIsRemovedFromItOnceTheCodeRegistersIt() throws Exception {
        StringBuilder code = new StringBuilder();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                code.append(Files.readString(p));
            }
        }
        String src = code.toString();
        List<String> registered = NOT_REGISTERED_YET.stream().filter(id -> src.contains("\"" + id + "\"")).sorted().toList();
        assertTrue(registered.isEmpty(), "named in the code now, so take it off NOT_REGISTERED_YET: " + registered);
    }
}
