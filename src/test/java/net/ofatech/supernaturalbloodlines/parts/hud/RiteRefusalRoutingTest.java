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
    /** What tells someone why a rite stops: a refusal, never a success line ({@code RiteTell.say}). */
    private static final List<String> TOLD = List.of("RiteTell.refuse(", "RiteTell.refuseGeneric(", "RiteTargets.refuseAbout(", "RiteTargets.askForBlood(",
            "RiteTargets.tell(");
    /**
     * The chat lines a rite file may send itself, per file: answers and notices that were always chat (a gene revealed, a ward invitation, the
     * trial confirmed, a line broken), never a refusal. A new one fails here by file.
     */
    static final java.util.Map<String, Integer> CHAT_ANSWERS = java.util.Map.of(
            "artifacts/rite/LocatorNotices.java", 2, "artifacts/ward/WardService.java", 6, "species/npc/WerewolfBrotherRiteFactory.java", 1,
            "species/pack/AlphaTrialRiteFactory.java", 3, "species/pack/LineageRiteFactory.java", 1, "species/rite/BrokenLineRiteFactory.java", 1,
            "species/rite/DoppelgangerBloodRiteFactory.java", 1, "species/rite/ImprisonedOriginalRiteFactory.java", 1, "species/rite/RevelationRiteFactory.java", 1,
            "species/unlock/RiteUnlocks.java", 2);

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
    void onlyTheListedAnswersGoToChatDirectly() throws IOException {
        java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
        for (Path f : riteFiles()) {
            int n = 0;
            for (String line : Files.readAllLines(f)) {
                n += line.contains("sendSystemMessage(") ? 1 : 0;
            }
            if (n > 0) {
                seen.put(rel(f), n);
            }
        }
        assertEquals(new java.util.TreeMap<>(CHAT_ANSWERS), seen, "chat lines a rite file sends itself (an answer is listed with its count; a refusal goes through RiteTell)");
    }

    @Test
    void everyCancelHasToldSomeoneOrSaysWhyNot() throws IOException {
        List<String> mute = new ArrayList<>();
        int cancels = 0;
        for (Path f : riteFiles()) {
            List<String> lines = Files.readAllLines(f);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.contains("cancel();")) {
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

    /** Whether a refusal is said in the block the cancel is in (nested blocks of it included), before the cancel. */
    static boolean told(List<String> lines, int at) {
        int depth = 0;
        for (int i = at; i >= 0; i--) {
            String l = lines.get(i);
            if (i < at && TOLD.stream().anyMatch(l::contains)) {
                return true;
            }
            if (i == at && TOLD.stream().anyMatch(s -> l.indexOf(s) >= 0 && l.indexOf(s) < l.indexOf("cancel();"))) {
                return true;
            }
            if (i < at) {
                depth += count(l, '}') - count(l, '{');
                if (depth < 0) {
                    return false;
                }
            }
        }
        return false;
    }

    private static int count(String s, char c) {
        return (int) s.chars().filter(x -> x == c).count();
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
    void aRefusalInAnotherBlockDoesNotCount() {
        assertTrue(told(List.of("if (a) {", "    RiteTell.refuse(p, K);", "    return cancel();", "}"), 2));
        assertTrue(told(List.of("if (a) {", "    if (p != null) {", "        RiteTell.refuse(p, K);", "    }", "    return cancel();", "}"), 4));
        assertTrue(!told(List.of("if (b) {", "    RiteTell.refuse(p, K);", "}", "if (a) {", "    return cancel();", "}"), 4));
        assertTrue(!told(List.of("if (a) {", "    RiteTell.say(p, K);", "    return cancel();", "}"), 2));
        assertTrue(!told(List.of("if (a) {", "    cancel(); return false;", "}"), 1));
    }

    @Test
    void onlyAListedReasonMakesACancelSilent() {
        assertTrue(silent("return cancel(); // silent: the performer left, nobody is changed"));
        assertTrue(!silent("return cancel(); // silent: too lazy to say"));
        assertTrue(!silent("return cancel(); // the performer left"));
    }
}
