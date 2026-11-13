package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** Puts the apex races into ManasCore's race registry, next to Tensura's own (they share the registry; ids are ours). */
public final class ApexRaces {
    private ApexRaces() {}

    /** Call from the mod constructor, before the registries open. */
    public static void register() {
        for (ApexSpec spec : ApexSpec.ALL) {
            RaceAPI.getRaceRegistry().register(id(spec), () -> new ApexRace(spec));
        }
    }

    public static ResourceLocation id(ApexSpec spec) {
        return ResourceLocation.fromNamespaceAndPath(ApexSpec.RACE_NAMESPACE, spec.id());
    }

    public static Optional<ResourceLocation> apexFor(Species species) {
        return ApexSpec.forSpecies(species).map(ApexRaces::id);
    }
}
