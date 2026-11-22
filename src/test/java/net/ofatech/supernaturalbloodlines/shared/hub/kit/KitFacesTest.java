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
        for (int[] th : palettes()) {
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
        // the Grimoire seal letter (cream) and the Blood Moon one (gold) on the themes' own accent colours: the flat seal is the accent
        int[][] palettes = palettes();
        int[][] sealThemes = {{palettes[0][0], palettes[0][1], palettes[0][2], 0xFFF0DCB0}, {palettes[1][0], palettes[1][1], palettes[1][2], 0xFFFFD36B}};
        boolean someNeededTheFallback = false;
        for (int[] th : sealThemes) {
            int face = KitFaces.flatFace(KitSprites.Part.SEAL.flat(), th[0], th[1], th[2]);
            int letter = KitFaces.labelOn(face, th[3]);
            assertTrue(Contrast.ratio(letter, face) >= 4.5, "seal letter #" + Integer.toHexString(letter) + " on #" + Integer.toHexString(face));
            someNeededTheFallback |= letter != th[3];
        }
        assertTrue(someNeededTheFallback, "the Blood Moon's gold letter on its gold accent is the case that was invisible");
    }

    @Test
    void textUnderTheWashOfACardDrawnAfterItsContentStillReads() {
        // the wash is the card surface at CARD_OVER_ALPHA over both the text and the paper behind it, in each theme's own colours
        int[][] themes = palettes();
        float a = KitSprites.CARD_OVER_ALPHA;
        List<String> bad = new ArrayList<>();
        for (int k = 0; k < themes.length; k++) {
            int card = KitFaces.face(THEMES[k].kit(), KitSprites.CARD);
            int paper = blend(themes[k][0], card, a);
            for (int text : new int[] {themes[k][3], themes[k][2], themes[k][1]}) {
                double ratio = Contrast.ratio(blend(text, card, a), paper);
                if (ratio < 4.5) {
                    bad.add(THEMES[k].kit() + " text #" + Integer.toHexString(text) + " " + String.format("%.2f", ratio));
                }
            }
        }
        assertTrue(a > 0f && a < 0.5f, "a wash, not a cover");
        assertTrue(bad.isEmpty(), "under the wash: " + bad);
    }

    private static final net.ofatech.supernaturalbloodlines.shared.hub.HubTheme[] THEMES = {new net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme(),
            new net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme(), new net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme()};

    /** Paper, accent, muted and ink of the Grimoire, the Blood Moon and Tensura, read from the themes. */
    private static int[][] palettes() {
        int[][] out = new int[THEMES.length][];
        for (int i = 0; i < THEMES.length; i++) {
            out[i] = new int[] {THEMES[i].paper(), THEMES[i].accent(), THEMES[i].muted(), THEMES[i].ink()};
        }
        return out;
    }

    private static int blend(int under, int over, float a) {
        int r = Math.round(((under >> 16) & 0xFF) * (1 - a) + ((over >> 16) & 0xFF) * a);
        int g = Math.round(((under >> 8) & 0xFF) * (1 - a) + ((over >> 8) & 0xFF) * a);
        int b = Math.round((under & 0xFF) * (1 - a) + (over & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }}
