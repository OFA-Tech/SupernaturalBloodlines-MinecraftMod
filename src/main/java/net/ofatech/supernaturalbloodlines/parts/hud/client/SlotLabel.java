package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/**
 * The lines a slot gets beside it while Alt is held (the text only; {@link HudChrome} draws them and {@code HudLayout.labels} places them). A label is at most
 * {@link #MAX_LINES} lines, which is what fits between two neighbouring slots, so labels never cover each other. What does not fit is not cut quietly: the
 * label says it was cut, and the column then shows its wrapped panel instead ({@code HudChrome.panels}). Pure.
 */
final class SlotLabel {
    /** Air inside the label's box, each side. */
    static final int PAD = 4;
    /** A label with less text room than this is not drawn: it would be an ellipsis. */
    static final int MIN_ROOM = 24;
    private static final String SEP = " \u00b7 ";
    /** The most lines a label has: the name, then one line. Two lines are what fit between two slots (a slot's row is 24 px, a two-line label 20). */
    static final int MAX_LINES = 2;
    /** The suffix of the lang key that holds a reason's full sentence; the short one is on the label, the long one in the wrapped panel. */
    static final String LONG = ".long";

    private SlotLabel() {
    }

    /** What a slot's label says; {@code cut} is true when the name, the reason or the text after it did not fit {@code room} and ends in an ellipsis. */
    record Label(String[] lines, boolean cut) {
    }

    /** The label of a slot that has no room beside it at all: it is not drawn and its column falls back to the panel. */
    static final Label TOO_NARROW = new Label(new String[0], true);

    /**
     * The name; then one line: a blocked slot's short reason (that is all: the state, the mastery and the description are in the panel), else why the slot
     * cannot be used or its cooldown, the mastery, the mode and a short description in that order. Each line is cut at the room with an ellipsis.
     */
    static Label of(String name, boolean blocked, @Nullable String state, @Nullable String mastery, @Nullable String detail, @Nullable String description,
                    int room, ToIntFunction<String> measure) {
        String first = LabelFit.cut(name, room, measure);
        boolean reasoned = blocked && state != null && !state.isBlank();
        String second = reasoned ? state.strip() : join(state, mastery, detail, description);
        String shown = LabelFit.cut(second, room, measure);
        return new Label(new String[] {first, shown}, !first.equals(name) || !shown.equals(second));
    }

    /** Which of the two columns shows its wrapped panel instead of labels: one in which a label was cut, or had no room beside it. A slot with nothing in it has no label (null). */
    static boolean[] fallbackColumns(Label[] labels) {
        boolean[] out = new boolean[2];
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != null && labels[i].cut()) {
                out[i / 3] = true;
            }
        }
        return out;
    }

    private static String join(@Nullable String... parts) {
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                out.append(out.length() == 0 ? "" : SEP).append(p.strip());
            }
        }
        return out.toString();
    }

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}