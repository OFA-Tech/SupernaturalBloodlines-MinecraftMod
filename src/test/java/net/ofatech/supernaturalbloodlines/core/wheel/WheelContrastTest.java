package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import org.junit.jupiter.api.Test;

class WheelContrastTest {
    private static final String SPRITES = "/assets/supernaturalbloodlines/textures/gui/sprites/";

    @Test
    void everyLooksTextReadsAt4_5To1() {
        for (Look look : Look.values()) {
            List<String> failed = new ArrayList<>();
            for (WheelPalette.TextPair p : WheelPalette.of(look).textPairs()) {
                double r = Contrast.ratio(p.fg(), p.bg());
                if (r < 4.5) {
                    failed.add(String.format("%s: #%06x on #%06x is %.2f:1", p.what(), p.fg() & 0xFFFFFF, p.bg() & 0xFFFFFF, r));
                }
            }
            assertTrue(failed.isEmpty(), look + " has text below 4.5:1: " + failed);
        }
    }

    /** A band is translucent: what the player sees is the band over the game, which is dark at night and pale in snow. */
    private static int over(int argb, int sky) {
        int a = argb >>> 24;
        int r = ((argb >> 16 & 255) * a + (sky >> 16 & 255) * (255 - a)) / 255;
        int g = ((argb >> 8 & 255) * a + (sky >> 8 & 255) * (255 - a)) / 255;
        int b = ((argb & 255) * a + (sky & 255) * (255 - a)) / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Test
    void theLinesAndTheRimReadAt3To1OnBothBandsOverADarkAndALightSky() {
        int[] skies = {0xFF05070B, 0xFFEAF2FF};
        for (Look look : Look.values()) {
            WheelPalette p = WheelPalette.of(look);
            for (int sky : skies) {
                // the Grimoire's two bands are a parchment and a dark red: no one colour reads on both, so each line has the colour of the band it is drawn on
                int outer = over(p.outerBand(), sky);
                int inner = over(p.innerBand(), sky);
                for (int[] pair : new int[][] {{p.rim(), outer}, {p.line(), outer}, {p.innerLine(), inner}}) {
                    double r = Contrast.ratio(pair[0] | 0xFF000000, pair[1]);
                    assertTrue(r >= 3.0, String.format("%s line %08x on a band seen as %06x over sky %06x: %.2f", look, pair[0], pair[1] & 0xFFFFFF, sky & 0xFFFFFF, r));
                }
            }
        }
    }

    @Test
    void theLockOnACellReadsAt3To1() {
        for (Look look : Look.values()) {
            WheelPalette p = WheelPalette.of(look);
            assertTrue(Contrast.ratio(p.lockOnCell(), p.cell()) >= 3.0, look + " lock on its cell: " + Contrast.ratio(p.lockOnCell(), p.cell()));
        }
    }
    @Test
    void thePanelColourIsCloseToTheKitsPaper() {
        // read off the mockups (Grimoire E9DDB8 against the kit's E9DCBC): the panel sprite is what is drawn, this is the colour its text is checked on
        for (Look look : Look.values()) {
            HudKit kit = look == Look.GRIMOIRE ? HudKit.GRIMOIRE : look == Look.BLOOD_MOON ? HudKit.BLOOD_MOON : HudKit.TENSURA;
            assertTrue(Contrast.ratio(kit.paper() | 0xFF000000, WheelPalette.of(look).panel() | 0xFF000000) < 1.1, look.name());
        }
    }

    @Test
    void everyNewSpriteNameIsValidAndExists() throws Exception {
        for (Part part : new Part[] {Part.WHEEL_CELL_36, Part.WHEEL_CELL_36_ACTIVE, Part.CELL_GLOW_52, Part.WHEEL_CARET, Part.WHEEL_EDGE}) {
            assertTrue(KitSprites.validName(part.sprite()), part.sprite());
            for (String kit : KitSprites.KITS) {
                try (InputStream in = getClass().getResourceAsStream(SPRITES + KitSprites.of(kit, part.sprite()) + ".png")) {
                    assertNotNull(in, kit + "/" + part.sprite() + ".png is missing");
                }
            }
        }
        for (String shared : new String[] {KitSprites.WHEEL_SWEEP, KitSprites.WHEEL_LOCK_OUTLINE, KitSprites.icon(16, "infinity_stones"), KitSprites.icon(16, "great_sage")}) {
            try (InputStream in = getClass().getResourceAsStream(SPRITES + shared + ".png")) {
                assertNotNull(in, shared + ".png is missing");
            }
        }
    }
}
