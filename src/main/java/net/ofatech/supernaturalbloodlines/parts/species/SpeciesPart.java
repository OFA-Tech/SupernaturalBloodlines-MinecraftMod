package net.ofatech.supernaturalbloodlines.parts.species;

import net.neoforged.bus.api.IEventBus;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SpeciesPart {
    private SpeciesPart() {}

    public static void init(IEventBus modBus) {
        SupernaturalBloodlines.LOGGER.info("SpeciesPart enabled");
    }
}
