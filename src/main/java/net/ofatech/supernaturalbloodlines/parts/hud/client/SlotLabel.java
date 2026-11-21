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
    /** The most lines a label has, name included (what fits between two neighbouring slots); only a long reason takes more. */
    static final int MAX_LINES = 3;
    /** A reason is wrapped over at most this many lines, which is far more than any sentence of the mod needs in the narrowest label that is drawn. */
    static final int REASON_LINES = 8;

    private SlotLabel() {
    }

    /**
     * The name (cut only when it is wider than the room); then, in this order, why the slot cannot be used or its cooldown, the mastery, the mode and a short
     * description. A cut loses the end, so the description goes first. A slot that cannot be used puts its reason first, wrapped over as many lines as it needs
     * (never cut); what follows takes the room left in {@link #MAX_LINES} lines, the number that fits between two neighbouring slots.
     *
     * @return the name, then one line, or for a blocked slot the reason's lines and, room permitting, one more
     */
    static String[] lines(String name, boolean blocked, @Nullable String state, @Nullable String mastery, @Nullable String detail, @Nullable String description,
                          int room, ToIntFunction<String> measure) {
        List<String> out = new ArrayList<>();
        out.add(LabelFit.cut(name, room, measure));
        boolean reasoned = blocked && state != null && !state.isBlank();
        if (reasoned) {
            out.addAll(LabelFit.wrap(state.strip(), room, REASON_LINES, measure));
        }
        String rest = join(reasoned ? null : state, mastery, detail, description);
        int left = reasoned ? MAX_LINES - out.size() : 1;
        if (left > 0 && !rest.isEmpty()) {
            if (left == 1) {
                out.add(LabelFit.cut(rest, room, measure));
            } else {
                out.addAll(LabelFit.wrap(rest, room, left, measure));
            }
        } else if (!reasoned && out.size() == 1) {
            out.add("");
        }
        return out.toArray(new String[0]);
    }

    private static String join(@Nullable String... parts) {
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                kept.add(p.strip());
            }
        }
        return String.join(SEP, kept);
    }

    static int percent(Mastery m) {
        return Math.round(m.fraction() * 100f);
    }
}
