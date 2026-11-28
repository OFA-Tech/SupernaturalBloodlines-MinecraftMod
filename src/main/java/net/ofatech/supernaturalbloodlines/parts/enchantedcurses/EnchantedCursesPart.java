package net.ofatech.supernaturalbloodlines.parts.enchantedcurses;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class EnchantedCursesPart {
    private EnchantedCursesPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        if (!ModList.get().isLoaded("enchanted")) {
            return;
        }
        PlagueCurses.register();
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.PlagueCurseGameTests.class);
        });
        SupernaturalBloodlines.LOGGER.info("EnchantedCursesPart enabled");
    }
}