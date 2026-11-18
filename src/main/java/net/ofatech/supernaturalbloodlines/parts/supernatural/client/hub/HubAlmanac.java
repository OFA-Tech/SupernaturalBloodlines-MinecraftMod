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
