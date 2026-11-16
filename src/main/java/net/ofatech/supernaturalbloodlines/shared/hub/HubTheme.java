package net.ofatech.supernaturalbloodlines.shared.hub;

import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.UiContext;

/**
 * A skin and layout over the one hub screen model: where things go and how they look. The supernatural part's shared page views draw
 * into {@link HubLayout#MAIN} and {@link HubLayout#SIDE} through these calls and never pick colours or textures of their own. It lives in
 * {@code shared} so another part (Tensura's theme) can bring one without importing the supernatural part; a theme whose shape does not
 * fit the shared views also implements that part's {@code HubThemeViews}.
 */
public interface HubTheme {
    HubLayout layout(int width, int height, int tabs, int seals);

    void drawFrame(GuiGraphics g, HubLayout layout);

    void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused);

    /** A launcher (another mod's screen), marked with {@code letter}. */
    void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit);

    /** A launcher in its place; a theme with room for the name draws it too. */
    default void drawLauncher(GuiGraphics g, Rect r, HubHooks.Launcher launcher, boolean lit) {
        drawSeal(g, r, launcher.letter(), launcher.color(), lit);
    }

    /** A rung's mark, {@link #MARK} square. */
    void drawRungMark(GuiGraphics g, int x, int y, RungStatus status);

    /** A task's box, {@link #BOX} square. */
    void drawTaskBox(GuiGraphics g, int x, int y, TaskState state);

    /** The claim or buy button, {@link #STAMP} square. */
    void drawStamp(GuiGraphics g, int x, int y, boolean lit);

    void drawRule(GuiGraphics g, int x, int y, int w);

    void drawBar(GuiGraphics g, Rect r, float fraction);

    /** Shows that {@code region} holds more than fits: {@code offset} of {@code max} scrolled. */
    void drawScrollbar(GuiGraphics g, Rect region, int offset, int max);

    /** The keyboard focus around a target. */
    void drawFocus(GuiGraphics g, Rect r);

    /** The theme switch button. A theme draws its own (the clasp, the moon, the title bar button). */
    default void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.button(g, this, r, Icons.THEME, hot, focused);
    }

    /** The close button, for a theme whose layout has one. */
    default void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.button(g, this, r, Icons.CLOSE, hot, focused);
    }

    /** The detail of a selection in {@code r}: the shared contract of every page, which a theme may style as the page opposite, a bar or a pane. */
    default void drawDetail(UiContext ctx, Rect r, Detail detail) {
        KitDraw.detail(ctx, this, r, detail);
    }

    /** The colour of the keyboard focus ring (a target outline, not text): 3:1 or more on {@link #paper()}. */
    default int focusColor() {
        return FocusRing.forPaper(paper());
    }

    /** The colour behind the content regions: every text colour below is readable on it (4.5:1 or more). */
    int paper();

    int ink();

    int muted();

    int accent();

    int good();

    /** The tag colour of a source mod ({@code species}, {@code vampirism}, {@code werewolves}, {@code enchanted}, {@code tensura}). */
    int sourceColor(String source);

    /** Text the theme draws on something other than {@link #paper()}: {@code fg} on {@code bg}, named for the test that checks it. */
    record TextPair(String what, int fg, int bg) {
    }

    /** Every such pair, so a test can check each is readable (4.5:1 or more); a gradient lists its lightest and darkest end. */
    default java.util.List<TextPair> textPairs() {
        return java.util.List.of();
    }

    int MARK = 9;
    int BOX = 7;
    int STAMP = 14;
}
