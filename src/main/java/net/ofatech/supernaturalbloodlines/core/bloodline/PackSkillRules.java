package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;

/**
 * Who may use the two pack abilities of plan 15b (R146), and when: Moonblood Surge (a werewolf in a pack with someone else) and Wolf Spirits
 * (an Alpha or True Alpha). Both are refused in a lunar eclipse, while dead, and during their cooldown; a refusal costs nothing. Pure.
 */
public final class PackSkillRules {
    public enum Refusal { NONE, NOT_WEREWOLF, NOT_ALPHA, NO_PACK, ECLIPSE, UNAVAILABLE, COOLDOWN, OFF }

    /** The Surge's price after it: Weakness I and Slowness I for this long. */
    public static final int SURGE_AFTER_TICKS = 600;
    public static final int SURGE_COOLDOWN_TICKS = 4800;
    public static final int SPIRITS_COOLDOWN_TICKS = 1800;
    public static final int SPIRITS_COUNT = 3;
    public static final int SPIRITS_LIFE_TICKS = 3600;
    public static final double SURGE_RANGE = 24;

    private PackSkillRules() {
    }

    public static Refusal surge(boolean enabled, boolean werewolf, boolean othersInPack, boolean eclipse, boolean available, long now, long last, long cooldown) {
        if (!enabled) {
            return Refusal.OFF;
        }
        if (!werewolf) {
            return Refusal.NOT_WEREWOLF;
        }
        if (!othersInPack) {
            return Refusal.NO_PACK;
        }
        return common(eclipse, available, now, last, cooldown);
    }

    public static Refusal spirits(boolean enabled, boolean werewolf, Rank rank, boolean eclipse, boolean available, long now, long last, long cooldown) {
        if (!enabled) {
            return Refusal.OFF;
        }
        if (!werewolf || (rank != Rank.ALPHA && rank != Rank.TRUE_ALPHA)) {
            return werewolf ? Refusal.NOT_ALPHA : Refusal.NOT_WEREWOLF;
        }
        return common(eclipse, available, now, last, cooldown);
    }

    private static Refusal common(boolean eclipse, boolean available, long now, long last, long cooldown) {
        if (eclipse) {
            return Refusal.ECLIPSE;
        }
        if (!available) {
            return Refusal.UNAVAILABLE;
        }
        return last >= 0 && now - last < cooldown ? Refusal.COOLDOWN : Refusal.NONE;
    }

    /** Whole seconds of cooldown left (rounded up). */
    public static long secondsLeft(long now, long last, long cooldown) {
        return Math.max(0, (cooldown - (now - last) + 19) / 20);
    }
}
