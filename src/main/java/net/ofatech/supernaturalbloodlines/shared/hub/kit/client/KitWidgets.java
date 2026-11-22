package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitMetrics;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;

/** The small widgets every theme draws the same way from its own kit: the rule, the bar, the scroll bar, the rung mark, the task box and the stamp. Client only. */
public final class KitWidgets {
    private static final int RUNG_PX = 12;
    private static final int TASK_PX = 10;
    private static final int STAMP_PX = 16;

    private KitWidgets() {
    }

    /** A rule: the divider picture centred on {@code y}. */
    public static void rule(GuiGraphics g, HubTheme theme, int x, int y, int w) {
        UiKit.draw(g, theme, Part.DIVIDER, x, y - KitMetrics.DIVIDER_H / 2, w, KitMetrics.DIVIDER_H);
    }

    /** A progress bar: the track picture centred on {@code r} (it is 8 px tall; the box may be less) and the fill inside its edges. */
    public static void bar(GuiGraphics g, HubTheme theme, Rect r, float fraction) {
        if (!KitMetrics.barFitsPicture(r.h())) {
            g.fill(r.x(), r.y(), r.right(), r.bottom(), theme.muted() & 0xFFFFFF | 0x80000000);
            g.fill(r.x(), r.y(), r.x() + KitMetrics.barFill(r.w(), fraction), r.bottom(), theme.accent());
            return;
        }
        int cy = r.y() + r.h() / 2;
        UiKit.draw(g, theme, Part.BAR_TRACK, r.x(), cy - KitMetrics.BAR_H / 2, r.w(), KitMetrics.BAR_H);
        int inset = KitMetrics.barInset(theme.kit());
        int fill = KitMetrics.barFill(r.w() - 2 * inset, fraction);
        if (fill > 0) {
            UiKit.draw(g, theme, Part.BAR_FILL, r.x() + inset, cy - KitMetrics.BAR_FILL_H / 2, fill, KitMetrics.BAR_FILL_H);
        }
    }

    /** The scroll bar of a region that holds more than fits: just outside its right edge. */
    public static void scrollbar(GuiGraphics g, HubTheme theme, Rect region, int offset, int max) {
        int x = region.right() + 1;
        UiKit.draw(g, theme, Part.SCROLL_TRACK, x, region.y(), KitMetrics.SCROLL_W, region.h());
        KitMetrics.Thumb t = KitMetrics.thumb(region.h(), offset, max);
        UiKit.draw(g, theme, Part.SCROLL_THUMB, x, region.y() + t.top(), KitMetrics.SCROLL_W, t.length());
    }

    /** A rung's mark, centred on the {@link HubTheme#MARK} square at ({@code x}, {@code y}). */
    public static void rungMark(GuiGraphics g, HubTheme theme, int x, int y, RungStatus status) {
        UiKit.draw(g, theme, KitMetrics.rung(status), x + (HubTheme.MARK - RUNG_PX) / 2, y + (HubTheme.MARK - RUNG_PX) / 2, RUNG_PX, RUNG_PX);
    }

    /** A task's box, centred on the {@code box} square at ({@code x}, {@code y}). */
    public static void taskBox(GuiGraphics g, HubTheme theme, int x, int y, int box, TaskState state) {
        UiKit.draw(g, theme, KitMetrics.task(state), x + (box - TASK_PX) / 2, y + (box - TASK_PX) / 2, TASK_PX, TASK_PX);
    }

    public static void stamp(GuiGraphics g, HubTheme theme, int x, int y, boolean lit) {
        UiKit.draw(g, theme, lit ? Part.STAMP_LIT : Part.STAMP, x + (HubTheme.STAMP - STAMP_PX) / 2, y + (HubTheme.STAMP - STAMP_PX) / 2, STAMP_PX, STAMP_PX);
    }
}
