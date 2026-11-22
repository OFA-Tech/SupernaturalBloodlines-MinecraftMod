package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import org.junit.jupiter.api.Test;

class KitFacesTest {
    private static final int[] INK = {0xFF3B2414, 0xFFF0E0E0, 0xFFE6F3FB};

    @Test
    void theTableIsTheMiddleOfEachButtonPicture() throws Exception {
        List<String> drifted = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (Part button : KitFaces.BUTTONS) {
                BufferedImage img = UiKitSlicesTest.png(KitSprites.of(kit, button.sprite()));
                int px = img.getRGB(img.getWidth() / 2, img.getHeight() / 2);
                if (px != KitFaces.face(kit, button)) {
                    drifted.add(kit + "/" + button.sprite() + " is #" + Integer.toHexString(px) + " in the picture, #" + Integer.toHexString(KitFaces.face(kit, button)) + " in the table");
                }
            }
        }
        assertTrue(drifted.isEmpty(), drifted.toString());
    }

    @Test
    void everyButtonLabelIsReadableOnItsFace() {
        List<String> bad = new ArrayList<>();
        for (int k = 0; k < KitSprites.KITS.size(); k++) {
            String kit = KitSprites.KITS.get(k);
            for (Part button : KitFaces.BUTTONS) {
                int face = KitFaces.face(kit, button);
                double ratio = Contrast.ratio(KitFaces.label(kit, button, INK[k]), face);
                if (ratio < 4.5) {
                    bad.add(kit + "/" + button.sprite() + " label " + String.format("%.2f", ratio) + ":1");
                }
            }
        }
        assertTrue(bad.isEmpty(), "labels below 4.5:1: " + bad);
    }

    @Test
    void theStateOfAButtonPicksItsPicture() {
        assertEquals(Part.BUTTON_NORMAL, KitFaces.button(false, true, false, false));
        assertEquals(Part.BUTTON_HOVER, KitFaces.button(false, true, true, false));
        assertEquals(Part.BUTTON_PRESSED, KitFaces.button(false, true, true, true));
        assertEquals(Part.BUTTON_DISABLED, KitFaces.button(false, false, true, true));
        assertEquals(Part.BUTTON_PRIMARY_NORMAL, KitFaces.button(true, true, false, false));
        assertEquals(Part.BUTTON_PRIMARY_HOVER, KitFaces.button(true, true, true, false));
        assertEquals(Part.BUTTON_PRIMARY_DISABLED, KitFaces.button(true, false, false, false));
    }
}
