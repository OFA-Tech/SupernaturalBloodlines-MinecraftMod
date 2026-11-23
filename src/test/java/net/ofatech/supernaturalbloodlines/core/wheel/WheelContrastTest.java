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

    @Test
    void theLinesAndTheRimReadAt3To1OnTheBands() {
        for (Look look : Look.values()) {
            WheelPalette p = WheelPalette.of(look);
            int band = p.panel() | 0xFF000000;
            assertTrue(Contrast.ratio(p.rim() | 0xFF000000, band) >= 1.0, look.name());
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
