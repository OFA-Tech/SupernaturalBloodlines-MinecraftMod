package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;

/**
 * The weaker spark of an NPC Alpha (R145). It goes straight to the killer; it counts for nothing (not toward {@code max_stacked_sparks}, the pack
 * bonus or the Roar) and cannot be stolen, because it is not in {@link WolfState#sparks()}; it grows only while its holder is online; when it has
 * grown it becomes a normal spark by the rules of a steal ({@link PackRules#stealBy}); it is lost on a cure, when the holder becomes a True Alpha,
 * and with the rank when an Alpha loses it. Pure.
 */
public final class WeakSparkRules {
    private static final long DAY = 24000L;

    /** What a kill of an NPC Alpha by this player does. */
    public enum Grant {
        /** A weak spark takes root. */
        GRANTED,
        /** The path to True Alpha is open and protected: the spark is destroyed (the killer keeps the path). */
        DESTROYED,
        /** A True Alpha, or no werewolf: nothing to give. */
        NEVER,
        /** The killer already holds the most they may. */
        FULL,
        /** The last one was given less than the cooldown ago. */
        COOLDOWN
    }

    private WeakSparkRules() {
    }

    /** The game day of a game time. */
    public static long day(long gameTime) {
        return Math.floorDiv(gameTime, DAY);
    }

    /**
     * What happens to the spark of an NPC Alpha {@code killer} killed on game day {@code today}. A True Alpha and a non-werewolf get nothing; an open,
     * protected path destroys it (as for a stolen spark); then the cooldown and the most that may be held.
     */
    public static Grant grant(WolfState killer, long today, int maxHeld, int cooldownDays) {
        if (killer.lineage() == WolfState.Lineage.NONE || PackRules.stealBy(killer) == PackRules.Steal.NEVER) {
            return Grant.NEVER;
        }
        if (PackRules.stealBy(killer) == PackRules.Steal.PROTECTED) {
            return Grant.DESTROYED;
        }
        Weak weak = killer.weak();
        if (weak.day() > 0 && today - (weak.day() - 1) < cooldownDays) {
            return Grant.COOLDOWN;
        }
        if (weak.held() >= Math.max(1, maxHeld)) {
            return Grant.FULL;
        }
        return Grant.GRANTED;
    }

    /**
     * One more weak spark, given on {@code today}; the one already growing keeps its progress and its time, a first one takes {@code growTicks}.
     * The day is kept as {@code today + 1}, so that 0 can mean "never".
     */
    public static Weak given(Weak weak, long today, int growTicks) {
        return new Weak(weak.held() + 1, weak.progress(), Math.max(0, today) + 1, weak.held() > 0 ? weak.need() : Math.max(1, growTicks));
    }

    /** The result of growing: the state after {@code ticks} online ticks and whether one spark finished. */
    public record Grown(Weak weak, boolean grown) {
    }

    /**
     * {@code ticks} more online ticks of growth; the oldest spark finishes at the time it started with (the setting {@code growTicks} for one from
     * before the field) and the next one starts from nothing, with the setting of that moment.
     */
    public static Grown grow(Weak weak, int ticks, int growTicks) {
        if (weak.held() <= 0) {
            return new Grown(weak, false);
        }
        long progress = (long) weak.progress() + Math.max(0, ticks);
        if (progress >= needOf(weak, growTicks)) {
            return new Grown(new Weak(weak.held() - 1, 0, weak.day(), Math.max(1, growTicks)), true);
        }
        return new Grown(new Weak(weak.held(), (int) progress, weak.day(), weak.need()), false);
    }

    private static long needOf(Weak weak, int fallbackTicks) {
        return Math.max(1, weak.need() > 0 ? weak.need() : fallbackTicks);
    }

    /** What a grown spark does for its holder now, by the rule of a steal: it becomes a normal spark, or withers to protect an open path. */
    public static PackRules.Steal onGrown(WolfState holder) {
        return PackRules.stealBy(holder);
    }

    /** The weak sparks after a cure, a True Alpha earned or the rank lost: none (the day of the last one stays, so a cure is no way round the cooldown). */
    public static Weak lost(Weak weak) {
        return new Weak(0, 0, weak.day(), 0);
    }

    /** Whole days of growth left for the hub ("grows in 2 days"), at least 1 while one is growing; {@code fallbackTicks} is for a spark that carries no time. */
    public static int daysLeft(Weak weak, int fallbackTicks) {
        if (weak.held() <= 0) {
            return 0;
        }
        long left = needOf(weak, fallbackTicks) - weak.progress();
        return (int) Math.max(1, (left + DAY - 1) / DAY);
    }
}
