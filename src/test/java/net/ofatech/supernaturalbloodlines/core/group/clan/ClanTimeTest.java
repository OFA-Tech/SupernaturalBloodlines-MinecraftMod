package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClanTimeTest {
    @Test
    void aDayIsTwentyFourHours() {
        assertEquals(86_400_000L, ClanTime.DAY);
        assertEquals(3_600_000L, ClanTime.HOUR);
    }

    @Test
    void noClanClassSpellsADayOutAgain() throws Exception {
        Path at = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        Path root = at.resolve("src/main/java/net/ofatech/supernaturalbloodlines");
        for (Path dir : new Path[] {root.resolve("core/group/clan"), root.resolve("parts/species/group/clan")}) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("ClanTime.java")).toList()) {
                    assertFalse(Files.readString(f).contains("24L * 60 * 60 * 1000"), f.getFileName() + " spells a day out instead of using ClanTime.DAY");
                }
            }
        }
    }
}
