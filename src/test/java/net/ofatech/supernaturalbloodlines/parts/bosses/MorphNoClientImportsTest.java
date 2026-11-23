package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.core.boss.MorphClipsTest;
import org.junit.jupiter.api.Test;

/**
 * UI overhaul phase 7: the server never loads a client class, and nothing in the world reads the reduce-motion setting. A source scan, no Minecraft. The
 * dedicated-server GameTests catch the first at run time ({@code NoClassDefFoundError}); this catches it before a run, and names the file. It looks at the code
 * (comments taken out) for a client package named anywhere, so a fully qualified use in the middle of a method counts as much as an import.
 */
class MorphNoClientImportsTest {
    private static final String ROOT = "src/main/java/net/ofatech/supernaturalbloodlines/";
    private static final List<String> SERVER_SIDE = List.of("core/boss", "parts/bosses/entity", "parts/bosses/director");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\b|com\\.mojang\\.blaze3d\\b|net\\.neoforged\\.neoforge\\.client\\b|software\\.bernie\\.geckolib\\.(renderer|model)\\b"
            + "|net\\.ofatech\\.supernaturalbloodlines\\.parts\\.bosses\\.client\\b");
    /** The files of the morph, which draw the world: reduce motion is for the interface only (the owner's answer). */
    private static final List<String> MORPH_FILES = List.of("core/boss/MorphRules.java", "core/boss/ManPoseTable.java", "parts/bosses/entity/MorphCues.java",
            "parts/bosses/entity/ShiftCore.java", "parts/bosses/entity/KlausEntity.java", "parts/bosses/entity/DeucalionEntity.java", "parts/bosses/entity/PeterHaleEntity.java",
            "parts/bosses/entity/BeastEntity.java", "parts/bosses/client/ManPose.java", "parts/bosses/client/MorphPlayerModel.java", "parts/bosses/client/HumanoidBossRenderer.java",
            "parts/bosses/client/ShiftRenderer.java");

    /** The source without its comments (line, block and doc), string literals kept: a client name in a string is a client name in code ({@code Class.forName}). */
    static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                int start = i++;
                while (i < n && source.charAt(i) != c) {
                    i += source.charAt(i) == '\\' ? 2 : 1;
                }
                i = Math.min(n, i + 1);
                out.append(source, start, i);
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** The client names that the code of this source uses, in order. */
    static List<String> clientNames(String source) {
        List<String> found = new ArrayList<>();
        Matcher m = CLIENT.matcher(withoutComments(source));
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    /** Every file under the given folders (relative to the source root) that uses a client name, as {@code file: name}. */
    static List<String> offenders(Path base, List<String> folders) throws IOException {
        List<String> found = new ArrayList<>();
        for (String folder : folders) {
            try (Stream<Path> files = Files.walk(base.resolve(ROOT + folder))) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    for (String name : clientNames(Files.readString(file, StandardCharsets.UTF_8))) {
                        found.add(base.relativize(file) + ": " + name);
                    }
                }
            }
        }
        return found;
    }

    @Test
    void theServerSideOfTheBossesUsesNoClientClass() throws IOException {
        assertTrue(offenders(MorphClipsTest.root(), SERVER_SIDE).isEmpty(), "a server-side boss file uses a client class: " + offenders(MorphClipsTest.root(), SERVER_SIDE));
    }

    /** The control: the same scan on the client folder finds plenty, so a scan that finds nothing is not just looking in the wrong place. */
    @Test
    void theScanSeesTheClientFolder() throws IOException {
        assertFalse(offenders(MorphClipsTest.root(), List.of("parts/bosses/client")).isEmpty(), "the scan found no client name in the client folder");
    }

    @Test
    void aFullyQualifiedUseInTheMiddleOfAMethodCounts() {
        assertEquals(List.of("net.minecraft.client"), clientNames("class A { Object o = net.minecraft.client.Minecraft.class; }"));
        assertEquals(List.of("net.minecraft.client"), clientNames("import net.minecraft.client.Minecraft;\nclass A { }"));
        assertEquals(List.of("software.bernie.geckolib.renderer"), clientNames("class A { void m() { var r = new software.bernie.geckolib.renderer.GeoEntityRenderer<>(null, null); } }"));
        assertEquals(List.of("net.minecraft.client"), clientNames("class A { Class<?> c = Class.forName(\"net.minecraft.client.Minecraft\"); }"));
    }

    @Test
    void aClientNameInAComment() {
        assertTrue(clientNames("/** The client's {@link net.minecraft.client.Minecraft} reads this. */\nclass A { // see com.mojang.blaze3d.vertex\n /* net.neoforged.neoforge.client */ int x; }").isEmpty(),
                "a name in a comment is not a use");
        assertTrue(clientNames("class A { String s = \"a // not a comment\"; int y; }").isEmpty());
        assertEquals(List.of("net.minecraft.client"), clientNames("class A { String s = \"//\"; Object o = net.minecraft.client.Minecraft.class; }"), "a // inside a string does not hide the code after it");
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

    /** Entity event 2 (the old hurt event) is not handled on 1.21.1: the shake is a damage event. A call that sends it does nothing. */
    @Test
    void noBossSendsTheDeadHurtEvent() throws IOException {
        Pattern dead = Pattern.compile("broadcastEntityEvent\\([^;]*\\(byte\\)\\s*2\\)");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MorphClipsTest.root().resolve(ROOT + "parts/bosses/entity"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (dead.matcher(withoutComments(Files.readString(file, StandardCharsets.UTF_8))).find()) {
                    found.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(found.isEmpty(), "entity event 2 does nothing on 1.21.1, use broadcastDamageEvent: " + found);
    }
}