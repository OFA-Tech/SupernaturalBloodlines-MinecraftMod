package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.VampirismEnchantedDataGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.VampirismEnchantedGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.VampirismEnchantedWerewolfGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

public final class VampirismEnchantedPart {
    private VampirismEnchantedPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // before the datapacks load, so the rite JSONs can name this factory
        RiteFactoryRegistry.get().register(FactionRiteFactory.ID, FactionRiteFactory.CODEC);
        boolean werewolves = ModList.get().isLoaded("werewolves");
        // the part is only initialised when Vampirism and Enchanted are present, so the tests only exist then;
        // the Werewolves tests link Werewolves classes, so they are registered only when it is loaded
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(VampirismEnchantedDataGameTests.class);
            e.register(VampirismEnchantedGameTests.class);
            if (werewolves) {
                e.register(VampirismEnchantedWerewolfGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("VampirismEnchantedPart enabled");
    }
}
