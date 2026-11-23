package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.ofatech.supernaturalbloodlines.core.boss.MorphRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;

/**
 * The server's part of the morph (UI overhaul phase 7): the sounds and particles of the cues {@link MorphRules#cues} names, on the tick it names them. Vanilla
 * assets only, a few particles at most (the swap's own burst is the entity's and is unchanged), and no packet of its own: the particles and sounds are the usual
 * ones. Nothing here knows about the client.
 */
public final class MorphCues {
    /** Tests: told every cue that is played. */
    public interface Observer {
        void played(Entity boss, int shiftTick, int cues);
    }

    private static volatile Observer observer;

    private MorphCues() {
    }

    /** Tests: sees every cue; null stops. */
    public static void observe(@Nullable Observer o) {
        observer = o;
    }

    /** Plays what happens on this tick of the shift: the growl, the dust that climbs a body, the bone cracks, the landing. */
    static void play(ServerLevel level, Entity boss, int tick, int cues) {
        if (cues == 0) {
            return;
        }
        double x = boss.getX();
        double y = boss.getY();
        double z = boss.getZ();
        if ((cues & MorphRules.GROWL) != 0) {
            level.playSound(null, x, y, z, SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        if ((cues & MorphRules.DUST) != 0) {
            // the smoke climbs from the feet to the shoulder as the tick goes on
            double height = 0.2 + 1.4 * Math.min(1.0, tick / (double) ShiftRules.SWAP_TICK);
            level.sendParticles(ParticleTypes.SMOKE, x, y + height, z, 4, 0.25, 0.05, 0.25, 0.01);
        }
        if ((cues & MorphRules.CRACK) != 0) {
            level.playSound(null, x, y, z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 0.8F, 0.7F);
        }
        if ((cues & MorphRules.LAND) != 0) {
            level.sendParticles(ParticleTypes.POOF, x, y + 0.1, z, 12, 0.4, 0.05, 0.4, 0.02);
            level.playSound(null, x, y, z, SoundEvents.WOLF_STEP, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        Observer o = observer;
        if (o != null) {
            o.played(boss, tick, cues);
        }
    }
}
