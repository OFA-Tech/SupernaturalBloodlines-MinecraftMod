package net.ofatech.supernaturalbloodlines.parts.species;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SpeciesGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;

public final class SpeciesPart {
    private SpeciesPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SpeciesConfig.SPEC, SpeciesConfig.FILE);
        // the faction rites (vampirism_enchanted part) now run with the real context and through the executor
        FactionRiteHooks.install((p, faction, time) -> SpeciesContext.of(p, faction, time, null), OutcomeExecutor::run);
        BloodlineAccess.addListener((p, change) -> SpeciesData.get(p.server).setOriginal(p.getUUID(), isOriginal(change.after().species())));
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(SpeciesGameTests.class));
        SupernaturalBloodlines.LOGGER.info("SpeciesPart enabled");
    }

    private static boolean isOriginal(Species s) {
        return s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID;
    }
}
