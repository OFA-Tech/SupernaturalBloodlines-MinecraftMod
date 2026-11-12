package net.ofatech.supernaturalbloodlines.parts.species;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SpeciesPart {
    private SpeciesPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        SupernaturalBloodlines.LOGGER.info("SpeciesPart enabled");
    }
}
