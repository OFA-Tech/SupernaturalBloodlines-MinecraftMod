package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import org.junit.jupiter.api.Test;

class ChipLookTest {
    @Test
    void theLitChipIsOnlyTheChosenOne() {
        assertEquals(Part.CHIP_ON, ChipLook.of(true, false).part());
        assertEquals(Part.CHIP, ChipLook.of(false, false).part());
        assertEquals(Part.CHIP, ChipLook.of(false, true).part(), "the pointer over a chip does not light it");
    }

    @Test
    void thePointerOverAnotherChipDrawsAnOutlineAndTheChosenOneNeedsNone() {
        assertTrue(ChipLook.of(false, true).outline());
        assertFalse(ChipLook.of(false, false).outline());
        assertFalse(ChipLook.of(true, true).outline());
    }
}