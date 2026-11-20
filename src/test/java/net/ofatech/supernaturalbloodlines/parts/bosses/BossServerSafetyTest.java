package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Plan 17, U6: a boss class that a dedicated server loads must not name a client class. Everything under {@code parts/bosses} except its {@code client}
 * folder (and the GameTests, which only run on a server) is checked for imports of the client packages. The GameTest run is the proof that it works;
 * this is the reason it keeps working.
 */
class BossServerSafetyTest {
    private static final List<String> CLIENT = List.of("net.minecraft.client", "com.mojang.blaze3d", "net.neoforged.neoforge.client", "software.bernie.geckolib.renderer",
            "software.bernie.geckolib.model", "software.bernie.geckolib.cache.texture", "net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT");

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base.resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses");
    }

    static List<String> offenders(Path dir) throws Exception {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = dir.relativize(f).toString().replace('\\', '/');
                if (rel.startsWith("client/")) {
                    continue;
                }
                for (String line : Files.readAllLines(f)) {
                    if (line.startsWith("import ") && CLIENT.stream().anyMatch(line::contains)) {
                        bad.add(rel + ": " + line.trim());
                    }
                }
            }
        }
        return bad;
    }

    @Test
    void noBossClassOutsideTheClientFolderImportsAClientClass() throws Exception {
        List<String> bad = offenders(root());
        assertTrue(bad.isEmpty(), "client classes named where a dedicated server loads them: " + bad);
    }

    @Test
    void theCheckSeesAnImportOfAClientClass(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Files.writeString(tmp.resolve("Bad.java"), "package x;\nimport net.minecraft.client.Minecraft;\nclass Bad {}\n");
        Files.createDirectories(tmp.resolve("client"));
        Files.writeString(tmp.resolve("client/Fine.java"), "package x.client;\nimport net.minecraft.client.Minecraft;\nclass Fine {}\n");
        assertTrue(offenders(tmp).size() == 1, "the check missed the import, or flagged the client folder: " + offenders(tmp));
    }
}
