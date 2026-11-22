package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SourceTagTest {
    /** The picture of a chip is drawn one pixel past the box on every side (its margin): a border of b px in the picture is b - 1 px inside the box. */
    private static final int MARGIN = KitSprites.Part.CHIP.margin();

    @Test
    void theMarkAndTheLabelAreInsideTheBorderOfTheChipPictureOfEveryKit() throws Exception {
        for (String kit : KitSprites.KITS) {
            int[] b = UiKitSlicesTest.border(KitSprites.of(kit, KitSprites.Part.CHIP.sprite()));
            String at = kit + " chip border " + java.util.Arrays.toString(b);
            assertTrue(SourceTag.STRIPE_X >= b[0] - MARGIN, at + ": the mark is on the left end of the chip");
            assertTrue(SourceTag.STRIPE_Y >= b[1] - MARGIN && SourceTag.STRIPE_Y + SourceTag.STRIPE_H <= SourceTag.HEIGHT + MARGIN - b[3], at + ": nor is it on the top or the bottom edge");
            for (SourceTag tag : SourceTag.values()) {
                int label = tag.label().length() * 6;
                int width = SourceTag.width(label);
                assertTrue(SourceTag.TEXT_X + label <= width + MARGIN - b[2], at + ": " + tag + " runs into the right end of the chip");
            }
        }
        assertTrue(SourceTag.TEXT_X > SourceTag.STRIPE_X + SourceTag.STRIPE_W, "the label does not touch the mark");
    }

    @Test
    void theMarkIsASquareDotNotABarThatReadsAsTheLetterIBesideTheLabel() {
        assertTrue(SourceTag.STRIPE_H <= SourceTag.STRIPE_W, "a mark taller than it is wide reads as an I: " + SourceTag.STRIPE_W + "x" + SourceTag.STRIPE_H);
        assertEquals(SourceTag.TEXT_X + 60 + SourceTag.RIGHT_PAD, SourceTag.width(60));
    }

    @Test
    void everyModHasItsTag() {
        assertEquals(SourceTag.VMP, SourceTag.of("vampirism"));
        assertEquals(SourceTag.WW, SourceTag.of("werewolves"));
        assertEquals(SourceTag.ENC, SourceTag.of("enchanted"));
        assertEquals(SourceTag.TEN, SourceTag.of("Tensura"));
        assertEquals(SourceTag.SB, SourceTag.of("species"));
        assertEquals(SourceTag.SB, SourceTag.of(null));
        assertEquals(SourceTag.SB, SourceTag.of("some_other_mod"));
    }

    @Test
    void theLabelsAreTheDesignsAndTheSourcesAreWhatThemesColour() {
        assertEquals("SB VMP WW ENC TEN", String.join(" ", java.util.Arrays.stream(SourceTag.values()).map(SourceTag::label).toList()));
        for (SourceTag t : SourceTag.values()) {
            assertEquals(t, SourceTag.of(t.source()));
        }
    }
}
