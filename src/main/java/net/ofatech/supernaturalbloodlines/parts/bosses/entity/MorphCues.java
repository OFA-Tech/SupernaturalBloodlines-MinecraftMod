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
    /**
     * What one tick's cues actually made: the mask, the sounds played, the particles asked for and how many players the level sent them to (what
     * {@code ServerLevel.sendParticles} says it did, not what the mask says should have happened: with nobody near, it is 0).
     */
    public record Played(Entity boss, int shiftTick, int cues, int sounds, int particles, int viewers) {
    }

    /** Tests: told every cue that is played. */
    public interface Observer {
        void played(Played played);
    }

    /** The smoke of one dust cue and the puff of the landing: a few particles, never a burst (the swap's own is the entity's). */
    public static final int DUST_COUNT = 4;
    public static final int LAND_COUNT = 12;

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
        int sounds = 0;
        int particles = 0;
        int viewers = 0;
        if ((cues & MorphRules.GROWL) != 0) {
            level.playSound(null, x, y, z, SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.0F, 0.6F);
            sounds++;
        }
        if ((cues & MorphRules.DUST) != 0) {
            // the smoke climbs from the feet to the shoulder as the tick goes on
            double height = 0.2 + 1.4 * Math.min(1.0, tick / (double) ShiftRules.SWAP_TICK);
            viewers += level.sendParticles(ParticleTypes.SMOKE, x, y + height, z, DUST_COUNT, 0.25, 0.05, 0.25, 0.01);
            particles += DUST_COUNT;
        }
        if ((cues & MorphRules.CRACK) != 0) {
            level.playSound(null, x, y, z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 0.8F, 0.7F);
            sounds++;
        }
        if ((cues & MorphRules.LAND) != 0) {
            viewers += level.sendParticles(ParticleTypes.POOF, x, y + 0.1, z, LAND_COUNT, 0.4, 0.05, 0.4, 0.02);
            particles += LAND_COUNT;
            level.playSound(null, x, y, z, SoundEvents.WOLF_STEP, SoundSource.HOSTILE, 1.0F, 0.5F);
            sounds++;
        }
        Observer o = observer;
        if (o != null) {
            o.played(new Played(boss, tick, cues, sounds, particles, viewers));
        }
    }
}
