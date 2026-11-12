package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class VampirismTensuraPart {
    private VampirismTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VampireSyncConfig.SPEC, VampireSyncConfig.FILE);
        SupernaturalBloodlines.LOGGER.info("VampirismTensuraPart enabled");
    }
}
