package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Facts about the world and the player that the rules need but the state does not hold.
 *
 * @param maxOriginals 0 = unlimited
 */
public record BloodlineContext(BaseFaction faction, boolean humanLike, int originalsInWorld, int maxOriginals,
                               @Nullable UUID bloodSource, long transitionTicks, long gameTime, int fiveCount,
                               int maxFive, boolean latentGene) {

    public static BloodlineContext defaults() {
        return new BloodlineContext(BaseFaction.NONE, true, 0, 0, null, 24000, 0, 0, 5, false);
    }

    public BloodlineContext withFaction(BaseFaction v) {
        return new BloodlineContext(v, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withHumanLike(boolean v) {
        return new BloodlineContext(faction, v, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withOriginalsInWorld(int v) {
        return new BloodlineContext(faction, humanLike, v, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withMaxOriginals(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, v, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withBloodSource(@Nullable UUID v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, v, transitionTicks, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withTransitionTicks(long v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, v, gameTime, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withGameTime(long v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, v, fiveCount, maxFive, latentGene);
    }

    public BloodlineContext withFiveCount(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, v, maxFive, latentGene);
    }

    public BloodlineContext withMaxFive(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, v, latentGene);
    }

    public BloodlineContext withLatentGene(boolean v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, v);
    }
}
