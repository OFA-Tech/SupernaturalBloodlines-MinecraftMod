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
    void theSurfaceTableIsTheMiddleOfEachChipPlateAndInsetPicture() throws Exception {
        List<String> drifted = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (Part surface : KitFaces.SURFACES) {
                BufferedImage img = UiKitSlicesTest.png(KitSprites.of(kit, surface.sprite()));
                int px = img.getRGB(img.getWidth() / 2, img.getHeight() / 2);
                if (px != KitFaces.face(kit, surface)) {
                    drifted.add(kit + "/" + surface.sprite() + " is #" + Integer.toHexString(px) + " in the picture, #" + Integer.toHexString(KitFaces.face(kit, surface)) + " in the table");
                }
            }
        }
        assertTrue(drifted.isEmpty(), drifted.toString());
    }

    @Test
    void theInkOfEachThemeReadsOnItsChipsPlatesAndInsets() {
        List<String> bad = new ArrayList<>();
        for (int k = 0; k < KitSprites.KITS.size(); k++) {
            String kit = KitSprites.KITS.get(k);
            for (Part surface : KitFaces.SURFACES) {
                double ratio = Contrast.ratio(KitFaces.label(kit, surface, INK[k]), KitFaces.face(kit, surface));
                if (ratio < 4.5) {
                    bad.add(kit + "/" + surface.sprite() + " " + String.format("%.2f", ratio));
                }
            }
        }
        assertTrue(bad.isEmpty(), "text below 4.5:1: " + bad);
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

    @Test
    void aMissingSpritesFlatColoursAreTheThemesOwnAndTheLabelReadsOnThem() {
        // the Grimoire, Blood Moon and Tensura paper, accent, muted and ink
        int[][] themes = {{0xFFE9DCBC, 0xFF7A1218, 0xFF5E4228, 0xFF3B2414}, {0xFF1A0A10, 0xFFFFD36B, 0xFFC9A0A8, 0xFFF0E0E0}, {0xFF12293F, 0xFF3FD0D6, 0xFF8FB2C4, 0xFFE6F3FB}};
        for (int[] th : themes) {
            for (KitSprites.Part button : KitFaces.BUTTONS) {
                int face = KitFaces.flatFace(button.flat(), th[0], th[1], th[2]);
                double ratio = Contrast.ratio(KitFaces.labelOn(face, th[3]), face);
                assertTrue(ratio >= 4.5, button + " flat on #" + Integer.toHexString(face) + " label " + ratio);
            }
        }
        assertEquals(0xFF7A1218, KitFaces.flatFace(KitSprites.Flat.ACCENT, 0xFFE9DCBC, 0xFF7A1218, 0xFF5E4228));
        assertEquals(0xFFE9DCBC, KitFaces.flatFace(KitSprites.Flat.SURFACE, 0xFFE9DCBC, 0xFF7A1218, 0xFF5E4228));
        // a track is the muted colour at half over the paper: neither of them
        int track = KitFaces.flatFace(KitSprites.Flat.TRACK, 0xFF000000, 0xFF7A1218, 0xFFFFFFFF);
        assertEquals(0xFF808080, track);
    }

    @Test
    void anInkThatDoesNotReadOnAFaceGivesWayToWhiteOrBlack() {
        assertEquals(0xFFFFFFFF, KitFaces.labelOn(0xFF101010, 0xFF202020));
        assertEquals(0xFF000000, KitFaces.labelOn(0xFFF0F0F0, 0xFFE0E0E0));
        assertEquals(0xFF202020, KitFaces.labelOn(0xFFF0F0F0, 0xFF202020));
    }

    @Test
    void theLetterOnAFlatSealReadsOnTheSealsFlatFace() {
        // the Grimoire seal letter (cream) and the Blood Moon one (gold) on the themes' accent colours: the flat seal is the accent
        int[][] sealThemes = {{0xFFE9DCBC, 0xFF7A1218, 0xFF5E4228, 0xFFF0DCB0}, {0xFF1A0A10, 0xFFFFD36B, 0xFFC9A0A8, 0xFFFFD36B}};
        boolean someNeededTheFallback = false;
        for (int[] th : sealThemes) {
            int face = KitFaces.flatFace(KitSprites.Part.SEAL.flat(), th[0], th[1], th[2]);
            int letter = KitFaces.labelOn(face, th[3]);
            assertTrue(Contrast.ratio(letter, face) >= 4.5, "seal letter #" + Integer.toHexString(letter) + " on #" + Integer.toHexString(face));
            someNeededTheFallback |= letter != th[3];
        }
        assertTrue(someNeededTheFallback, "the Blood Moon's gold letter on its gold accent is the case that was invisible");
    }
}
