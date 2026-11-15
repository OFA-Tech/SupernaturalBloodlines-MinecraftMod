package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A skin and layout over the one hub screen model: where things go, how they look, and which view draws each page. The shared views
 * ({@link HubPages}) draw into {@link HubLayout#MAIN} and {@link HubLayout#SIDE} through these calls and never pick colours or textures
 * of their own; a theme whose shape does not fit them overrides {@link #view} for the pages it lays out its own way.
 */
public interface HubTheme {
    HubLayout layout(int width, int height, int tabs, int seals);

    /** The view of a page in this theme; the shared one unless the theme brings its own. */
    default HubPageView view(HubPage page) {
        return HubPages.shared(page);
    }

    void drawFrame(GuiGraphics g, HubLayout layout);

    void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused);

    /** A launcher (another mod's screen), marked with {@code letter}. */
    void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit);

    /** A rung's mark, {@link #MARK} square. */
    void drawRungMark(GuiGraphics g, int x, int y, HubModel.RungStatus status);

    /** A task's box, {@link #BOX} square. */
    void drawTaskBox(GuiGraphics g, int x, int y, HubModel.TaskState state);

    /** The claim or buy button, {@link #STAMP} square. */
    void drawStamp(GuiGraphics g, int x, int y, boolean lit);

    void drawRule(GuiGraphics g, int x, int y, int w);

    void drawBar(GuiGraphics g, Rect r, float fraction);

    /** Shows that {@code region} holds more than fits: {@code offset} of {@code max} scrolled. */
    void drawScrollbar(GuiGraphics g, Rect region, int offset, int max);

    /** The keyboard focus around a target. */
    void drawFocus(GuiGraphics g, Rect r);

    int ink();

    int muted();

    int accent();

    int good();

    /** The tag colour of a source mod ({@code species}, {@code vampirism}, {@code werewolves}, {@code enchanted}, {@code tensura}). */
    int sourceColor(String source);

    int MARK = 9;
    int BOX = 7;
    int STAMP = 14;
}
