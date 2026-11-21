package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/** The lines a slot gets beside it while Alt is held (the text only; {@link HudChrome} draws them and {@code HudLayout.labels} places them). Pure. */
final class SlotLabel {
    /** Air inside the label's box, each side. */
    static final int PAD = 4;
    /** A label with less text room than this is not drawn: it would be an ellipsis. */
    static final int MIN_ROOM = 24;
    private static final String SEP = " \u00b7 ";
    /** A blocked slot's second block: enough lines for the reason (a sentence) to be whole in the narrowest label that is drawn; what comes after it may be cut. */
    static final int MAX_BLOCKED_LINES = 4;

    private SlotLabel() {
    }

    /**
     * The name (cut only when it is wider than the room); then, in this order, why the slot cannot be used or its cooldown, the mastery, the mode and a short
     * description. A cut loses the end, so the description goes first. A slot that cannot be used wraps onto a third line, so the reason is never cut, and a label stays within three lines (which is what fits between two neighbouring slots) unless the reason alone needs more.
     *
     * @return two lines, or three for a blocked slot whose second line does not fit (more only for a reason of three lines or more)
     */
    static String[] lines(String name, boolean blocked, @Nullable String state, @Nullable String mastery, @Nullable String detail, @Nullable String description,
                          int room, ToIntFunction<String> measure) {
        List<String> parts = new ArrayList<>();
        for (String p : new String[] {state, mastery, detail, description}) {
            if (p != null && !p.isBlank()) {
                parts.add(p.strip());
            }
        }
        String rest = String.join(SEP, parts);
        List<String> out = new ArrayList<>();
        out.add(LabelFit.cut(name, room, measure));
        List<String> reason = state == null ? List.of() : LabelFit.wrap(state.strip(), room, MAX_BLOCKED_LINES, measure);
        if (blocked && reason.size() > 2) {
            out.addAll(reason); // a long sentence in a narrow label: all of it, and nothing after it
        } else if (blocked) {
            out.addAll(LabelFit.wrap(rest, room, 2, measure)); // the reason is whole in these two lines (it fits one or two), what follows may be cut
        } else {
            out.add(LabelFit.cut(rest, room, measure));
        }
        return out.toArray(new String[0]);
    }

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}
