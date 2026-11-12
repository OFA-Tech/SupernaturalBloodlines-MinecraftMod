package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.neoforged.bus.api.IEventBus;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class VampirismEnchantedPart {
    private VampirismEnchantedPart() {}

    public static void init(IEventBus modBus) {
        SupernaturalBloodlines.LOGGER.info("VampirismEnchantedPart enabled");
    }
}
