package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;

/**
 * The evolution rites (plan 19): a witch-type performer opens a road to a skill that already exists, on a full moon or an eclipse, and the Rite of
 * Mastery. No compile dependency on any addon, no mixin: evolutionRites 0 of 3.
 */
public final class EvolutionRitesPart {
    private EvolutionRitesPart() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, EvolutionRitesConfig.SPEC, EvolutionRitesConfig.FILE);
        AdminSpecs.register(EvolutionRitesConfig.FILE, EvolutionRitesConfig.SPEC);
        SupernaturalBloodlines.LOGGER.info("EvolutionRitesPart enabled");
    }
}
