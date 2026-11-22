package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** The glow a HUD slot gives for one loop when its cooldown has just run out. A flourish, not information: reduce motion switches it off. Pure. */
public final class ReadyGlow {
    public static final int TICKS = Motion.GLOW_LOOP;

    private ReadyGlow() {
    }

    /** The slot was cooling down and is ready now. */
    public static boolean justReady(float before, float now) {
        return before > 0f && now <= 0f;
    }

    /** The glow's alpha (0 to 1) {@code age} ticks after the slot became ready; nothing before, after the loop, or with reduce motion. */
    public static float alpha(long age, boolean reduce) {
        if (reduce || age < 0 || age >= TICKS) {
            return 0f;
        }
        return Motion.glowPercent(age, false) / 100f;
    }
}