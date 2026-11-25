package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Plan 20c, R274: every class that reads Vampirism's blood-drink events is listed with what it does to a share (a drink with an empty
 * source that a clanmate gets), so a new listener fails this test by name until somebody has decided.
 */
class FeedListenerAuditTest {
    static final String IGNORES_EMPTY = "ignores an empty source";
    static final String NEEDS_GUARD = "needs the SharedFeed guard";
    static final String NOT_A_RULE = "not a feed rule";
    static final String DRINKER_RULE = "acts on the drinker, wanted on a share";

    /** File name -> what it does to a drink with no living source. */
    static final Map<String, String> LISTENERS = Map.ofEntries(
            Map.entry("AshDagger.java", DRINKER_RULE),
            Map.entry("VervainRules.java", IGNORES_EMPTY),
            Map.entry("Detectors.java", IGNORES_EMPTY),
            Map.entry("BloodMoonBite.java", IGNORES_EMPTY),
            Map.entry("BloodEffects.java", IGNORES_EMPTY),
            Map.entry("TurningHandler.java", IGNORES_EMPTY),
            Map.entry("GhoulGates.java", IGNORES_EMPTY),
            Map.entry("AugustineRules.java", NEEDS_GUARD),
            Map.entry("TensuraFeed.java", NOT_A_RULE));

    private static Path projectRoot() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    /** File name -> path of every non-GameTest main source that names a blood-drink event. */
    static Map<String, Path> mentions() throws Exception {
        Map<String, Path> found = new TreeMap<>();
        try (Stream<Path> files = Files.walk(projectRoot().resolve("src/main/java"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String unix = p.toString().replace('\\', '/');
                if (unix.contains("/gametest/")) {
                    continue;
                }
                String text = Files.readString(p);
                if (text.contains("PlayerDrinkBloodEvent") || text.contains("EntityDrinkBloodEvent") || text.contains("BloodDrinkEvent")) {
                    found.put(p.getFileName().toString(), p);
                }
            }
        }
        return found;
    }

    @Test
    void everyListenerIsOnTheList() throws Exception {
        List<String> unlisted = new ArrayList<>();
        for (String name : mentions().keySet()) {
            if (!LISTENERS.containsKey(name)) {
                unlisted.add(name);
            }
        }
        assertTrue(unlisted.isEmpty(), "reads a blood-drink event with no entry in FeedListenerAuditTest.LISTENERS (say what it does to a share): " + unlisted);
    }

    @Test
    void theListHoldsOnlyRealListeners() throws Exception {
        Map<String, Path> found = mentions();
        List<String> stale = LISTENERS.keySet().stream().filter(n -> !found.containsKey(n)).sorted().toList();
        assertTrue(stale.isEmpty(), "on the list but it no longer names a blood-drink event: " + stale);
    }

    @Test
    void everyEntryIsOneOfTheKnownAnswers() {
        for (Map.Entry<String, String> e : LISTENERS.entrySet()) {
            assertTrue(List.of(IGNORES_EMPTY, NEEDS_GUARD, NOT_A_RULE, DRINKER_RULE).contains(e.getValue()), e.getKey() + " has the answer '" + e.getValue() + "'");
        }
    }

    @Test
    void aListenerThatIgnoresAnEmptySourceTestsTheSourceEntity() throws Exception {
        Map<String, Path> found = mentions();
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, String> e : LISTENERS.entrySet()) {
            if (IGNORES_EMPTY.equals(e.getValue())) {
                String text = Files.readString(found.get(e.getKey()));
                if (!text.contains("getBloodSource().getEntity()")) {
                    bad.add(e.getKey());
                }
            }
        }
        assertEquals(List.of(), bad, "listed as ignoring an empty source but it never reads getBloodSource().getEntity()");
    }
}
