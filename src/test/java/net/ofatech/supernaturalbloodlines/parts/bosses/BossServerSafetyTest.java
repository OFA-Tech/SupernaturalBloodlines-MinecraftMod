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

    /** The lines of BossesPart (outside imports) that name a class of the client folder. */
    static List<Integer> clientNamingLines(List<String> lines) {
        List<Integer> at = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (!l.startsWith("import ") && (l.contains("BossRenderers") || l.contains("BossShots"))) {
                at.add(i);
            }
        }
        return at;
    }

    @Test
    void thePartNamesItsClientClassesOnlyInsideTheClientGuard() throws Exception {
        List<String> lines = Files.readAllLines(root().resolve("BossesPart.java"));
        int guard = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("FMLEnvironment.dist == Dist.CLIENT")) {
                guard = i;
            }
        }
        assertTrue(guard >= 0, "the client guard is gone from BossesPart");
        int end = guard + 1;
        while (!lines.get(end).equals("        }")) {
            end++;
        }
        List<Integer> naming = clientNamingLines(lines);
        assertTrue(!naming.isEmpty(), "BossesPart no longer registers the renderers");
        for (int i : naming) {
            assertTrue(i > guard && i < end, "BossesPart names a client class outside the Dist.CLIENT guard, line " + (i + 1) + ": " + lines.get(i).trim());
        }
    }

    @Test
    void noClassOutsideTheClientFolderNamesTheClientFolderExceptThePartThatGuardsIt() throws Exception {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = root().relativize(f).toString().replace('\\', '/');
                if (rel.startsWith("client/") || rel.equals("BossesPart.java") || rel.startsWith("gametest/")) {
                    continue;
                }
                for (String line : Files.readAllLines(f)) {
                    if (line.contains("parts.bosses.client")) {
                        bad.add(rel + ": " + line.trim());
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "a server class names the client folder: " + bad);
    }

    @Test
    void theGeckoLibRendererAndModelAreNamedOnlyUnderClient() throws Exception {
        List<String> users = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = root().relativize(f).toString().replace('\\', '/');
                // the GameTest that proves the renderer cannot load on a server names it in a string, to try to load it
                if (!rel.startsWith("gametest/") && (Files.readString(f).contains("software.bernie.geckolib.renderer") || Files.readString(f).contains("software.bernie.geckolib.model"))) {
                    users.add(rel);
                }
            }
        }
        assertTrue(!users.isEmpty() && users.stream().allMatch(u -> u.startsWith("client/")), "GeckoLib renderer or model used outside client/: " + users);
    }

    @Test
    void theCheckSeesAnImportOfAClientClass(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Files.writeString(tmp.resolve("Bad.java"), "package x;\nimport net.minecraft.client.Minecraft;\nclass Bad {}\n");
        Files.createDirectories(tmp.resolve("client"));
        Files.writeString(tmp.resolve("client/Fine.java"), "package x.client;\nimport net.minecraft.client.Minecraft;\nclass Fine {}\n");
        assertTrue(offenders(tmp).size() == 1, "the check missed the import, or flagged the client folder: " + offenders(tmp));
    }
}
