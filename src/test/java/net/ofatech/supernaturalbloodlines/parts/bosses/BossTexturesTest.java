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
import net.ofatech.supernaturalbloodlines.core.boss.MorphClipsTest;
import org.junit.jupiter.api.Test;

/**
 * Every body a boss is drawn with has its texture, and a glow layer is only added where there is something to glow (a bug of UI overhaul phase 7: the Beast went
 * through {@code ShiftRenderer}, whose wolf renderer put GeckoLib's {@code AutoGlowingGeoLayer} on every texture; the Beast has no {@code _glowmask}, GeckoLib
 * then registers an emissive texture that is never uploaded, and the layer painted a solid black silhouette over the model from tick 20 on). A source and file
 * scan, no Minecraft.
 */
class BossTexturesTest {
    private static final Path SRC = MorphClipsTest.root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/client");
    private static final Path TEXTURES = MorphClipsTest.root().resolve("src/main/resources/assets/supernaturalbloodlines/textures/entity/boss");
    private static final Pattern SHIFT = Pattern.compile("new ShiftRenderer<>\\(context,\\s*\"([a-z_]+)\",\\s*\"([a-z_]+)\"");

    /** The {model, texture} of every boss drawn by {@code ShiftRenderer}, as {@code BossRenderers} registers them. */
    private static List<String[]> shifters() throws IOException {
        String text = Files.readString(SRC.resolve("BossRenderers.java"), StandardCharsets.UTF_8);
        List<String[]> found = new ArrayList<>();
        Matcher m = SHIFT.matcher(text);
        while (m.find()) {
            found.add(new String[] {m.group(1), m.group(2)});
        }
        return found;
    }

    @Test
    void theFourShiftersAreFound() throws IOException {
        assertEquals(4, shifters().size(), "Klaus, Deucalion, Peter and the Beast: a different count means the scan is looking at the wrong thing");
    }

    @Test
    void everyWolfTextureExists() throws IOException {
        for (String[] s : shifters()) {
            assertTrue(Files.exists(TEXTURES.resolve(s[1] + ".png")), s[0] + ": " + s[1] + ".png is not in textures/entity/boss");
        }
    }

    /** The man's skin of every boss that shifts, as the entity's {@code skin()} names it (Klaus's id, Deucalion's {@code deucalion_human}, Peter's two, the Host's). */
    @Test
    void everyMansSkinExists() {
        for (String skin : List.of("klaus", "deucalion_human", "peter_hale", "peter_hale_burned", "sebastien_valet")) {
            assertTrue(Files.exists(TEXTURES.resolve(skin + ".png")), skin + ".png is not in textures/entity/boss");
        }
    }

    /**
     * A wolf with no glow mask gets no glow layer. Where there is a {@code <texture>_glowmask.png} (or a glowing section in the texture's own .mcmeta) the layer is
     * right; where there is neither, GeckoLib paints black. Either the file is there, or {@code ShiftRenderer} asks the resource manager before it adds the layer.
     */
    @Test
    void aGlowLayerIsOnlyAddedWhereThereIsAMask() throws IOException {
        String renderer = Files.readString(SRC.resolve("ShiftRenderer.java"), StandardCharsets.UTF_8);
        boolean guarded = renderer.contains("getResource(") && renderer.contains("_glowmask");
        List<String> unmasked = new ArrayList<>();
        for (String[] s : shifters()) {
            boolean mask = Files.exists(TEXTURES.resolve(s[1] + "_glowmask.png")) || Files.exists(TEXTURES.resolve(s[1] + ".png.mcmeta"));
            if (!mask) {
                unmasked.add(s[0]);
            }
        }
        assertTrue(unmasked.isEmpty() || guarded, unmasked + " have no glow mask and ShiftRenderer adds the glow layer anyway (it paints them black)");
    }

    /** The control: the three men's wolves do have masks, so the guard must leave their glow in. */
    @Test
    void theThreeWolvesKeepTheirEyes() throws IOException {
        for (String[] s : shifters()) {
            if (!s[0].equals("beast_of_gevaudan")) {
                assertTrue(Files.exists(TEXTURES.resolve(s[1] + "_glowmask.png")), s[0] + " lost its glow mask");
            }
        }
        assertFalse(Files.exists(TEXTURES.resolve("beast_of_gevaudan_geo_glowmask.png")), "the Beast has a mask now: this test and the guard may be simplified");
    }
}
