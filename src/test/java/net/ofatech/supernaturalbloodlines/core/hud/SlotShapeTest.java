package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlotShapeTest {
    @Test
    void tensuraModeIsSquareAndSupernaturalModeKeepsTheThemesShape() {
        assertEquals(SlotShape.SQUARE, SlotShape.of(SlotMode.TENSURA));
        assertEquals(SlotShape.THEMED, SlotShape.of(SlotMode.SUPERNATURAL));
    }

    @Test
    void onlyTensuraModeIsMarkedSoItReadsWithoutThePill() {
        org.junit.jupiter.api.Assertions.assertTrue(SlotShape.of(SlotMode.TENSURA).marked());
        org.junit.jupiter.api.Assertions.assertFalse(SlotShape.of(SlotMode.SUPERNATURAL).marked());
    }
}
