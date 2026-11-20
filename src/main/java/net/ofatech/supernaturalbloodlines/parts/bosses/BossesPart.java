package net.ofatech.supernaturalbloodlines.parts.bosses;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossRenderers;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossShots;
import net.ofatech.supernaturalbloodlines.parts.bosses.command.BossCommand;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalEvents;
import net.ofatech.supernaturalbloodlines.parts.bosses.gametest.BossReviewGameTests;
import net.ofatech.supernaturalbloodlines.parts.bosses.gametest.BossServerGameTests;
import net.ofatech.supernaturalbloodlines.parts.bosses.gametest.KlausGameTests;
import net.ofatech.supernaturalbloodlines.parts.bosses.gametest.MikaelGameTests;
import net.ofatech.supernaturalbloodlines.parts.bosses.gametest.NpcOriginalGameTests;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;

/**
 * The bosses (plan 17): the first entity types of the mod, the NPC Original base and Mikael in this phase. Needs the species part (the lines the NPC
 * Originals root) and GeckoLib (which Enchanted already requires, so it costs nobody anything). No mixin: bosses 0 of 3.
 */
public final class BossesPart {
    private BossesPart() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, BossesConfig.SPEC, BossesConfig.FILE);
        AdminSpecs.register(BossesConfig.FILE, BossesConfig.SPEC);
        AdminSpecs.choices(BossesConfig.FILE + ":boss_reward_on", BossesConfig.rewardChoices());
        BossRegistries.register(modBus);
        BossItems.register(modBus);
        // R158: a mob's white oak never counts as a white oak kill; Mikael's does only when the config says so
        net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules.mobWhiteOakCounts(a -> a instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity && BossesConfig.mikaelStakesOriginals());
        NpcOriginalEvents.register();
        BossDirector.register();
        BossBars.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BossCommand.register(e.getDispatcher()));
        WorldNowHook.setBossNear(BossDirector::near);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(BossRenderers::register);
            modBus.addListener((FMLClientSetupEvent e) -> BossShots.registerIfRequested());
        }
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(BossServerGameTests.class);
            e.register(NpcOriginalGameTests.class);
            e.register(MikaelGameTests.class);
            e.register(BossReviewGameTests.class);
            e.register(KlausGameTests.class);
        });
        SupernaturalBloodlines.LOGGER.info("BossesPart enabled");
    }
}
