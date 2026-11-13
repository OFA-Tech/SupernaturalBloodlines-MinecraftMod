package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.VampirismEnchantedDataGameTests;

public final class VampirismEnchantedPart {
    private VampirismEnchantedPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        // the part is only initialised when Vampirism and Enchanted are present, so the tests only exist then
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(VampirismEnchantedDataGameTests.class));
        SupernaturalBloodlines.LOGGER.info("VampirismEnchantedPart enabled");
    }
}
