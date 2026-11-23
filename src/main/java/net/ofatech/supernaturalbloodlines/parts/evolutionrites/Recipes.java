package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import java.util.Optional;
import java.util.function.Predicate;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AddonProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Probes;

/** Whether a recipe exists in this world: its addon is loaded, its probe found every hook, the addon's road is open. */
public final class Recipes {
    private Recipes() {
    }

    public static boolean modLoaded(String modId) {
        return modId.isEmpty() || ModList.get() != null && ModList.get().isLoaded(modId);
    }

    /** The probe of the recipe when it is usable: the mod is loaded, the probe exists and finds all its hooks. */
    public static Optional<AddonProbe> usableProbe(EvolutionRecipe recipe, Predicate<String> loaded) {
        if (!loaded.test(recipe.requiresMod())) {
            return Optional.empty();
        }
        return Probes.of(recipe.probe()).filter(p -> p.missingHook().isEmpty());
    }

    public static boolean available(EvolutionRecipe recipe, Predicate<String> loaded) {
        return EvolutionRitesConfig.recipeOpen(recipe.id(), recipe.tier()) && usableProbe(recipe, loaded).filter(AddonProbe::roadOpen).isPresent()
                && EvoSkills.skill(recipe.grants()).isPresent();
    }
}
