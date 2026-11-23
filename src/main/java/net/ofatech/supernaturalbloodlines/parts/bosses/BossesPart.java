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
        // before the datapacks load, so the rite JSONs can name these factories
        net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry.get().register(net.ofatech.supernaturalbloodlines.parts.bosses.rite.CloakingRiteFactory.ID,
                net.ofatech.supernaturalbloodlines.parts.bosses.rite.CloakingRiteFactory.CODEC);
        net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry.get().register(net.ofatech.supernaturalbloodlines.parts.bosses.rite.LinkingRiteFactory.ID,
                net.ofatech.supernaturalbloodlines.parts.bosses.rite.LinkingRiteFactory.CODEC);
        net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry.get().register(net.ofatech.supernaturalbloodlines.parts.bosses.rite.PikeRiteFactory.ID,
                net.ofatech.supernaturalbloodlines.parts.bosses.rite.PikeRiteFactory.CODEC);
        net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry.get().register(net.ofatech.supernaturalbloodlines.parts.bosses.rite.FrequencyRiteFactory.ID,
                net.ofatech.supernaturalbloodlines.parts.bosses.rite.FrequencyRiteFactory.CODEC);
        net.ofatech.supernaturalbloodlines.parts.bosses.link.Links.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.item.CloakEvents.register();
        BossRegistries.register(modBus);
        net.ofatech.supernaturalbloodlines.parts.bosses.world.HaleHouse.register(modBus);
        BossItems.register(modBus);
        // R158: a mob's white oak never counts as a white oak kill; Mikael's does only when the config says so
        net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules.mobWhiteOakCounts(a -> a instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity && BossesConfig.mikaelStakesOriginals());
        NpcOriginalEvents.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEvents.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterEvents.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEvents.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.item.MantleEvents.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.deal.BossEvents17d.register();
        net.ofatech.supernaturalbloodlines.parts.bosses.RiteLoreBackfill.register();
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> {
            var registrar = e.registrar("1").optional();
            registrar.playToClient(net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload.TYPE, net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealClientHook.open(payload)));
            registrar.playToServer(net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealChoicePayload.TYPE, net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealChoicePayload.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer p) {
                            net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals.choose(p, payload.entityId(), payload.original());
                        }
                    }));
        });
        BossDirector.register();
        BossBars.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BossCommand.register(e.getDispatcher()));
        WorldNowHook.setBossNear(BossDirector::near);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(BossRenderers::register);
            net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealClientHook.install(net.ofatech.supernaturalbloodlines.parts.bosses.client.ElijahDealScreen::open);
            modBus.addListener((FMLClientSetupEvent e) -> BossShots.registerIfRequested());
        }
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(BossServerGameTests.class);
            e.register(NpcOriginalGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.DesiccatedHitboxGameTests.class);
            e.register(MikaelGameTests.class);
            e.register(BossReviewGameTests.class);
            e.register(KlausGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.DeucalionGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.PeterGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.BeastGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.ElijahGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.RebekahGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.ElixirGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.KolGameTests.class);
        e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.GrimoireLoreGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.FinnGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.CloakLinkGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.bosses.gametest.MorphGameTests.class);
        });
        SupernaturalBloodlines.LOGGER.info("BossesPart enabled");
    }
}
