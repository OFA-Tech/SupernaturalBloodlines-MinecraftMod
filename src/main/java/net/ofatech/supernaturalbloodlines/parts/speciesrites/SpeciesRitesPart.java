package net.ofatech.supernaturalbloodlines.parts.speciesrites;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SpeciesRitesPart {
    private SpeciesRitesPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        if (!ModList.get().isLoaded("enchanted")) {
            return;
        }
        SupernaturalBloodlines.LOGGER.info("SpeciesRitesPart enabled");
    }
}