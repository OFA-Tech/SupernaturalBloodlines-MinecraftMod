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

    /**
     * What each slot glows with this frame. It remembers the cooldown it saw last frame, so a slot whose cooldown runs out glows once. It starts over (no slot
     * glows) whenever the HUD was not shown or the mode or the loadout changed, since a different slot's cooldown running out in another mode is not this slot
     * becoming ready; and a slot that is empty or greyed never glows.
     */
    public static final class Tracker {
        private final float[] before;
        private final long[] readyAt;
        private int context;
        private boolean primed;

        public Tracker(int slots) {
            before = new float[slots];
            readyAt = new long[slots];
            reset();
        }

        /** The HUD was not drawn: the next frame is a fresh start. */
        public void reset() {
            primed = false;
            java.util.Arrays.fill(before, 0f);
            java.util.Arrays.fill(readyAt, Long.MIN_VALUE / 2);
        }

        /**
         * @param cooldowns 0 to 1 per slot, how much of its cooldown is left
         * @param eligible  per slot, whether it can glow: filled and not greyed
         * @param context   anything that differs when the mode or the loadout does
         * @param out       filled with each slot's glow alpha, 0 to 1
         */
        public void update(float[] cooldowns, boolean[] eligible, int context, long time, boolean reduce, float[] out) {
            boolean fresh = !primed || context != this.context;
            this.context = context;
            primed = true;
            for (int i = 0; i < before.length; i++) {
                float now = eligible[i] ? cooldowns[i] : 0f;
                if (!eligible[i] || fresh) {
                    before[i] = now;
                    readyAt[i] = Long.MIN_VALUE / 2;
                    out[i] = 0f;
                    continue;
                }
                if (justReady(before[i], now)) {
                    readyAt[i] = time;
                }
                before[i] = now;
                out[i] = alpha(time - readyAt[i], reduce);
            }
        }
    }
}