package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse.Curses;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.EnchantedTensuraDataGameTests;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.EnchantedTensuraGameTests;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.EnchantedTensuraPoppetGameTests;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.EnchantedTensuraRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.rite.MagiculeOfferingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.rite.PurificationRiteFactory;

public final class EnchantedTensuraPart {
    private EnchantedTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // the part is only initialised when Enchanted and Tensura are present, so the tests only exist then
        Curses.register();
        // before the datapacks load, so the rite JSONs can name these factories
        RiteFactoryRegistry.get().register(PurificationRiteFactory.ID, PurificationRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(MagiculeOfferingRiteFactory.ID, MagiculeOfferingRiteFactory.CODEC);
        PartBlocks.register(modBus);
        PartItems.register(modBus);
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(EnchantedTensuraDataGameTests.class);
            e.register(EnchantedTensuraGameTests.class);
            e.register(EnchantedTensuraRiteGameTests.class);
            e.register(EnchantedTensuraPoppetGameTests.class);
        });
        SupernaturalBloodlines.LOGGER.info("EnchantedTensuraPart enabled");
    }
}
