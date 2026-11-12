package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SpeciesTensuraPart {
    private SpeciesTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        SupernaturalBloodlines.LOGGER.info("SpeciesTensuraPart enabled");
    }
}
