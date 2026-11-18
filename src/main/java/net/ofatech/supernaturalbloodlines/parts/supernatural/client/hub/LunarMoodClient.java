package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;

/**
 * The scheduled lunar events for the hub. The source (Enhanced Celestials' forecast, by reflection) is asked once a second, not per frame or per
 * tick; everything the pages need from it is worked out then. Empty without Enhanced Celestials or when its forecast cannot be read.
 */
public final class LunarMoodClient {
    /** How far ahead the "next event" line looks. The rings only cover the almanac's row. */
    public static final int LOOKAHEAD = 100;
    /** Ticks between two reads of the source. */
    static final int REFRESH_TICKS = 20;

    /** What the almanac draws: the events of the row by day (the rings), the lines to write, and whether there is anything at all. */
    public record View(Map<Integer, LunarSource.Upcoming> byDay, List<LunarSource.Upcoming> lines) {
        static final View NONE = new View(Map.of(), List.of());

        public boolean isEmpty() {
            return lines.isEmpty();
        }

        public Optional<LunarSource.Upcoming> onDay(int day) {
            return Optional.ofNullable(byDay.get(day));
        }
    }

    private static WeakReference<Level> of = new WeakReference<>(null);
    private static long bucket = Long.MIN_VALUE;
    private static View cached = View.NONE;

    private LunarMoodClient() {
    }

    /** The almanac's events as of now (at most a second old). */
    public static View view(Level level) {
        if (level == null) {
            return View.NONE;
        }
        long now = level.getGameTime() / REFRESH_TICKS;
        if (of.get() != level || now != bucket) {
            of = new WeakReference<>(level);
            bucket = now;
            cached = build(LunarMoods.ahead(level, LOOKAHEAD));
        }
        return cached;
    }

    /** Pure: the view of a list of events (nearest first). */
    static View build(List<LunarSource.Upcoming> events) {
        if (events.isEmpty()) {
            return View.NONE;
        }
        Map<Integer, LunarSource.Upcoming> byDay = new HashMap<>();
        for (LunarSource.Upcoming e : events) {
            if (e.days() >= 0 && e.days() < HubAlmanac.ROW) {
                byDay.putIfAbsent(e.days(), e);
            }
        }
        return new View(Map.copyOf(byDay), HubAlmanac.listed(events));
    }
}
