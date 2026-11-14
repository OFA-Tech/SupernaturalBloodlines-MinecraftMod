package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class SupernaturalTensuraPart {
    private SupernaturalTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        TensuraEpReward.register();
        SupernaturalBloodlines.LOGGER.info("SupernaturalTensuraPart enabled");
    }
}
