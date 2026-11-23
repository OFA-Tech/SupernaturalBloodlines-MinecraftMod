package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import org.junit.jupiter.api.Test;

/** Every text colour of a boss bar on every pixel of the kit picture it stands on, in all three looks, and the HUD's looks held equal to the hub's themes. */
class BossFacesTest {
    private static final String SPRITES = "/assets/supernaturalbloodlines/textures/gui/sprites/kit/";

    private static BufferedImage image(HudKit k, Part part) throws Exception {
        try (InputStream in = BossFacesTest.class.getResourceAsStream(SPRITES + k.kit() + "/" + part.name().toLowerCase() + ".png")) {
            assertNotNull(in, k.kit() + "/" + part + " is missing");
            return ImageIO.read(in);
        }
    }

    /** The borders of a nine-slice sprite; 0 for each side of one that has none (a text box is read from one pixel in). */
    private static int[] borders(HudKit k, Part part) throws Exception {
        try (InputStream in = BossFacesTest.class.getResourceAsStream(SPRITES + k.kit() + "/" + part.name().toLowerCase() + ".png.mcmeta")) {
            assertNotNull(in, part + " has no .mcmeta");
            JsonObject b = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling").getAsJsonObject("border");
            return new int[] {b.get("left").getAsInt(), b.get("top").getAsInt(), b.get("right").getAsInt(), b.get("bottom").getAsInt()};
        }
    }

    /** The opaque pixels of the picture in the rectangle rows {@code y0} to {@code y1} (exclusive), columns {@code x0} to {@code x1} (exclusive), each colour once. */
    private static List<Integer> region(HudKit k, Part part, int x0, int y0, int x1, int y1) throws Exception {
        BufferedImage img = image(k, part);
        List<Integer> out = new ArrayList<>();
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) > 0xC0 && !out.contains(argb)) {
                    out.add(argb);
                }
            }
        }
        return out;
    }

    /** Where the name stands on its plate: from 8 px in, on the rows the capitals cover. */
    private static List<Integer> nameBox(HudKit k) throws Exception {
        BufferedImage img = image(k, Part.BOSS_NAME_PLATE);
        return region(k, Part.BOSS_NAME_PLATE, BossBarLayout.NAME_TEXT_X, BossBarLayout.NAME_TEXT_Y + BossBarLayout.CAP_TOP, img.getWidth() - BossBarLayout.NAME_TEXT_X,
                BossBarLayout.NAME_TEXT_Y + BossBarLayout.DESCENDER_BOTTOM + 1);
    }

    /** Where a chip's word stands: between the 3 px of padding, on the rows the capitals cover (the picture has a pixel of margin round the body). */
    private static List<Integer> chipBox(HudKit k, Part chip) throws Exception {
        BufferedImage img = image(k, chip);
        int pad = BossBarLayout.CHIP_PAD / 2;
        return region(k, chip, 1 + pad, 1 + BossBarLayout.CHIP_TEXT_Y + BossBarLayout.CAP_TOP, img.getWidth() - 1 - pad, 1 + BossBarLayout.CHIP_TEXT_Y + BossBarLayout.CAP_BOTTOM + 1);
    }

    /**
     * What the health text stands on: the stretched middle of the card's picture, and the rows of its bottom border that the text's rows reach (card rows
     * {@code hpY} to {@code hpY + 6} against a card {@code CARD_H} high whose bottom border is drawn 1:1), in the middle columns, clear of the corner ornaments.
     */
    private static List<Integer> cardInside(HudKit k) throws Exception {
        BufferedImage img = image(k, Part.BOSS_FRAME);
        int[] b = borders(k, Part.BOSS_FRAME);
        int x0 = b[0];
        int x1 = img.getWidth() - b[2];
        List<Integer> out = new ArrayList<>(region(k, Part.BOSS_FRAME, x0, b[1], x1, img.getHeight() - b[3]));
        int hpY = BossBarLayout.geometry(BossBarLayout.CARD_W, new int[0], 0).hpY();
        int borderTop = BossBarLayout.CARD_H - b[3];
        for (int row = hpY + BossBarLayout.CAP_TOP; row <= hpY + BossBarLayout.CAP_BOTTOM; row++) {
            if (row >= borderTop) {
                int spriteRow = img.getHeight() - b[3] + (row - borderTop);
                for (int c : region(k, Part.BOSS_FRAME, x0, spriteRow, x1, spriteRow + 1)) {
                    if (!out.contains(c)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    private static void assertReads(String what, int text, List<Integer> faces) {
        assertTrue(!faces.isEmpty(), what + ": no pixels to read on");
        for (int face : faces) {
            double r = Contrast.ratio(text, face);
            assertTrue(r >= 4.5, what + " #" + Integer.toHexString(text) + " on #" + Integer.toHexString(face) + " is " + String.format("%.2f", r) + ":1");
        }
    }

    @Test
    void theNameReadsOnItsPlateAndTheHealthOnTheCardInEveryLook() throws Exception {
        for (HudKit k : HudKit.values()) {
            assertReads(k + " name", BossFaces.name(k), nameBox(k));
            assertReads(k + " health", BossFaces.health(k), cardInside(k));
        }
    }

    @Test
    void theWordOnEachChipReadsOnTheChipInEveryLook() throws Exception {
        for (HudKit k : HudKit.values()) {
            for (Part chip : new Part[] {Part.CHIP, Part.CHIP_ON, Part.CHIP_SHIFTING}) {
                assertReads(k + " " + chip, BossFaces.chip(k, chip), chipBox(k, chip));
            }
        }
    }

    @Test
    void theCardsInkAndTheNameColourAreTheThemesOwn() {
        HubTheme[] themes = {new GrimoireTheme(), new BloodMoonTheme(), new TensuraHubTheme()};
        HudKit[] kits = {HudKit.GRIMOIRE, HudKit.BLOOD_MOON, HudKit.TENSURA};
        for (int i = 0; i < 3; i++) {
            assertEquals(themes[i].muted(), BossFaces.health(kits[i]), kits[i].toString());
            assertEquals(themes[i].kit(), kits[i].kit());
            assertEquals(themes[i].ink(), kits[i].ink());
        }
        assertEquals(HudKit.GRIMOIRE.ink() == BossFaces.name(HudKit.GRIMOIRE), false, "the Grimoire's ink is dark brown and would not read on its dark red plate");
    }
}
