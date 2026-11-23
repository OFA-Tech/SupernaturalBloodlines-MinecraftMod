package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.core.boss.MorphClipsTest;
import org.junit.jupiter.api.Test;

/**
 * UI overhaul phase 7: the server never loads a client class, and nothing in the world reads the reduce-motion setting. A source scan, no Minecraft. The
 * dedicated-server GameTests catch the first at run time ({@code NoClassDefFoundError}); this catches it before a run, and names the file.
 */
class MorphNoClientImportsTest {
    private static final String ROOT = "src/main/java/net/ofatech/supernaturalbloodlines/";
    private static final List<String> SERVER_SIDE = List.of("core/boss", "parts/bosses/entity", "parts/bosses/director");
    private static final List<String> CLIENT_ONLY = List.of("net.minecraft.client", "com.mojang.blaze3d", "net.neoforged.neoforge.client", "software.bernie.geckolib.renderer",
            "software.bernie.geckolib.model");
    /** The files of the morph, which draw the world: reduce motion is for the interface only (the owner's answer). */
    private static final List<String> MORPH_FILES = List.of("core/boss/MorphRules.java", "core/boss/ManPoseTable.java", "parts/bosses/entity/MorphCues.java",
            "parts/bosses/entity/ShiftCore.java", "parts/bosses/entity/KlausEntity.java", "parts/bosses/entity/DeucalionEntity.java", "parts/bosses/entity/PeterHaleEntity.java",
            "parts/bosses/entity/BeastEntity.java", "parts/bosses/client/ManPose.java", "parts/bosses/client/MorphPlayerModel.java", "parts/bosses/client/HumanoidBossRenderer.java",
            "parts/bosses/client/ShiftRenderer.java");

    /** Every file under the given folders (relative to the source root) that imports one of the packages, as {@code file: import}. */
    static List<String> offenders(Path base, List<String> folders, List<String> packages) throws IOException {
        List<String> found = new ArrayList<>();
        for (String folder : folders) {
            try (Stream<Path> files = Files.walk(base.resolve(ROOT + folder))) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        String t = line.trim();
                        if (t.startsWith("import ") && packages.stream().anyMatch(p -> t.startsWith("import " + p) || t.startsWith("import static " + p))) {
                            found.add(base.relativize(file) + ": " + t);
                        }
                    }
                }
            }
        }
        return found;
    }

    @Test
    void theServerSideOfTheBossesImportsNoClientClass() throws IOException {
        assertTrue(offenders(MorphClipsTest.root(), SERVER_SIDE, CLIENT_ONLY).isEmpty(), "a server-side boss file imports a client class: " + offenders(MorphClipsTest.root(), SERVER_SIDE, CLIENT_ONLY));
    }

    /** The control: the same scan on the client folder finds plenty, so a scan that finds nothing is not just looking in the wrong place. */
    @Test
    void theScanSeesTheClientFolder() throws IOException {
        assertFalse(offenders(MorphClipsTest.root(), List.of("parts/bosses/client"), CLIENT_ONLY).isEmpty(), "the scan found no client import in the client folder");
    }

    @Test
    void noMorphFileReadsReduceMotion() throws IOException {
        Path base = MorphClipsTest.root();
        List<String> found = new ArrayList<>();
        for (String file : MORPH_FILES) {
            Path path = base.resolve(ROOT + file);
            assertTrue(Files.exists(path), file + " is not there: the list is out of date");
            if (Files.readString(path, StandardCharsets.UTF_8).contains("ReduceMotion")) {
                found.add(file);
            }
        }
        assertTrue(found.isEmpty(), "a file of the morph reads ReduceMotion (it is for the interface only): " + found);
    }
}
