package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import org.junit.jupiter.api.Test;

class SlotArtShapeTest {
    @Test
    void tensuraModeIsASquareTileInEveryTheme() {
        // the frame picture is chosen by the mode only: each theme's kit has its own square tile
        assertEquals(Part.SLOT_SQUARE, SlotArt.framePart(SlotShape.SQUARE, false));
        assertEquals(Part.SLOT_SQUARE_ACTIVE, SlotArt.framePart(SlotShape.SQUARE, true));
    }

    @Test
    void supernaturalModeKeepsEachThemesOwnShape() {
        assertEquals(Part.SLOT, SlotArt.framePart(SlotShape.THEMED, false));
        assertEquals(Part.SLOT_ACTIVE, SlotArt.framePart(SlotShape.THEMED, true));
    }

    @Test
    void eachMasteryStateHasItsOwnFill() {
        assertEquals(Part.MASTERY_FILL, SlotArt.fillPart(Mastery.State.PROGRESS));
        assertEquals(Part.MASTERY_FILL_MASTERED, SlotArt.fillPart(Mastery.State.MASTERED));
        assertEquals(Part.MASTERY_FILL_LEARNING, SlotArt.fillPart(Mastery.State.LEARNING));
    }
}