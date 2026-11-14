package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import java.util.function.LongFunction;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;

/** The clock the hybrid transition and the turning share: online ticks, counted down in steps of {@link #STEP}. */
public final class TransitionTimer {
    public static final int STEP = 20;

    private TransitionTimer() {
    }

    /** True on the player tick where another {@link #STEP} ticks have passed. */
    public static boolean secondIsUp(ServerPlayer p) {
        return p.tickCount % STEP == 0;
    }

    /**
     * One step passed with {@code left} ticks on the clock. While time remains the lower count is saved but not synced (the species
     * does not change, and a packet a second for the whole transition is noise; the next real change or a login sends the current
     * value) and this returns false. True means it ran out and the caller applies its timeout.
     */
    public static boolean tick(ServerPlayer p, long left, LongFunction<BloodlineState> withLeft) {
        long next = left - STEP;
        if (next > 0) {
            p.setData(ModAttachments.BLOODLINE, withLeft.apply(next));
            return false;
        }
        return true;
    }
}
