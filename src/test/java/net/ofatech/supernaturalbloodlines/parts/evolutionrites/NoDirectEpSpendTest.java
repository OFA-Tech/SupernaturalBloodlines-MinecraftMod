package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Plan 20b: EP leaves a player in one place ({@code RiteEp}), so a circle can split it. No rite or mastery source writes base Max EP itself. */
class NoDirectEpSpendTest {
    private static final Path ROOT = root();
    private static final Path MAIN = ROOT.resolve("src/main/java/net/ofatech/supernaturalbloodlines");

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    private static List<Path> sources(String dir) throws IOException {
        try (Stream<Path> s = Files.walk(MAIN.resolve(dir))) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void noRiteOrMasterySourceWritesBaseMaxEpItself() throws IOException {
        for (String dir : List.of("parts/evolutionrites/rite", "parts/evolutionrites/mastery")) {
            for (Path p : sources(dir)) {
                assertTrue(!Files.readString(p).contains("setBaseMaxEP"), p.getFileName() + " writes base Max EP: go through RiteEp");
            }
        }
    }

    @Test
    void riteEpIsTheOneWriter() throws IOException {
        Path riteEp = MAIN.resolve("parts/evolutionrites/RiteEp.java");
        assertTrue(Files.exists(riteEp) && Files.readString(riteEp).contains("EnergyHelper.setBaseMaxEP"), "RiteEp is missing or writes nothing");
    }
}
