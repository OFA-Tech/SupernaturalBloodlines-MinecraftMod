package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class VampirismEnchantedPart {
    private VampirismEnchantedPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        SupernaturalBloodlines.LOGGER.info("VampirismEnchantedPart enabled");
    }
}
