package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * The morph (UI overhaul phase 7): what the two halves of a shift are and what the server does on which tick. The shift itself (2 seconds, the body swapped at
 * the midpoint) is {@link ShiftRules}'s and is not changed here; this only says what is drawn and made to sound while it runs. Pure.
 *
 * <pre>
 *  0 .. 19   the man, posed by a small table (client); growl, dust and bone cracks (server)
 *  20        the cut: the wolf's body and hitbox (ShiftRules); smoke, howl sound, hurt tilt
 *  20 .. 39  the wolf plays {@code shift_in}; the landing dust on tick 32
 *  40 ..     the wolf plays its howl clip (re-timed to the 2.2 s that are left of the 3.2 s howl)
 * </pre>
 */
public final class MorphRules {
    /** Cue bits for {@link #cues}. */
    public static final int GROWL = 1;
    public static final int DUST = 2;
    public static final int CRACK = 4;
    public static final int LAND = 8;

    private MorphRules() {
    }

    /** What the server makes happen on this tick of a shift (a bit mask; 0 for anything but a shift, or a tick outside it). */
    public static int cues(BossForm form, int tick) {
        if (form != BossForm.SHIFTING || tick < 0 || tick >= ShiftRules.SHIFT_TICKS) {
            return 0;
        }
        int cues = 0;
        if (tick == 0) {
            cues |= GROWL;
        }
        if (tick == 4 || tick == 8 || tick == 12 || tick == 16) {
            cues |= DUST;
        }
        if (tick == 7 || tick == 14) {
            cues |= CRACK;
        }
        if (tick == ShiftRules.SWAP_TICK + 12) {
            cues |= LAND;
        }
        return cues;
    }

    /** The man is drawn: a shift that has not reached the swap. The same as the negation of {@link ShiftRules#showsBeast} for a shift. */
    public static boolean manHalf(BossForm form, int ticks) {
        return form == BossForm.SHIFTING && ticks < ShiftRules.SWAP_TICK;
    }

    /** The wolf is arriving: from the swap to the end of the shift, while its {@code shift_in} clip plays. */
    public static boolean arriving(BossForm form, int ticks) {
        return form == BossForm.SHIFTING && ticks >= ShiftRules.SWAP_TICK && ticks < ShiftRules.SHIFT_TICKS;
    }

    /** How far the man's pose has come, 0 to 1 over the first half; a partial tick over 1 counts as 1, and anything that is not a number counts as 0. */
    public static float manProgress(int ticks, float partial) {
        if (ticks < 0) {
            return 0.0F;
        }
        float part = Float.isNaN(partial) ? 0.0F : Math.max(0.0F, Math.min(partial, 1.0F));
        float p = (ticks + part) / ShiftRules.SWAP_TICK;
        return Math.max(0.0F, Math.min(1.0F, p));
    }
}
