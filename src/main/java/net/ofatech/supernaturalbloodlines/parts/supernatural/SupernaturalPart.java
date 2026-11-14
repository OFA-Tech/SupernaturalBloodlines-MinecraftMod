package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.DetectorGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.LadderDataGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.SupernaturalStateGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.Detectors;

public final class SupernaturalPart {
    private SupernaturalPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SupernaturalConfig.SPEC, SupernaturalConfig.FILE);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new LadderReloadListener()));
        Detectors.register();
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(SupernaturalStateGameTests.class);
            e.register(LadderDataGameTests.class);
            e.register(DetectorGameTests.class);
        });
        SupernaturalBloodlines.LOGGER.info("SupernaturalPart enabled");
    }
}
