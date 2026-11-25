package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/** Plan 20d Task 10: the dependency runs one way. Bosses may call the species part; the species part never names the bosses part (the GameTests, which check both, are left out). */
class NoBossesImportTest {
    private static boolean namesBosses(String text) {
        return text.lines().anyMatch(l -> !l.trim().startsWith("//") && !l.trim().startsWith("*") && l.contains("supernaturalbloodlines.parts.bosses"));
    }

    @Test
    void speciesNeverNamesTheBossesPart() {
        List<String> offenders = new ArrayList<>();
        for (String f : SourceTree.javaFiles("main")) {
            if (f.contains("/parts/species/") && !f.contains("/gametest/") && namesBosses(SourceTree.read(f))) {
                offenders.add(f);
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    void theScanSeesTheFilesItIsMeantTo() {
        long scanned = SourceTree.javaFiles("main").stream().filter(f -> f.contains("/parts/species/") && !f.contains("/gametest/")).count();
        assertTrue(scanned > 100, "only " + scanned + " species files were scanned");
        assertTrue(SourceTree.javaFiles("main").stream().anyMatch(f -> f.endsWith("/parts/species/group/brotherhood/KolShadow.java")));
        assertTrue(namesBosses("import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;"));
        assertTrue(!namesBosses("// the supernaturalbloodlines.parts.bosses part"));
    }

    @Test
    void theBossesPartCallsKolShadowOnce() {
        String director = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/director/BossDirector.java");
        assertEquals(1, director.split("KolShadow\\.warn", -1).length - 1);
    }
}