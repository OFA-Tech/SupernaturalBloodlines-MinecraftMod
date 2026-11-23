package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

/** The one cooldown a group starts, whichever side was used. Pure. */
public final class SharedCooldown {
    /** A member the player does not have (or whose cooldown cannot be read now). */
    public static final int NOT_OWNED = -1;

    private SharedCooldown() {
    }

    /**
     * The main side's own cooldown (0 when it has none, which means the group has none). When the player does not have the main side, the way {@link Dedup} falls back: the first member
     * they do have, in file order; and when that one has none, the used side's own, so the shared cooldown is never 0 while the side that was used has one.
     *
     * @param own each member's own cooldown in ticks, in file order, {@link #NOT_OWNED} for a member the player does not have
     * @param main the index of the main member
     * @param used the index of the side that was used
     */
    public static int pick(int[] own, int main, int used) {
        if (main >= 0 && main < own.length && own[main] != NOT_OWNED) {
            return own[main];
        }
        for (int cooldown : own) {
            if (cooldown != NOT_OWNED) {
                return cooldown > 0 ? cooldown : usedCooldown(own, used);
            }
        }
        return usedCooldown(own, used);
    }

    private static int usedCooldown(int[] own, int used) {
        return used >= 0 && used < own.length ? Math.max(0, own[used]) : 0;
    }
}
