package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/**
 * The lines a slot gets beside it while Alt is held (the text only; {@link HudChrome} draws them and {@code HudLayout.labels} places them): its name and its state.
 * A label is at most {@link #MAX_LINES} lines, which is what fits between two neighbouring slots, so labels never cover each other. Everything else is in the
 * column's panel ({@link PanelText}), which shows after Alt has been held a second, or at once where a column has no room for a label. Pure.
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

    /**
     * What a slot's label says. {@code cut} is true when its state does not fit {@code room} even in its widest form ("Ready in 99 s"): the label is then no use
     * and its column shows the panel instead. Deciding by the widest form keeps a label from switching to the panel and back as a cooldown counts down. A name
     * that is too long ends in an ellipsis and is not "cut" in this sense: the full name is in the panel.
     */
    record Label(String[] lines, boolean cut) {
    }

    /** The label of a slot that has no room beside it at all: it is not drawn and its column falls back to the panel. */
    static final Label TOO_NARROW = new Label(new String[0], true);

    /**
     * The name, then its state: why a blocked slot cannot be used (the short reason), else its cooldown, and the mastery after it when both fit. The
     * description, the mode and the long texts are in the panel. {@code stateWidest} and {@code masteryWidest} are the same texts at their widest (99 seconds,
     * 100 %), or null when they do not change.
     */
    static Label of(String name, boolean blocked, @Nullable String state, @Nullable String stateWidest, @Nullable String mastery, @Nullable String masteryWidest,
                    int room, ToIntFunction<String> measure) {
        String first = LabelFit.cut(name, room, measure);
        boolean hasState = state != null && !state.isBlank();
        boolean hasMastery = mastery != null && !mastery.isBlank();
        String second;
        String widest;
        if (hasState) {
            String sw = stateWidest != null ? stateWidest : state;
            String mw = masteryWidest != null ? masteryWidest : mastery;
            boolean both = hasMastery && !blocked && measure.applyAsInt(sw.strip() + SEP + mw.strip()) <= room;
            second = both ? state.strip() + SEP + mastery.strip() : state.strip();
            widest = sw.strip();
        } else if (hasMastery) {
            second = mastery.strip();
            widest = (masteryWidest != null ? masteryWidest : mastery).strip();
        } else {
            second = "";
            widest = "";
        }
        return new Label(new String[] {first, LabelFit.cut(second, room, measure)}, measure.applyAsInt(widest) > room);
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

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}