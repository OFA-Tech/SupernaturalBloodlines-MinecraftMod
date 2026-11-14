package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.SupernaturalStateGameTests;

public final class SupernaturalPart {
    private SupernaturalPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SupernaturalConfig.SPEC, SupernaturalConfig.FILE);
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(SupernaturalStateGameTests.class));
        SupernaturalBloodlines.LOGGER.info("SupernaturalPart enabled");
    }
}
