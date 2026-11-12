package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.neoforged.bus.api.IEventBus;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SpeciesTensuraPart {
    private SpeciesTensuraPart() {}

    public static void init(IEventBus modBus) {
        SupernaturalBloodlines.LOGGER.info("SpeciesTensuraPart enabled");
    }
}
