package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The text of a paragraph that opens with a drop cap: the first letter is drawn large on its own, and the rest of the word continues beside it on the same line
 * ({@code V} + {@code ampire}), so the two read as one word. The first {@code capLines} lines are indented past the capital, the others use the full width. Pure.
 */
public final class DropCap {
    /** One line of the paragraph: how far it is indented and what it says. */
    public record Line(int indent, String text) {
    }

    private DropCap() {
    }

    /**
     * @param rest     the paragraph without its first letter
     * @param indent   the width of the capital and the one pixel after it
     * @param capLines how many lines the capital is as tall as
     */
    public static List<Line> layout(String rest, int width, int indent, int capLines, ToIntFunction<String> measure) {
        List<Line> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String raw : rest.strip().split("\\s+")) {
            if (raw.isEmpty()) {
                continue;
            }
            int in = out.size() < capLines ? indent : 0;
            // one word wider than its line is cut with an ellipsis, never run out of the page
            String word = LabelFit.cut(raw, Math.max(1, width - in), measure);
            String joined = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && measure.applyAsInt(joined) > width - in) {
                out.add(new Line(in, line.toString()));
                line.setLength(0);
                joined = word;
            }
            line.setLength(0);
            line.append(joined);
        }
        if (line.length() > 0) {
            out.add(new Line(out.size() < capLines ? indent : 0, line.toString()));
        }
        return out;
    }
}
