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

    // The middles of the kit's pictures, as the tests below hold them: the oracle the text colours are checked against.
    private static int keyFace(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFF9A1C22;
            case BLOOD_MOON -> 0xFF10060A;
            case TENSURA -> 0xFF3FD0D6;
        };
    }

    private static int labelFace(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFFE9DDB8;
            case BLOOD_MOON -> 0xFF1D0B13;
            case TENSURA -> 0xFF12293F;
        };
    }

    private static int pillFace(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFF4A1717 : labelFace(k);
    }

    /** The colour of the slot's dark face, in the middle of the picture. */
    private static int slotFace(HudKit k, String slot) throws Exception {
        return 0xFF000000 | (image(k, slot).getRGB(14, 14) & 0xFFFFFF);
    }

    private static boolean opaque(BufferedImage img, int x, int y) {
        return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && (img.getRGB(x, y) >>> 24) >= 64;
    }

    @Test
    void theHudIconsOuterSilhouetteReadsOnTheSlotFaceOfEveryLook() throws Exception {
        for (String name : net.ofatech.supernaturalbloodlines.core.hud.HudIcons.OURS) {
            BufferedImage icon;
            try (InputStream in = HudContrastTest.class.getResourceAsStream("/assets/supernaturalbloodlines/textures/gui/sprites/icons/hud/" + name + ".png")) {
                assertNotNull(in, "icons/hud/" + name + ".png is missing");
                icon = ImageIO.read(in);
            }
            int edge = 0;
            for (int y = 0; y < icon.getHeight(); y++) {
                for (int x = 0; x < icon.getWidth(); x++) {
                    if (!opaque(icon, x, y) || (opaque(icon, x - 1, y) && opaque(icon, x + 1, y) && opaque(icon, x, y - 1) && opaque(icon, x, y + 1))) {
                        continue;
                    }
                    edge++;
                    for (HudKit k : HudKit.values()) {
                        for (String slot : new String[] {"slot", "slot_square"}) {
                            double r = Contrast.ratio(0xFF000000 | (icon.getRGB(x, y) & 0xFFFFFF), slotFace(k, slot));
                            assertTrue(r >= 3.0, name + " (" + x + "," + y + ") on the " + k + " " + slot + " is " + r + ":1");
                        }
                    }
                }
            }
            assertTrue(edge > 10, name + " has an outline to check");
        }
    }

    @Test
    void theCooldownVeilShowsAgainstTheSlotFaceAndTheSecondsStillReadOnIt() throws Exception {
        BufferedImage sweep;
        try (InputStream in = HudContrastTest.class.getResourceAsStream(SPRITES + "shared/cooldown_sweep.png")) {
            sweep = ImageIO.read(in);
        }
        int veil = 0;
        for (int y = 0; y < sweep.getHeight() && veil == 0; y++) {
            for (int x = 0; x < sweep.getWidth(); x++) {
                if ((sweep.getRGB(x, y) >>> 24) > 0) {
                    veil = sweep.getRGB(x, y);
                    break;
                }
            }
        }
        assertTrue(veil != 0, "the strip has a veil");
        int a = veil >>> 24;
        for (HudKit k : HudKit.values()) {
            int face = slotFace(k, "slot");
            int over = 0xFF000000;
            for (int shift : new int[] {16, 8, 0}) {
                int c = ((veil >> shift) & 0xFF) * a / 255 + ((face >> shift) & 0xFF) * (255 - a) / 255;
                over |= c << shift;
            }
            assertTrue(Contrast.ratio(over, face) >= 3.0, k + ": the veil is " + Contrast.ratio(over, face) + ":1 against the slot face");
            assertTrue(Contrast.ratio(0xFFFFFFFF, over) >= 4.5, k + ": the seconds are " + Contrast.ratio(0xFFFFFFFF, over) + ":1 on the veil");
        }
    }

    @Test
    void theColoursOfTheFacesAreTheMiddlesOfTheirPictures() throws Exception {
        for (HudKit k : HudKit.values()) {
            assertEquals(labelFace(k), 0xFF000000 | (image(k, "hud_label").getRGB(8, 8) & 0xFFFFFF), k + " hud_label");
            assertEquals(labelFace(k), 0xFF000000 | (image(k, "plate").getRGB(12, 7) & 0xFFFFFF), k + " plate");
            assertEquals(pillFace(k), 0xFF000000 | (image(k, "hud_pill").getRGB(12, 7) & 0xFFFFFF), k + " hud_pill");
            assertEquals(keyFace(k), 0xFF000000 | (image(k, "key_badge").getRGB(5, 5) & 0xFFFFFF), k + " key_badge");
            assertEquals(keyFace(k), 0xFF000000 | (image(k, "key_badge_wide").getRGB(8, 5) & 0xFFFFFF), k + " key_badge_wide");
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
