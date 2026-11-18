package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.lang.ref.WeakReference;
import java.util.List;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;

/** The scheduled lunar events for the hub, read at most once per game tick (never per frame). Empty without Enhanced Celestials or when its forecast cannot be read. */
public final class LunarMoodClient {
    /** How far ahead the "next event" line looks. The rings only cover the almanac's six days. */
    public static final int LOOKAHEAD = 100;

    private static WeakReference<Level> of = new WeakReference<>(null);
    private static long at = Long.MIN_VALUE;
    private static List<LunarSource.Upcoming> cached = List.of();

    private LunarMoodClient() {
    }

    public static List<LunarSource.Upcoming> upcoming(Level level) {
        if (level == null) {
            return List.of();
        }
        long now = level.getGameTime();
        if (of.get() != level || now != at) {
            of = new WeakReference<>(level);
            at = now;
            cached = LunarMoods.ahead(level, LOOKAHEAD);
        }
        return cached;
    }
}
