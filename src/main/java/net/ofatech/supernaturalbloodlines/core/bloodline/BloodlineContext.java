package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Facts about the world and the player that the rules need but the state does not hold. The hidden werewolf
 * gene is not here: rules read it from {@link BloodlineState#wolf()}.
 *
 * @param maxOriginals 0 = unlimited
 * @param werewolvesLoaded false: the rules refuse every hybrid type (and the hybrid transition) with {@code needs_werewolves}
 * @param vampireBloodTicks how long vampire blood stays in the system after it is drunk (plan 11)
 */
public record BloodlineContext(BaseFaction faction, boolean humanLike, int originalsInWorld, int maxOriginals,
                               @Nullable UUID bloodSource, long transitionTicks, long gameTime, int fiveCount,
                               int maxFive, boolean werewolvesLoaded, long vampireBloodTicks) {

    public static BloodlineContext defaults() {
        return new BloodlineContext(BaseFaction.NONE, true, 0, 0, null, 24000, 0, 0, 5, true, 24000);
    }

    public BloodlineContext withFaction(BaseFaction v) {
        return new BloodlineContext(v, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withHumanLike(boolean v) {
        return new BloodlineContext(faction, v, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withOriginalsInWorld(int v) {
        return new BloodlineContext(faction, humanLike, v, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withMaxOriginals(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, v, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withBloodSource(@Nullable UUID v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, v, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withTransitionTicks(long v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, v, gameTime, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withGameTime(long v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, v, fiveCount, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withFiveCount(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, v, maxFive, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withMaxFive(int v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, v, werewolvesLoaded, vampireBloodTicks);
    }

    public BloodlineContext withWerewolvesLoaded(boolean v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, v, vampireBloodTicks);
    }

    public BloodlineContext withVampireBloodTicks(long v) {
        return new BloodlineContext(faction, humanLike, originalsInWorld, maxOriginals, bloodSource, transitionTicks, gameTime, fiveCount, maxFive, werewolvesLoaded, v);
    }
}
