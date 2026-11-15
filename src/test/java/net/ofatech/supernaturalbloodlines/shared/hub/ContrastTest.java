package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ContrastTest {
    @Test
    void theRatioIsWcags() {
        assertEquals(21, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01);
        assertEquals(1, Contrast.ratio(0xFF777777, 0xFF777777), 0.01);
        assertEquals(Contrast.ratio(0xFF102030, 0xFFe0d0c0), Contrast.ratio(0xFFe0d0c0, 0xFF102030), 1e-9);
    }

    @Test
    void theCheckCatchesALowContrastPair() {
        HubTheme grey = new FakeTheme(0xFF808080);
        assertThrows(AssertionError.class, () -> ThemeContrast.assertReadable("grey on grey", grey));
    }

    /** Every colour the same: the check must fail it. */
    private record FakeTheme(int c) implements HubTheme {
        public HubLayout layout(int w, int h, int t, int s) { throw new UnsupportedOperationException(); }
        public void drawFrame(net.minecraft.client.gui.GuiGraphics g, HubLayout l) { }
        public void drawTab(net.minecraft.client.gui.GuiGraphics g, Rect r, HubPage p, boolean s, boolean f) { }
        public void drawSeal(net.minecraft.client.gui.GuiGraphics g, Rect r, String l, int c, boolean lit) { }
        public void drawRungMark(net.minecraft.client.gui.GuiGraphics g, int x, int y, RungStatus s) { }
        public void drawTaskBox(net.minecraft.client.gui.GuiGraphics g, int x, int y, TaskState s) { }
        public void drawStamp(net.minecraft.client.gui.GuiGraphics g, int x, int y, boolean lit) { }
        public void drawRule(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w) { }
        public void drawBar(net.minecraft.client.gui.GuiGraphics g, Rect r, float f) { }
        public void drawScrollbar(net.minecraft.client.gui.GuiGraphics g, Rect r, int o, int m) { }
        public void drawFocus(net.minecraft.client.gui.GuiGraphics g, Rect r) { }
        public int paper() { return c; }
        public int ink() { return c; }
        public int muted() { return c; }
        public int accent() { return c; }
        public int good() { return c; }
        public int sourceColor(String s) { return c; }
    }
}
