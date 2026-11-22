package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SourceTagTest {
    @Test
    void theStripeIsInsideTheChipsEdgeAndTheLabelStartsAfterIt() {
        int edge = 4;
        assertTrue(SourceTag.STRIPE_X >= edge, "the stripe is not on the picture's left edge");
        assertTrue(SourceTag.STRIPE_Y >= 2 && SourceTag.STRIPE_Y + SourceTag.STRIPE_H <= SourceTag.HEIGHT - 2, "nor on its top or bottom edge");
        assertTrue(SourceTag.TEXT_X > SourceTag.STRIPE_X + SourceTag.STRIPE_W, "the label does not touch the stripe");
        for (SourceTag tag : SourceTag.values()) {
            int label = tag.label().length() * 6;
            assertEquals(label + SourceTag.TEXT_X + 4, SourceTag.width(label), tag + ": room for the stripe, the label and the right edge");
        }
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
