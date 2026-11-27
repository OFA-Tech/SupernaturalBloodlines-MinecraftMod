package net.ofatech.supernaturalbloodlines.parts.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/**
 * R357: a rite tells a player only through {@code RiteTell}, and every {@code return cancel();} of a rite either has just told someone why or says,
 * with a {@code // silent: <reason>} comment, why nobody can be told.
 */
class RiteRefusalRoutingTest {
    private static final Path PARTS = SourceTree.root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts");
    /** The only reasons a rite may cancel without a word: there is nobody to tell. */
    static final List<String> SILENT_REASONS = List.of("no player on the centre", "the performer left");
    private static final List<String> TOLD = List.of("RiteTell.", "RiteTargets.refuseAbout(", "RiteTargets.askForBlood(");

    private static List<Path> riteFiles() throws IOException {
        try (Stream<Path> all = Files.walk(PARTS)) {
            return all.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/gametest/"))
                    .filter(p -> {
                        String s = p.toString().replace('\\', '/');
                        return s.contains("/rite/") || s.contains("/ward/") || s.contains("/unlock/") || p.getFileName().toString().endsWith("RiteFactory.java");
                    })
                    .sorted().toList();
        }
    }

    private static String rel(Path p) {
        return PARTS.relativize(p).toString().replace('\\', '/');
    }

    @Test
    void theScanSeesTheRiteFiles() throws IOException {
        List<String> names = riteFiles().stream().map(RiteRefusalRoutingTest::rel).toList();
        for (String must : List.of("artifacts/ward/WardRiteFactory.java", "artifacts/ward/WardEnforcer.java", "species/npc/WerewolfBrotherRiteFactory.java",
                "species/pack/AlphaTrialRiteFactory.java", "species/unlock/RiteUnlocks.java", "vampirismenchanted/rite/RiteTargets.java")) {
            assertTrue(names.contains(must), must + " is not scanned");
        }
    }

    @Test
    void noRiteFileTalksToAPlayerDirectly() throws IOException {
        List<String> direct = new ArrayList<>();
        for (Path f : riteFiles()) {
            List<String> lines = Files.readAllLines(f);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("displayClientMessage(")) {
                    direct.add(rel(f) + ":" + (i + 1));
                }
            }
        }
        assertEquals(List.of(), direct, "rite files that talk to a player without RiteTell");
    }

    @Test
    void everyCancelHasToldSomeoneOrSaysWhyNot() throws IOException {
        List<String> mute = new ArrayList<>();
        int cancels = 0;
        for (Path f : riteFiles()) {
            List<String> lines = Files.readAllLines(f);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.contains("return cancel();")) {
                    continue;
                }
                cancels++;
                if (!told(lines, i) && !silent(line)) {
                    mute.add(rel(f) + ":" + (i + 1));
                }
            }
        }
        assertTrue(cancels > 60, "the scan found only " + cancels + " cancels");
        assertEquals(List.of(), mute, "cancels that tell nobody and give no allowed reason");
    }

    private static boolean told(List<String> lines, int at) {
        for (int i = Math.max(0, at - 3); i <= at; i++) {
            String l = lines.get(i);
            if (TOLD.stream().anyMatch(l::contains)) {
                return true;
            }
        }
        return false;
    }

    static boolean silent(String line) {
        int c = line.indexOf("// silent: ");
        if (c < 0) {
            return false;
        }
        String reason = line.substring(c + "// silent: ".length());
        return SILENT_REASONS.stream().anyMatch(reason::startsWith);
    }

    @Test
    void onlyAListedReasonMakesACancelSilent() {
        assertTrue(silent("return cancel(); // silent: the performer left, nobody is changed"));
        assertTrue(!silent("return cancel(); // silent: too lazy to say"));
        assertTrue(!silent("return cancel(); // the performer left"));
    }
}
