package net.ofatech.supernaturalbloodlines.parts.hud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.parts.hud.gametest.BiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.hud.gametest.BiteHybridGameTests;
import net.ofatech.supernaturalbloodlines.parts.hud.gametest.BiteWerewolfGameTests;
import net.ofatech.supernaturalbloodlines.parts.hud.gametest.TurningBiteGameTests;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClient;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;

/** The HUD part: no required mods. Tensura, Vampirism and Werewolves are optional extras reached through adapters after {@code ModList.isLoaded}. */
public final class HudPart {
    private HudPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, HudConfig.SPEC, HudConfig.FILE);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerConfig(ModConfig.Type.CLIENT, HudClientConfig.SPEC, HudClientConfig.FILE);
            HudClient.register(modBus);
        }
        BiteService.register();
        modBus.addListener((RegisterGameTestsEvent e) -> {
            if (ModList.get().isLoaded("vampirism")) {
                e.register(BiteGameTests.class);
                if (EnabledParts.isEnabled("species")) {
                    e.register(TurningBiteGameTests.class);
                }
                if (EnabledParts.isEnabled("species") && ModList.get().isLoaded("werewolves")) {
                    e.register(BiteHybridGameTests.class);
                }
                if (ModList.get().isLoaded("werewolves")) {
                    e.register(BiteWerewolfGameTests.class);
                }
            }
        });
        SupernaturalBloodlines.LOGGER.info("HudPart enabled");
    }
}
