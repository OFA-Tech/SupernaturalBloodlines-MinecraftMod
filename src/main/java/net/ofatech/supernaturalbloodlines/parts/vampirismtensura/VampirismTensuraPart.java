package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import net.neoforged.bus.api.IEventBus;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class VampirismTensuraPart {
    private VampirismTensuraPart() {}

    public static void init(IEventBus modBus) {
        SupernaturalBloodlines.LOGGER.info("VampirismTensuraPart enabled");
    }
}
