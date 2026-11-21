package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/** The two lines a slot gets beside it while Alt is held (the text only; {@link HudChrome} draws them and {@code HudLayout.labels} places them). Pure. */
final class SlotLabel {
    /** Air inside the label's box, each side. */
    static final int PAD = 4;
    /** A label with less text room than this is not drawn: it would be an ellipsis. */
    static final int MIN_ROOM = 24;
    private static final String SEP = " \u00b7 ";

    private SlotLabel() {
    }

    /**
     * The name; then, on the second line, the mastery ("42%" or "Mastered"), why the slot cannot be used or its cooldown, the mode and a short description,
     * in that order, so a cut line loses the description first.
     */
    static String[] lines(String name, @Nullable String mastery, @Nullable String state, @Nullable String detail, @Nullable String description) {
        List<String> parts = new ArrayList<>();
        for (String p : new String[] {mastery, state, detail, description}) {
            if (p != null && !p.isBlank()) {
                parts.add(p.strip());
            }
        }
        return new String[] {name, String.join(SEP, parts)};
    }

    /** The first line is cut only when the name itself is wider than the room, the second whenever it does not fit. */
    static String fit(String text, int width, ToIntFunction<String> measure) {
        return LabelFit.cut(text, width, measure);
    }

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}
