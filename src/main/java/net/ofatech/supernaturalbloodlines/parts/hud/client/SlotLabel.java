package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/** The one line a slot gets beside it while Alt is held (the text only; {@link HudChrome} draws it and {@code HudLayout.labels} places it). Pure. */
final class SlotLabel {
    /** Air inside the label's box, each side. */
    static final int PAD = 4;
    /** A label with less text room than this is not drawn: it would be an ellipsis. */
    static final int MIN_ROOM = 24;
    private static final String SEP = " \u00b7 ";

    private SlotLabel() {
    }

    /** "R  Fireball . mode . why not . mastery": the key and name first, so a cut line loses the extras and never the name. */
    static String text(String key, String name, @Nullable String detail, @Nullable String state, @Nullable String mastery) {
        List<String> parts = new ArrayList<>();
        for (String p : new String[] {detail, state, mastery}) {
            if (p != null && !p.isBlank()) {
                parts.add(p);
            }
        }
        return (key.isEmpty() ? "" : key + "  ") + name + (parts.isEmpty() ? "" : SEP + String.join(SEP, parts));
    }

    static String fit(String text, int width, ToIntFunction<String> measure) {
        return LabelFit.cut(text, width, measure);
    }

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}
