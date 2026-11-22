package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Pure rules for putting a label in a fixed width: wrap it to a few lines at spaces, and as a last resort cut the last line with an ellipsis. Every theme
 * draws its ladder, radial, card and title text through this, so a label never runs into its neighbour and never ends mid-word without a mark. The
 * width function is the font's (scaled for small text), so tests can use a fixed-width stand-in.
 */
public final class LabelFit {
    private static final String ELLIPSIS = "\u2026";

    private LabelFit() {
    }

    /** {@code text} in at most {@code maxLines} lines, each at most {@code width} wide; the last line is cut with an ellipsis when the text does not fit. */
    public static List<String> wrap(String text, int width, int maxLines, ToIntFunction<String> measure) {
        List<String> lines = new ArrayList<>();
        String rest = text.strip();
        while (!rest.isEmpty() && lines.size() < maxLines) {
            boolean last = lines.size() == maxLines - 1;
            if (measure.applyAsInt(rest) <= width) {
                lines.add(rest);
                rest = "";
                break;
            }
            if (last) {
                lines.add(cut(rest, width, measure));
                rest = "";
                break;
            }
            int end = rest.length();
            while (end > 0 && measure.applyAsInt(rest.substring(0, end)) > width) {
                end--;
            }
            int space = end >= rest.length() ? -1 : rest.lastIndexOf(' ', end);
            if (space <= 0) {
                // one word longer than the line: it is cut with an ellipsis, never broken in the middle with no mark
                lines.add(cut(rest, width, measure));
                rest = "";
                break;
            }
            lines.add(rest.substring(0, space).strip());
            rest = rest.substring(space).strip();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /** Whether some word of {@code text} (split at spaces) is wider than {@code width}: it cannot be wrapped, only cut. */
    public static boolean hasOverlongWord(String text, int width, ToIntFunction<String> measure) {
        for (String word : text.split("[ \n]")) {
            if (!word.isEmpty() && measure.applyAsInt(word) > width) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code text} wrapped at spaces (and at a newline) into as many lines as it takes, each at most {@code width} wide. A line is never broken in the middle of a
     * word: only a single word wider than {@code width} is cut, and it ends in an ellipsis.
     */
    public static List<String> wrapWords(String text, int width, ToIntFunction<String> measure) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String raw : paragraph.split(" ")) {
                if (raw.isEmpty()) {
                    continue;
                }
                String word = cut(raw, width, measure);
                if (line.length() == 0) {
                    line.append(word);
                } else if (measure.applyAsInt(line + " " + word) <= width) {
                    line.append(' ').append(word);
                } else {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Whether the lines {@link #wrap} gave end in the ellipsis: the name was cut, and the full one belongs in a tooltip. */
    public static boolean isCut(List<String> lines) {
        return !lines.isEmpty() && lines.get(lines.size() - 1).endsWith(ELLIPSIS);
    }

    /** {@code text} cut to {@code width} with an ellipsis, or whole when it fits. */
    public static String cut(String text, int width, ToIntFunction<String> measure) {
        if (measure.applyAsInt(text) <= width) {
            return text;
        }
        int room = width - measure.applyAsInt(ELLIPSIS);
        // the longest prefix that fits, by bisection: a prefix is never narrower than a shorter one, so a long text takes log n measures, not n
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (measure.applyAsInt(text.substring(0, mid)) <= room) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo == 0 ? (room >= 0 ? ELLIPSIS : "") : text.substring(0, lo).stripTrailing() + ELLIPSIS;
    }

    /** The width a ladder label may use: the gap between two rungs, less a margin, never more than {@code max}. */
    public static int ladderWidth(int step, int max) {
        return Math.max(24, Math.min(max, step - 8));
    }

    /** True when a ladder label's longest word does not fit the gap between rungs: the labels then alternate between two rows and get twice the width. */
    public static boolean needsStagger(List<String> labels, int step, ToIntFunction<String> measure) {
        int room = ladderWidth(step, 90);
        for (String label : labels) {
            for (String word : label.split(" ")) {
                if (measure.applyAsInt(word) > room) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The width of a radial label: the chord between two neighbouring nodes on the ring, less a margin, within {@code min} and {@code max}. */
    public static int radialWidth(int radius, int count, int min, int max) {
        int chord = (int) (2 * radius * Math.sin(Math.PI / Math.max(2, count)));
        return Math.max(min, Math.min(max, chord - 4));
    }

    /** One line of text in a title strip, measured so that the title and the stats beside it never overlap; see {@link #header}. */
    public record Header(String title, String stats) {
    }

    /**
     * What the title strip shows: the title candidates from the longest to the shortest, the stats candidates from the fullest to the barest; the first
     * pair that fits {@code available} (with {@code gap} between them) wins, and the title is shortened before the stats lose parts.
     */
    public static Header header(List<String> titles, List<String> stats, int available, int gap, ToIntFunction<String> titleWidth, ToIntFunction<String> statsWidth) {
        for (String t : titles) {
            if (titleWidth.applyAsInt(t) + gap + statsWidth.applyAsInt(stats.get(0)) <= available) {
                return new Header(t, stats.get(0));
            }
        }
        String shortest = titles.get(titles.size() - 1);
        for (String s : stats) {
            if (titleWidth.applyAsInt(shortest) + gap + statsWidth.applyAsInt(s) <= available) {
                return new Header(shortest, s);
            }
        }
        return new Header(cut(shortest, Math.max(20, available), titleWidth), "");
    }
}
