package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A skin and layout over the one hub screen model: where things go and how they look. Pages draw their content through it and never
 * pick colours or textures of their own, so a theme can be swapped without touching a page.
 */
public interface HubTheme {
    HubLayout layout(int width, int height, int tabs);

    void drawFrame(GuiGraphics g, HubLayout layout);

    void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused);

    /** A rung's mark, {@link #MARK} square. */
    void drawRungMark(GuiGraphics g, int x, int y, HubModel.RungStatus status);

    /** A task's box, {@link #BOX} square. */
    void drawTaskBox(GuiGraphics g, int x, int y, HubModel.TaskState state);

    /** The claim button, {@link #STAMP} square. */
    void drawStamp(GuiGraphics g, int x, int y, boolean lit);

    void drawRule(GuiGraphics g, int x, int y, int w);

    void drawBar(GuiGraphics g, Rect r, float fraction);

    /** The keyboard focus around a target. */
    void drawFocus(GuiGraphics g, Rect r);

    int ink();

    int muted();

    int accent();

    int good();

    /** The tag colour of a task's source mod ({@code species}, {@code vampirism}, {@code werewolves}, {@code enchanted}, {@code tensura}). */
    int sourceColor(String source);

    int MARK = 9;
    int BOX = 7;
    int STAMP = 14;
}
