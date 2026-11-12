package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest.VampirismTensuraGameTests;

public final class VampirismTensuraPart {
    private VampirismTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VampireSyncConfig.SPEC, VampireSyncConfig.FILE);
        VampireSync.register();
        // the part is only initialised when Vampirism and Tensura are present, so the tests only exist then
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(VampirismTensuraGameTests.class));
        SupernaturalBloodlines.LOGGER.info("VampirismTensuraPart enabled");
    }
}
