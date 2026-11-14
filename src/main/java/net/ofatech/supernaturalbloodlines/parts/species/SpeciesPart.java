package net.ofatech.supernaturalbloodlines.parts.species;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HybridGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HybridTraitsGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.OriginalGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.BloodThirst;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.OriginalHybridGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.FormControl;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.GeneAwakening;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.SireBond;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;
import net.ofatech.supernaturalbloodlines.parts.species.turning.GhoulGates;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.GhoulGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.TurningGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.WerewolfPackGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.WerewolfBiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.WerewolfSparkGameTests;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.ofatech.supernaturalbloodlines.client.PackKeybinds;
import net.ofatech.supernaturalbloodlines.client.RoarClient;
import net.ofatech.supernaturalbloodlines.core.network.RoarPayload;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.PackBonusGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaBite;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Anchors;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Electricity;
import net.ofatech.supernaturalbloodlines.parts.species.pack.FormBlocks;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.EclipseElectricityGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LineageBackfill;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Emissary;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RoarEffectPayload;
import net.ofatech.supernaturalbloodlines.parts.species.pack.DefenderKills;
import net.ofatech.supernaturalbloodlines.parts.species.pack.EyeSync;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LineageRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterInstinctGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterWerewolfGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineHybridGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.WitchGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterEvents;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCompulsionGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SeveranceGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SeveranceRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineDeathGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.BloodlineCommand;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.InfectionTracker;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SiresBlood;
import net.ofatech.supernaturalbloodlines.parts.species.rite.AncestralCallingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.HybridBloodRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.witch.Siphoning;
import net.ofatech.supernaturalbloodlines.parts.species.witch.VampireJoin;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.DownedGiveUpPayload;
import net.ofatech.supernaturalbloodlines.parts.species.original.DownedPayload;
import net.ofatech.supernaturalbloodlines.parts.species.original.DownedScreen;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.rite.OriginalBloodRiteFactory;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SpeciesGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteHooks;

public final class SpeciesPart {
    private static final String NETWORK_VERSION = "1";

    private SpeciesPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SpeciesConfig.SPEC, SpeciesConfig.FILE);
        // the faction rites (vampirism_enchanted part) now run with the real context and through the executor
        FactionRiteHooks.install((p, faction, time) -> SpeciesContext.of(p, faction, time, null), OutcomeExecutor::run);
        BloodlineAccess.addListener((p, change) -> SpeciesData.get(p.server).setOriginal(p.getUUID(), isOriginal(change.after().species())));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                syncOriginal(p);
            }
        });
        // before the datapacks load, so the rite JSONs can name this factory
        RiteFactoryRegistry.get().register(OriginalBloodRiteFactory.ID, OriginalBloodRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(HybridBloodRiteFactory.ID, HybridBloodRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(SeveranceRiteFactory.ID, SeveranceRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(AncestralCallingRiteFactory.ID, AncestralCallingRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(LineageRiteFactory.ID, LineageRiteFactory.CODEC);
        SpeciesRegistries.register(modBus);
        SpeciesSupernatural.register();
        OriginalRules.register();
        Siphoning.register();
        VampireJoin.register();
        HunterEvents.register(); // hunters need no Werewolves
        SireRecorder.register();
        SiresBlood.register();
        InfectionTracker.register();
        SireCall.register();
        Compulsion.register();
        SirelineDeath.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BloodlineCommand.register(e.getDispatcher()));
        HybridTraits.register(); // always: it strips the saved modifier when Werewolves is gone
        RankEffects.register(); // always, for the same reason
        PackBonus.register(); // always, for the same reason
        Electricity.register(); // always: Shocked and the Taser work on every supernatural
        TurningHandler.register(); // always: the turning is vampires only, no Werewolves needed
        GhoulGates.register();
        if (ModList.get().isLoaded("werewolves")) {
            FormBlocks.register();
            LunarEclipse.register();
            Anchors.register();
            AlphaRoar.register();
            Emissary.register();
            TransitionHandler.register();
            GeneAwakening.register();
            GeneHandler.register();
            PackIndex.register();
            LineageBackfill.register();
            AlphaBite.register();
            SparkHandler.register(); // after GeneHandler: an awakening comes before the innocent kill that caused it
            DefenderKills.register();
            EyeSync.register();
            NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> PackCommand.register(e.getDispatcher()));
            FormControl.register();
            BloodThirst.register();
            Venom.register();
            SireBond.register();
        }
        modBus.addListener((RegisterPayloadHandlersEvent e) -> {
            PayloadRegistrar registrar = e.registrar(NETWORK_VERSION).optional();
            registrar.playToClient(DownedPayload.TYPE, DownedPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> DownedScreen.show(payload.downed())));
            registrar.playToServer(DownedGiveUpPayload.TYPE, DownedGiveUpPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer p) {
                    Downed.giveUp(p);
                }
            }));
            // the server decides rank and cooldown; a refusal comes back as an action bar message
            registrar.playToServer(RoarPayload.TYPE, RoarPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer p && ModList.get().isLoaded("werewolves")) {
                    Component error = AlphaRoar.roar(p);
                    if (error != null) {
                        p.displayClientMessage(error, true);
                    }
                }
            }));
            registrar.playToClient(RoarEffectPayload.TYPE, RoarEffectPayload.STREAM_CODEC, (payload, ctx) ->
                    ctx.enqueueWork(() -> RoarClient.show(payload.playerId(), payload.ticks())));
        });
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("werewolves")) {
            PackKeybinds.register(modBus);
        }
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(SpeciesGameTests.class);
            e.register(OriginalGameTests.class);
            e.register(WitchGameTests.class);
            e.register(HunterGameTests.class);
            e.register(HunterMarkGameTests.class);
            e.register(HunterInstinctGameTests.class);
            e.register(SirelineGameTests.class);
            e.register(SirelineCommandGameTests.class);
            e.register(SirelineCompulsionGameTests.class);
            e.register(SirelineDeathGameTests.class);
            e.register(SeveranceGameTests.class);
            e.register(SeveranceRiteGameTests.class);
            e.register(TurningGameTests.class);
            e.register(GhoulGameTests.class);
            if (ModList.get().isLoaded("werewolves")) {
                e.register(SirelineHybridGameTests.class);
                e.register(HunterWerewolfGameTests.class);
                e.register(HybridGameTests.class);
                e.register(HybridTraitsGameTests.class);
                e.register(OriginalHybridGameTests.class);
                e.register(WerewolfPackGameTests.class);
                e.register(WerewolfBiteGameTests.class);
                e.register(WerewolfSparkGameTests.class);
                e.register(PackBonusGameTests.class);
                e.register(EclipseElectricityGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("SpeciesPart enabled");
    }

    /** Idempotent: heals SpeciesData if it ever missed a change (lost file, state set while the part was off). */
    public static void syncOriginal(ServerPlayer p) {
        SpeciesData.get(p.server).setOriginal(p.getUUID(), isOriginal(BloodlineAccess.get(p).species()));
    }

    private static boolean isOriginal(Species s) {
        return s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID;
    }
}
