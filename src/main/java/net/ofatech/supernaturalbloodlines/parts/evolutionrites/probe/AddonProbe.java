package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
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

    /** Whether a switch of the addon that belongs to the world (a game rule) lets this road work in the level; off is a plain refusal to the performer (E10 d). */
    default boolean enabledIn(ServerLevel level) {
        return true;
    }

    /** What the addon's own road writes when it grants (a cooldown tag): called once, after a learn was confirmed and the recipe's skills were forgotten. */
    default void onGranted(ServerPlayer target, EvolutionRecipe recipe) {
    }

    /** Whether the single-owner rule of the addon (FoxAblaze, 19b) says another player owns the skill. */
    default boolean ownedByOther(ServerPlayer target, EvolutionRecipe recipe) {
        return false;
    }
}
