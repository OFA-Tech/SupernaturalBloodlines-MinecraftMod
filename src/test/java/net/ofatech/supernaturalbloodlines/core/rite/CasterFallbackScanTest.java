package net.ofatech.supernaturalbloodlines.core.rite;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every rite factory takes its performer from {@code RiteTargets.performer} (PerformerRule): the old "the caster, or else the target" line made the target
 * stand in for a performer who had left, and a rite that asks the target's consent only when performer and target differ then changed an unwilling player.
 */
class CasterFallbackScanTest {
    private static Path base() {
        Path at = Path.of("").toAbsolutePath();
        while (at != null && !Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return at.resolve("src/main/java/net/ofatech/supernaturalbloodlines");
    }

    @Test
    void noRiteFactoryLetsTheTargetStandInForAPerformerWhoLeft() throws IOException {
        List<String> bad = new ArrayList<>();
        int factories = 0;
        try (Stream<Path> files = Files.walk(base())) {
            for (Path p : files.filter(f -> f.getFileName().toString().endsWith("RiteFactory.java")).toList()) {
                factories++;
                String src = Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
                if (src.matches("(?s).*findEntity\\([^)]*\\)\\s*instanceof\\s+ServerPlayer\\s+\\w+\\s*\\?\\s*\\w+\\s*:\\s*target\\b.*")) {
                    bad.add(p.getFileName().toString());
                }
            }
        }
        assertTrue(factories >= 19, "the scan found only " + factories + " rite factories: the path moved");
        assertTrue(bad.isEmpty(), "rite factories where the target stands in for a performer who left (use RiteTargets.performer): " + bad);
    }

    @Test
    void theNineFactoriesThatNeededItUseThePerformerHelper() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String name : List.of("parts/vampirismenchanted/rite/FactionRiteFactory.java", "parts/artifacts/rite/BindItemRiteFactory.java", "parts/species/pack/LineageRiteFactory.java",
                "parts/species/rite/SeveranceRiteFactory.java", "parts/species/rite/HybridBloodRiteFactory.java", "parts/species/rite/AncestralCallingRiteFactory.java",
                "parts/species/rite/OriginalBloodRiteFactory.java", "parts/evolutionrites/rite/EvolutionRiteFactory.java", "parts/evolutionrites/rite/MasteryRiteFactory.java")) {
            if (!Files.readString(base().resolve(name)).contains("RiteTargets.performer(")) {
                missing.add(name);
            }
        }
        assertTrue(missing.isEmpty(), "these factories do not take their performer from RiteTargets.performer: " + missing);
    }
}