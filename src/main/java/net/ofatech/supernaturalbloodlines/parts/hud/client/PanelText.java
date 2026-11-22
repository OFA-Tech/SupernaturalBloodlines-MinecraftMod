package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/**
 * The text of a column's panel, the place for everything the two-line side labels leave out: each slot's key and full name, why it cannot be used (the whole
 * sentence), its cooldown and mastery, and what it does. It is fitted to the lines the screen has room for: what is optional (the second line of a name, the
 * description) goes first, so every slot keeps its name and its state. Pure.
 */
final class PanelText {
    enum Role { NAME, WARN, BODY }

    /** @param required a line that is dropped only when nothing else is left to drop */
    record Line(String text, Role role, boolean required, boolean anchor) {
    }

    /**
     * One slot's contribution.
     *
     * @param head   the key and the name
     * @param reason why it cannot be used, in full; null when it can
     * @param state  its cooldown and mastery, joined; null when it has neither
     * @param about  its mode and what it does, joined; null when it says nothing
     */
    record Entry(String head, @Nullable String reason, @Nullable String state, @Nullable String about) {
    }

    private PanelText() {
    }

    static List<Line> lines(List<Entry> entries, int textW, int maxLines, ToIntFunction<String> measure) {
        List<Line> out = new ArrayList<>();
        for (Entry e : entries) {
            add(out, LabelFit.wrap(e.head(), textW, 2, measure), Role.NAME, true);
            if (e.reason() != null && !e.reason().isBlank()) {
                add(out, LabelFit.wrap(e.reason(), textW, 3, measure), Role.WARN, true);
            }
            if (e.state() != null && !e.state().isBlank()) {
                add(out, LabelFit.wrap(e.state(), textW, 2, measure), Role.BODY, true);
            }
            if (e.about() != null && !e.about().isBlank()) {
                add(out, LabelFit.wrap(e.about(), textW, 2, measure), Role.BODY, false);
            }
        }
        // too tall for the room above the column: the optional lines go from the last slot back, then the second lines of the rest
        while (out.size() > maxLines && dropLast(out, false)) {
            // dropped
        }
        while (out.size() > Math.max(1, maxLines) && dropLast(out, true)) {
            // dropped
        }
        return out;
    }

    /** The first line of a piece is required; its later lines are only as required as the piece's first. */
    private static void add(List<Line> out, List<String> lines, Role role, boolean required) {
        for (int i = 0; i < lines.size(); i++) {
            // the first line of a name anchors its slot: it is the last thing to go
            out.add(new Line(lines.get(i), role, required && i == 0, role == Role.NAME && i == 0));
        }
    }

    /** Drops the last line that is not required ({@code any} false) or the last line that is not the first line of a name ({@code any} true). */
    private static boolean dropLast(List<Line> out, boolean any) {
        for (int i = out.size() - 1; i >= 0; i--) {
            Line l = out.get(i);
            if (any ? !l.anchor() : !l.required()) {
                out.remove(i);
                return true;
            }
        }
        return false;
    }
}
