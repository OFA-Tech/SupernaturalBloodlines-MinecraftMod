package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class EnchantedTensuraPart {
    private EnchantedTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        SupernaturalBloodlines.LOGGER.info("EnchantedTensuraPart enabled");
    }
}
