package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Memo;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache;

/**
 * Everything the side panel and the chips work out from text, worked out once per input and not once per frame: the cut name, the wrapped description, mode and lock lines, the
 * help rows and the width of a label. The key holds the font, the text and every size, so another language, window size or power is another key and never a stale answer
 * ({@link #clear} runs when the screen starts or ends and when resources reload); a frame with the same inputs measures nothing. The render thread only. Pure.
 */
public final class WheelPanelText {
    public static final int LINE = 10;
    /** The lock box is a title line and its reason lines between 4 px of padding; the next block starts 4 px under it. */
    public static final int LOCK_BOX_BASE = 18;
    public static final int LOCK_BOX_GAP = 4;
    public static final int MAX_LINES = 4;

    /** The panel's text, cut and wrapped to fit. {@code description}, {@code mode} and {@code lock} are empty lists when there is nothing (or no room) for them. */
    public record Lines(String name, String where, List<String> cover, String kind, List<String> description, List<String> mode, List<String> lock) {
    }

    /** A help row: the key text, what it does, and where that text starts (after the key and a gap). */
    public record HelpRow(String key, String what, int whatX) {
    }

    private record LinesKey(Object font, int width, int top, int bottom, String name, String where, String cover, String kind, String description, String mode, String lock) {
    }

    private record HelpKey(Object font, int width, List<String> keys, List<String> whats, boolean bold) {
    }

    private record WidthKey(Object font, String text) {
    }

    private static final Memo<LinesKey, Lines> LINES = new Memo<>(256);
    private static final Memo<HelpKey, List<HelpRow>> HELP = new Memo<>(16);
    private static final Memo<WidthKey, Integer> WIDTHS = new Memo<>(1024);

    private WheelPanelText() {
    }

    /**
     * @param top       y where the description would start
     * @param bottom    y of the first row that must stay free (the slot row)
     * @param modeLines how many lines the mode text takes (0 for a power without modes)
     * @param lockLines how many lines of reason the lock box holds (0: no lock box)
     */
    public static int descriptionLines(int top, int bottom, int modeLines, int lockLines) {
        int free = bottom - top - modeLines * LINE - (lockLines > 0 ? lockBoxHeight(lockLines) + LOCK_BOX_GAP : 0) - 2;
        return Math.max(0, Math.min(MAX_LINES, free / LINE));
    }

    /** How tall the lock box is for {@code reasonLines} lines of reason: the box holds exactly what it says, so a one-line reason leaves a line more for the description. */
    public static int lockBoxHeight(int reasonLines) {
        return LOCK_BOX_BASE + LINE * Math.max(1, reasonLines);
    }

    /** {@code mode} and {@code lock} are empty strings when the power has no modes or is not locked. */
    public static Lines lines(Object font, ToIntFunction<String> measure, int width, int top, int bottom, String name, String where, String cover, String kind, String description,
            String mode, String lock) {
        return LINES.get(new LinesKey(font, width, top, bottom, name, where, cover, kind, description, mode, lock), () -> {
            List<String> coverLines = cover.isEmpty() ? List.of() : TextCache.wrap(font, cover, width, 2, measure);
            List<String> modeLines = mode.isEmpty() ? List.of() : TextCache.wrap(font, mode, width, 2, measure);
            List<String> lockLines = lock.isEmpty() ? List.of() : TextCache.wrap(font, lock, width - 22, 2, measure);
            int room = descriptionLines(top + coverLines.size() * LINE, bottom, modeLines.size(), lockLines.size());
            List<String> desc = description.isEmpty() || room == 0 ? List.of() : TextCache.wrap(font, description, width, room, measure);
            return new Lines(TextCache.cut(font, name, width, measure), TextCache.cut(font, where, width, measure), coverLines, TextCache.cut(font, kind, width, measure), desc, modeLines,
                    lockLines);
        });
    }

    /**
     * The help rows: each key text and its words, with where the words start. {@code keyWidth} measures the key text as it is drawn (bold), {@code whatWidth} the words;
     * the words are cut to what is left of {@code width} after the key, so a row never runs out of the panel.
     */
    public static List<HelpRow> help(Object font, ToIntFunction<String> keyWidth, ToIntFunction<String> whatWidth, int width, List<String> keys, List<String> whats) {
        return HELP.get(new HelpKey(font, width, List.copyOf(keys), List.copyOf(whats), true), () -> {
            java.util.ArrayList<HelpRow> rows = new java.util.ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                int whatX = keyWidth.applyAsInt(keys.get(i)) + 5;
                rows.add(new HelpRow(keys.get(i), TextCache.cut(font, whats.get(i), Math.max(0, width - whatX), whatWidth), whatX));
            }
            return List.copyOf(rows);
        });
    }

    /** The fewest help rows the panel has: click, right click, scroll or Alt, and the closing row or one extra. */
    public static final int MIN_HELP_ROWS = 4;

    /**
     * How many of the {@code wanted} help rows fit. The slot, mastery and help rows are drawn from the bottom of the panel, so every extra help row takes a line from what is
     * above them: the mode lines and the lock box (the description gives way first, to nothing). Rows that do not fit are dropped from the end, the lowest priority; the
     * standard four always stay, as they always fitted.
     *
     * @param panelBottom y of the panel's last text row
     * @param contentTop  y where the description would start, under the cover lines
     */
    public static int helpRows(int wanted, int panelBottom, int contentTop, int modeLines, int lockLines) {
        int contentEnd = contentTop + modeLines * LINE + (lockLines > 0 ? lockBoxHeight(lockLines) + LOCK_BOX_GAP : 0);
        int fit = (panelBottom - contentEnd) / LINE - 2;
        return Math.max(Math.min(wanted, MIN_HELP_ROWS), Math.min(wanted, fit));
    }

    /** The width of a label as the font draws it, measured once. */
    public static int width(Object font, String text, ToIntFunction<String> measure) {
        return WIDTHS.get(new WidthKey(font, text), () -> measure.applyAsInt(text));
    }

    public static void clear() {
        LINES.clear();
        HELP.clear();
        WIDTHS.clear();
        TextCache.clear();
    }

    /** How many answers were worked out (not reused) since the start: what a test counts. */
    public static int misses() {
        return LINES.misses() + HELP.misses() + WIDTHS.misses() + TextCache.misses();
    }
}
