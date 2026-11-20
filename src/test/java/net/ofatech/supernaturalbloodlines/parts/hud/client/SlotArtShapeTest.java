package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import org.junit.jupiter.api.Test;

class SlotArtShapeTest {
    @Test
    void tensuraModeIsASquareTileInEveryTheme() {
        for (HudStyle.Style style : HudStyle.Style.values()) {
            assertEquals(Shapes.Shape.SQUARE, SlotArt.shapeOf(style, SlotShape.SQUARE), style + " in Tensura mode");
        }
    }

    @Test
    void supernaturalModeKeepsEachThemesOwnShape() {
        assertEquals(Shapes.Shape.SQUARE, SlotArt.shapeOf(HudStyle.Style.GRIMOIRE, SlotShape.THEMED));
        assertEquals(Shapes.Shape.ROUND, SlotArt.shapeOf(HudStyle.Style.BLOOD_MOON, SlotShape.THEMED));
        assertEquals(Shapes.Shape.HEX, SlotArt.shapeOf(HudStyle.Style.TENSURA, SlotShape.THEMED));
    }
}
