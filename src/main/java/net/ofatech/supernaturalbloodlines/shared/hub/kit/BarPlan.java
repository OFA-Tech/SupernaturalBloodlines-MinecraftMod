package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.jetbrains.annotations.Nullable;

/**
 * Where the buttons of a floating bar go, for any width. The title keeps its full width while it can; the primary button comes next; the secondary buttons
 * follow while they fit, and what does not fit moves behind an overflow "More" button. Pure, so a test can hold it to a 320 pixel screen.
 *
 * @param pills    one rectangle per action, in the order given; null for an action that moved behind the overflow button
 * @param overflow the "More" button, or null when everything fits
 * @param title    the room left to the title and its text (from the bar's left padding)
 * @param hidden   the indexes of the actions behind the overflow button
 */
public record BarPlan(List<@Nullable Rect> pills, @Nullable Rect overflow, int title, List<Integer> hidden) {
    public static final int PAD = 6;
    public static final int GAP = 4;
    public static final int PILL_H = 16;
    public static final int MIN_TITLE = 60;
    public static final int MIN_PILL = 40;
    /** Wide enough for the word "More" on it (a bare "..." says nothing to someone who has not met it). */
    public static final int OVERFLOW_W = 34;

    /**
     * @param widths  the width each action's label needs (without padding), in order
     * @param primary the index of the primary action, or -1 for none
     * @param titleW  the full width the title wants
     */
    public static BarPlan of(Rect bar, List<Integer> widths, int primary, int titleW) {
        int n = widths.size();
        int inner = bar.w() - 2 * PAD;
        BarPlan first = place(bar, widths, primary, titleW, inner, false);
        if (first.hidden.isEmpty()) {
            return first;
        }
        return place(bar, widths, primary, titleW, inner, true);
    }

    private static BarPlan place(Rect bar, List<Integer> widths, int primary, int titleW, int inner, boolean overflow) {
        int n = widths.size();
        int reserved = overflow ? OVERFLOW_W + GAP : 0;
        // the title first, but never so wide that not even the primary button fits
        int primaryW = primary < 0 ? 0 : Math.max(MIN_PILL, widths.get(primary) + 14);
        int title = Math.min(titleW, Math.max(MIN_TITLE, inner - reserved - GAP - primaryW));
        int room = inner - reserved - title - GAP;
        boolean[] show = new boolean[n];
        if (primary >= 0) {
            show[primary] = true;
            room -= Math.min(room, primaryW);
        }
        // the secondary buttons from the last to the first, while they fit
        for (int i = n - 1; i >= 0; i--) {
            if (i == primary) {
                continue;
            }
            int w = Math.max(MIN_PILL, widths.get(i) + 14);
            if (room >= w + GAP) {
                show[i] = true;
                room -= w + GAP;
            }
        }
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!show[i]) {
                hidden.add(i);
            }
        }
        List<Rect> pills = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pills.add(null);
        }
        int right = bar.right() - PAD;
        Rect more = null;
        if (!hidden.isEmpty()) {
            more = new Rect(right - OVERFLOW_W, bar.y() + (bar.h() - PILL_H) / 2, OVERFLOW_W, PILL_H);
            right = more.x() - GAP;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (!show[i]) {
                continue;
            }
            int w = Math.max(MIN_PILL, i == primary ? Math.min(primaryW, Math.max(MIN_PILL, inner - reserved - title - GAP)) : widths.get(i) + 14);
            pills.set(i, new Rect(right - w, bar.y() + (bar.h() - PILL_H) / 2, w, PILL_H));
            right -= w + GAP;
        }
        return new BarPlan(pills, more, title, hidden);
    }
}
