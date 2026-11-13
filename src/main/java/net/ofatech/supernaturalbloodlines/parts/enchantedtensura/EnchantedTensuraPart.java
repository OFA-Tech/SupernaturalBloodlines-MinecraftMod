package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.EnchantedTensuraDataGameTests;

public final class EnchantedTensuraPart {
    private EnchantedTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // the part is only initialised when Enchanted and Tensura are present, so the tests only exist then
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(EnchantedTensuraDataGameTests.class));
        SupernaturalBloodlines.LOGGER.info("EnchantedTensuraPart enabled");
    }
}
