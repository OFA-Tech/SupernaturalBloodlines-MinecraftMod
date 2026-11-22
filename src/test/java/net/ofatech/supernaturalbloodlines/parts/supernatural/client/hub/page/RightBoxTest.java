package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import org.junit.jupiter.api.Test;

class RightBoxTest {
    @Test
    void aButtonAtTheRightEdgeKeepsItsPicturesBorderInsideTheColumn() {
        Rect column = new Rect(100, 40, 200, 300);
        Rect b = PageWidgets.rightBox(column, 60, 36);
        assertTrue(b.right() + Part.BUTTON_NORMAL.margin() <= column.right(), "the border of the picture is clipped: " + b);
        assertEquals(36, b.w());
        assertEquals(60, b.y());
    }
}