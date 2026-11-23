package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What the narrator says (and a token's tooltip shows) about the boss row: the focus boss in full, then every other boss, hidden ones included, so nothing the row
 * collapses into "+N" is lost to a player who cannot see it. Pure: it gives lang keys and arguments, the client turns them into text.
 */
public final class BossNarration {
    public static final String KEY = "supernaturalbloodlines.boss.bar.narration.";

    /** @param form the lang key of the boss's form chip, or null for a boss without forms */
    public record Entry(String name, int percent, @Nullable String form) {
        public Entry {
            percent = Math.max(0, Math.min(100, percent));
        }
    }

    /** A line to say: a lang key and its arguments. */
    public record Line(String key, List<Object> args) {
    }

    private BossNarration() {
    }

    /** The health as a whole percent, rounded up so a living boss never reads 0 (the same rule as the card's number). */
    public static int percent(int hp, int max) {
        return max <= 0 ? 0 : Math.max(hp > 0 ? 1 : 0, Math.min(100, (int) Math.ceil(100.0 * hp / max)));
    }

    /** The whole row: first the focus boss, then one line naming the others in the order of the row (none when the focus is alone). */
    public static List<Line> row(Entry focus, List<Entry> others) {
        List<Line> lines = new ArrayList<>();
        lines.add(one(focus, others.isEmpty() ? "focus_alone" : "focus"));
        if (!others.isEmpty()) {
            StringBuilder list = new StringBuilder();
            for (Entry e : others) {
                if (list.length() > 0) {
                    list.append(", ");
                }
                list.append(e.name()).append(' ').append(e.percent()).append('%');
            }
            lines.add(new Line(KEY + "others", List.of(others.size(), list.toString())));
        }
        return lines;
    }

    /** Just the focus boss: what is said when only the focus changes. */
    public static Line focusOnly(Entry e) {
        return one(e, "focus_alone");
    }

    /** One boss: the tooltip of a token, and the first line of the narration. */
    public static Line one(Entry e, String which) {
        if (e.form() == null) {
            return new Line(KEY + which, List.of(e.name(), e.percent()));
        }
        return new Line(KEY + which + "_form", List.of(e.name(), e.percent(), e.form()));
    }

    /** The tooltip of the "+N" token: the hidden bosses, one line each. */
    public static List<Line> hidden(List<Entry> hidden) {
        List<Line> lines = new ArrayList<>(hidden.size());
        for (Entry e : hidden) {
            lines.add(one(e, "token"));
        }
        return lines;
    }
}
