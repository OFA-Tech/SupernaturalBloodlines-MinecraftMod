package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;

/** The moon, as the almanac shows it: the next few phases and the days to the full moon. Vanilla phase 0 is the full moon, one phase a day, eight in all. Pure. */
public final class HubAlmanac {
    private HubAlmanac() {
    }

    /** Days until the next full moon: 0 when it is the full moon today. */
    public static int daysToFull(int phase) {
        return MoonCalendar.daysToFull(phase);
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

    /** The events of the moon calendar's days, by day (the first event of a day wins). */
    public static java.util.Map<Integer, LunarSource.Upcoming> calendarEvents(List<LunarSource.Upcoming> events) {
        java.util.Map<Integer, LunarSource.Upcoming> out = new java.util.HashMap<>();
        for (LunarSource.Upcoming e : events) {
            if (e.days() >= 0 && e.days() < MoonCalendar.DAYS) {
                out.putIfAbsent(e.days(), e);
            }
        }
        return java.util.Map.copyOf(out);
    }

    /** Least contrast (WCAG) a ring must have against the page. */
    public static final double RING_CONTRAST = 3.0;

    /** The colour of a ring: the event's ({@code event} RGB, -1 for none) when it has {@link #RING_CONTRAST} against the page's {@code paper}, else {@code fallback}. Opaque. */
    public static int ringColor(int event, int paper, int fallback) {
        return event >= 0 && Contrast.ratio(event, paper) >= RING_CONTRAST ? 0xFF000000 | event : fallback;
    }

    /** Whether the pinned almanac may sit at the foot of the page: it must leave at least half of the page to the scrolling part, else it stays in the flow. */
    public static boolean mayPin(int pinHeight, int regionHeight) {
        return pinHeight <= regionHeight / 2;
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
