package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;

/** The moon, as the almanac shows it: the next few phases and the days to the full moon. Vanilla phase 0 is the full moon, one phase a day, eight in all. Pure. */
public final class HubAlmanac {
    private HubAlmanac() {
    }

    /** Days until the next full moon: 0 when it is the full moon today. */
    public static int daysToFull(int phase) {
        return Math.floorMod(8 - phase, 8);
    }

    /** The lunar event on day {@code day} (days from today) of a list sorted nearest first, if any. */
    public static Optional<LunarSource.Upcoming> eventOn(List<LunarSource.Upcoming> events, int day) {
        return events.stream().filter(e -> e.days() == day).findFirst();
    }

    /** The almanac's row: this many days, from today. */
    public static final int ROW = 6;
    /** The room round a moon icon that a ring takes, on each side. */
    public static final int RING_REACH = 2;

    /**
     * Every event of the row's days as a text line (nearest first); when none falls in the row, the nearest one beyond it, so "Blood Moon in 31 days"
     * is still said. Empty without events.
     */
    public static List<LunarSource.Upcoming> listed(List<LunarSource.Upcoming> events) {
        List<LunarSource.Upcoming> inRow = events.stream().filter(e -> e.days() >= 0 && e.days() < ROW).toList();
        return inRow.isEmpty() ? next(events).map(List::of).orElse(List.of()) : inRow;
    }

    /** The distance between two moon icons of {@code iconSize}: {@code base} as it is, or wider when rings are drawn so that neighbours' rings never touch. */
    public static int step(int base, int iconSize, boolean rings) {
        return rings ? Math.max(base, iconSize + 2 * RING_REACH + 2) : base;
    }

    /** The nearest lunar event of a list sorted nearest first (today's counts), if any. */
    public static Optional<LunarSource.Upcoming> next(List<LunarSource.Upcoming> events) {
        return events.stream().filter(e -> e.days() >= 0).findFirst();
    }

    /** The vanilla phases of {@code count} days, starting today. */
    public static List<Integer> phases(int today, int count) {
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(Math.floorMod(today + i, 8));
        }
        return out;
    }
}
