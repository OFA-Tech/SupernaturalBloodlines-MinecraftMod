package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;

/**
 * Asks an installed addon, at the moment of a rite, what its own road for a skill needs (R2): its live config, its own helpers. A hook that cannot be
 * found makes the recipe unavailable and names the missing class or method; the probe never grants by guessing.
 */
public interface AddonProbe {
    /** The first hook this probe needs that is not there, or empty when every one is. */
    Optional<String> missingHook();

    /** Whether the addon's own road is open at all (an addon switch, a version that has closed it). */
    boolean roadOpen();

    /** Whether the target meets the addon's own requirements for this recipe. Called only after {@link #missingHook()} was empty. */
    boolean satisfied(ServerPlayer target, EvolutionRecipe recipe);

    /** Whether the single-owner rule of the addon (FoxAblaze, 19b) says another player owns the skill. */
    default boolean ownedByOther(ServerPlayer target, EvolutionRecipe recipe) {
        return false;
    }

    /** A skill id to grant instead of the recipe's own (the merged Raphael: FoxAblaze's when it is installed). Empty: the recipe's own. */
    default Optional<String> grantOverride() {
        return Optional.empty();
    }

    /** Skill ids that count as already holding the grant (either Raphael). */
    default List<String> alsoHolds() {
        return List.of();
    }
}
