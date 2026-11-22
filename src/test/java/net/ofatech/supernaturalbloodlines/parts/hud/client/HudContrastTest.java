package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hud.HudFaces;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import org.junit.jupiter.api.Test;

/**
 * Every text colour of the HUD on the kit picture it is drawn on, in all three looks: not on a colour the theme names, but on every pixel inside the picture's
 * border (a repainted kit, a speck or a gradient in the middle of a label fails here).
 */
class HudContrastTest {
    private static final String SPRITES = "/assets/supernaturalbloodlines/textures/gui/sprites/kit/";

    private static BufferedImage image(HudKit k, String sprite) throws Exception {
        try (InputStream in = HudContrastTest.class.getResourceAsStream(SPRITES + k.kit() + "/" + sprite + ".png")) {
            assertNotNull(in, k.kit() + "/" + sprite + ".png is missing");
            return ImageIO.read(in);
        }
    }

    /** The nine-slice borders of a sprite (left, top, right, bottom) from its .mcmeta. */
    private static int[] borders(HudKit k, String sprite) throws Exception {
        try (InputStream in = HudContrastTest.class.getResourceAsStream(SPRITES + k.kit() + "/" + sprite + ".png.mcmeta")) {
            assertNotNull(in, sprite + " has no .mcmeta");
            JsonObject b = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling").getAsJsonObject("border");
            return new int[] {b.get("left").getAsInt(), b.get("top").getAsInt(), b.get("right").getAsInt(), b.get("bottom").getAsInt()};
        }
    }

    /** The pixels of a nine-slice sprite that the stretching middle shows: inside its own borders (at least a pixel in), each colour once. */
    private static List<Integer> middle(HudKit k, String sprite) throws Exception {
        BufferedImage img = image(k, sprite);
        int[] b = borders(k, sprite);
        return inside(img, Math.max(1, b[0]), Math.max(1, b[1]), Math.max(1, b[2]), Math.max(1, b[3]));
    }

    private static List<Integer> inside(BufferedImage img, int border) {
        return inside(img, border, border, border, border);
    }

    /** The pixels of the rectangle that is {@code l, t, r, b} in from the picture's edges, each colour once. */
    private static List<Integer> inside(BufferedImage img, int l, int t, int r, int b) {
        List<Integer> out = new ArrayList<>();
        for (int y = t; y < img.getHeight() - b; y++) {
            for (int x = l; x < img.getWidth() - r; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) > 0xC0 && !out.contains(argb)) {
                    out.add(argb);
                }
            }
        }
        return out;
    }

    private static void assertReads(String what, int text, List<Integer> faces) {
        for (int face : faces) {
            double r = Contrast.ratio(text, face);
            assertTrue(r >= 4.5, what + " #" + Integer.toHexString(text) + " on #" + Integer.toHexString(face) + " is " + r + ":1");
        }
    }

    @Test
    void theColoursOfTheFacesAreTheMiddlesOfTheirPictures() throws Exception {
        for (HudKit k : HudKit.values()) {
            assertEquals(HudFaces.labelFace(k), 0xFF000000 | (image(k, "hud_label").getRGB(8, 8) & 0xFFFFFF), k + " hud_label");
            assertEquals(HudFaces.labelFace(k), 0xFF000000 | (image(k, "plate").getRGB(12, 7) & 0xFFFFFF), k + " plate");
            assertEquals(HudFaces.pillFace(k), 0xFF000000 | (image(k, "hud_pill").getRGB(12, 7) & 0xFFFFFF), k + " hud_pill");
            assertEquals(HudFaces.keyFace(k), 0xFF000000 | (image(k, "key_badge").getRGB(5, 5) & 0xFFFFFF), k + " key_badge");
            assertEquals(HudFaces.keyFace(k), 0xFF000000 | (image(k, "key_badge_wide").getRGB(8, 5) & 0xFFFFFF), k + " key_badge_wide");
        }
    }

    @Test
    void labelTextIsReadableOnEveryPixelOfTheLabelInEveryLook() throws Exception {
        for (HudKit k : HudKit.values()) {
            List<Integer> faces = middle(k, "hud_label");
            assertTrue(!faces.isEmpty(), k + " has a label middle");
            assertReads(k + " name", HudFaces.labelName(k), faces);
            assertReads(k + " body", HudFaces.labelBody(k), faces);
            assertReads(k + " warning", HudFaces.labelWarn(k), faces);
            assertReads(k + " ink", k.ink(), faces);
        }
    }

    @Test
    void thePillTextIsReadableOnEveryPixelOfThePill() throws Exception {
        for (HudKit k : HudKit.values()) {
            // the capitals stand on the dark rows between the pill's lines (the Grimoire's gold ones, the Blood Moon's red top one); the Grimoire's pill is drawn taller for it
            BufferedImage pill = image(k, "hud_pill");
            int[] b = borders(k, "hud_pill");
            assertReads(k + " pill", HudFaces.pillText(k), inside(pill, Math.max(1, b[0]), Math.max(3, b[1]), Math.max(1, b[2]), Math.max(3, b[3])));
        }
    }

    @Test
    void theKeyLettersAreReadableOnTheirBadge() throws Exception {
        for (HudKit k : HudKit.values()) {
            // the letter (5 px wide, 7 tall, from 2 below the top) is drawn on the face of the badge: the pixels the seal's rim does not take
            assertReads(k + " one character", HudFaces.keyLetter(k), inside(image(k, "key_badge"), 3, 3, 3, 3));
            assertReads(k + " two characters", HudFaces.keyLetter(k), inside(image(k, "key_badge_wide"), 3, 3, 3, 3));
        }
    }

    @Test
    void everyMasteryFillIsAtLeast3To1AgainstItsTrack() throws Exception {
        for (HudKit k : HudKit.values()) {
            int track = image(k, "mastery_track").getRGB(9, 1);
            for (String fill : new String[] {"mastery_fill", "mastery_fill_mastered", "mastery_fill_learning"}) {
                int colour = image(k, fill).getRGB(0, 0);
                double r = Contrast.ratio(colour, track);
                assertTrue(r >= 3.0, k + " " + fill + " is " + r + ":1 against its track");
            }
        }
    }

    @Test
    void theHudLooksHoldTheColoursOfTheHubThemesOfTheSameName() {
        HubTheme[] themes = {new GrimoireTheme(), new BloodMoonTheme(), new TensuraHubTheme()};
        HudKit[] kits = {HudKit.GRIMOIRE, HudKit.BLOOD_MOON, HudKit.TENSURA};
        for (int i = 0; i < 3; i++) {
            HubTheme t = themes[i];
            HudKit k = kits[i];
            assertEquals(t.kit(), k.kit());
            assertEquals(t.paper(), k.paper(), k + " paper");
            assertEquals(t.ink(), k.ink(), k + " ink");
            assertEquals(t.muted(), k.muted(), k + " muted");
            assertEquals(t.accent(), k.accent(), k + " accent");
            assertEquals(t.good(), k.good(), k + " good");
            assertEquals(t.focusColor(), k.focusColor(), k + " focus");
        }
    }
}
