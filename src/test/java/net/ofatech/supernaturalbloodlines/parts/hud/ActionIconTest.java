package net.ofatech.supernaturalbloodlines.parts.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/**
 * Our own actions never show the unknown picture (plan 23 Task 7). Every action icon of ours ({@code textures/gui/hud/<name>.png}, named in an action
 * class) is a real file, not the placeholder and not a copy of another action's icon. Two are waiting for art the owner has to approve: they are listed
 * by the file they will use, and the test fails as soon as that file lands so the wiring is not forgotten.
 */
class ActionIconTest {
    private static final Path SRC = SourceTree.root().resolve("src/main/java/net/ofatech/supernaturalbloodlines");
    private static final Path HUD = SourceTree.root().resolve("src/main/resources/assets/supernaturalbloodlines/textures/gui/hud");
    private static final Path SPRITES = SourceTree.root().resolve("src/main/resources/assets/supernaturalbloodlines/textures/gui/sprites/icons");
    private static final Pattern ICON = Pattern.compile("\"textures/gui/hud/([a-z_]+)\\.png\"");
    /** The packages whose classes are our own actions (and the wheel's entries for them). */
    private static final List<String> OURS = List.of("parts/species", "parts/hud/wheel", "parts/artifacts");
    /** The icons in the HUD folder that are not actions. */
    private static final Set<String> NOT_ACTIONS = Set.of("blood_full", "blood_empty", "slot_active", "unknown");
    /** Action class -> the icon it will use once the owner approves the art; until then it shows the placeholder. */
    static final Map<String, String> AWAITING_ART = Map.of("parts/species/blood/GiveBloodAction.java", "give_blood",
            "parts/hud/wheel/VampirismVision.java", "vision");

    private static Map<String, String> iconsByClass() throws IOException {
        Map<String, String> out = new TreeMap<>();
        for (String pkg : OURS) {
            try (Stream<Path> files = Files.walk(SRC.resolve(pkg))) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java") && !p.toString().contains("gametest")).toList()) {
                    Matcher m = ICON.matcher(Files.readString(f));
                    while (m.find()) {
                        out.put(SRC.relativize(f).toString().replace('\\', '/'), m.group(1));
                    }
                }
            }
        }
        return out;
    }

    @Test
    void everyOurActionHasItsOwnTexture() throws IOException {
        byte[] unknown = Files.readAllBytes(HUD.resolve("unknown.png"));
        Map<String, String> icons = iconsByClass();
        assertTrue(icons.size() >= 7, "too few action icons found: " + icons);
        List<String> wrong = new ArrayList<>();
        Map<String, String> byBytes = new TreeMap<>();
        for (var e : icons.entrySet()) {
            String name = e.getValue();
            if (AWAITING_ART.containsKey(e.getKey())) {
                assertEquals("unknown", name, e.getKey() + " has its icon: take it off AWAITING_ART");
                continue;
            }
            Path file = HUD.resolve(name + ".png");
            if (!Files.exists(file)) {
                wrong.add(e.getKey() + ": no " + file.getFileName());
                continue;
            }
            byte[] bytes = Files.readAllBytes(file);
            if (name.equals("unknown") || Arrays.equals(bytes, unknown)) {
                wrong.add(e.getKey() + ": the unknown picture");
            }
            String twin = byBytes.put(Arrays.toString(bytes), name);
            if (twin != null && !twin.equals(name)) {
                wrong.add(name + " is a copy of " + twin);
            }
        }
        Map<String, String> owner = new TreeMap<>();
        for (var e : icons.entrySet()) {
            String other = owner.putIfAbsent(e.getValue(), e.getKey());
            if (other != null && !e.getValue().equals("unknown")) {
                wrong.add(e.getKey() + " uses the icon of " + other + " (" + e.getValue() + ")");
            }
        }
        assertEquals(List.of(), wrong);
    }

    @Test
    void theAwaitingIconsAreStillAwaited() {
        for (String name : AWAITING_ART.values()) {
            assertFalse(Files.exists(HUD.resolve(name + ".png")), name + ".png is in: wire it in its action and take it off AWAITING_ART");
        }
    }

    /** The placeholder is the wheel's and the HUD's fallback for another mod's action; none of our action classes names it, except those waiting for art. */
    @Test
    void theUnknownFallbackIsNeverOursByConstruction() throws IOException {
        List<String> users = new ArrayList<>();
        for (String pkg : OURS) {
            try (Stream<Path> files = Files.walk(SRC.resolve(pkg))) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java") && !p.toString().contains("gametest")).toList()) {
                    String text = Files.readString(f);
                    String rel = SRC.relativize(f).toString().replace('\\', '/');
                    if ((text.contains("ClientIcons.UNKNOWN") || text.contains("gui/hud/unknown.png")) && !AWAITING_ART.containsKey(rel)) {
                        users.add(rel);
                    }
                }
            }
        }
        assertEquals(List.of(), users, "our action classes that show the unknown picture");
    }

    /** Each action icon ships as the HUD's 16 px file and the kit's 16 px and 32 px sprites, the way bite.png does. */
    @Test
    void everyIconIs16And32() throws IOException {
        Set<String> names = new HashSet<>(iconsByClass().values());
        names.removeAll(NOT_ACTIONS);
        names.removeAll(AWAITING_ART.values());
        for (String name : names) {
            assertEquals(16, ImageIO.read(HUD.resolve(name + ".png").toFile()).getWidth(), name + " in gui/hud");
            for (int size : new int[] {16, 32}) {
                Path f = SPRITES.resolve(size + "/" + name + ".png");
                if (Files.exists(f)) {
                    BufferedImage img = ImageIO.read(f.toFile());
                    assertTrue(img.getWidth() == size && img.getHeight() == size, name + " at " + size + " is " + img.getWidth() + "x" + img.getHeight());
                }
            }
        }
        for (String name : List.of("bite", "neck_snap", "rip_heart", "alpha_roar")) {
            assertTrue(Files.exists(SPRITES.resolve("32/" + name + ".png")), "the 32 px " + name);
        }
    }

    @Test
    void theIconsAreNotFullyTransparentOrFlat() throws IOException {
        Set<String> names = new HashSet<>(iconsByClass().values());
        names.removeAll(NOT_ACTIONS);
        for (String name : names) {
            Path f = HUD.resolve(name + ".png");
            if (!Files.exists(f)) {
                continue;
            }
            BufferedImage img = ImageIO.read(f.toFile());
            Set<Integer> colours = new HashSet<>();
            boolean clear = false;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    if (argb >>> 24 == 0) {
                        clear = true;
                    } else {
                        colours.add(argb);
                    }
                }
            }
            assertTrue(clear && colours.size() >= 3, name + ": " + colours.size() + " colours, transparent edge " + clear);
        }
    }
}
