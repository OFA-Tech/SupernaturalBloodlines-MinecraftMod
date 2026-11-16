package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModelDragTest {
    @Test
    void itFacesTheFrontUntilDraggedAndStaysWhereItIsLeft() {
        ModelDrag d = new ModelDrag();
        assertEquals(50f, d.lookX(50f));
        d.drag(30);
        assertEquals(20f, d.lookX(50f), "dragging right turns the model right: the point it looks at moves left");
        d.drag(-10);
        assertEquals(30f, d.lookX(50f));
    }

    @Test
    void theTurnIsClampedAndCanBeReset() {
        ModelDrag d = new ModelDrag();
        d.drag(9999);
        assertEquals(ModelDrag.MAX_OFFSET, d.offset());
        d.drag(-99999);
        assertEquals(-ModelDrag.MAX_OFFSET, d.offset());
        d.reset();
        assertEquals(0f, d.offset());
    }
}
