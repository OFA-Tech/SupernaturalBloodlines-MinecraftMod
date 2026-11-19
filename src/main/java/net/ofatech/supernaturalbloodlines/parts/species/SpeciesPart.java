package net.ofatech.supernaturalbloodlines.parts.species;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.WerewolfSkillRuleGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.SkillRuleGameTests;
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
import net.ofatech.supernaturalbloodlines.parts.species.gametest.TurningWerewolfGameTests;
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
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineDeathGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.line.NpcLines;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.BloodlineCommand;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.InfectionTracker;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SiresBlood;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerGene;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerVillagers;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.DoppelgangerRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.DoppelgangerVillagerGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.GeneGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.GeneWerewolfGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.RevelationGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.rite.DoppelgangerBloodRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.RevelationRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.AncestralCallingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.HybridBloodRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.BrokenLineRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.ImprisonedOriginalRiteFactory;
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
        LegacyConfig.register(); // reads the file before NeoForge loads it
        container.registerConfig(ModConfig.Type.COMMON, SpeciesConfig.SPEC, SpeciesConfig.FILE);
        AdminSpecs.register(SpeciesConfig.FILE, SpeciesConfig.SPEC);
        net.ofatech.supernaturalbloodlines.parts.species.profile.SpeciesProfiles.register();
        AdminSpecs.choices(SpeciesConfig.FILE + ":species.original_visibility", List.of(SpeciesConfig.LINE_DISCOVERED, SpeciesConfig.LINE_ALWAYS));
        AdminSpecs.choices(SpeciesConfig.FILE + ":species.gene_visibility", List.of(SpeciesConfig.GENES_DISCOVERED, SpeciesConfig.GENES_ALWAYS));
        AdminSpecs.choices(SpeciesConfig.FILE + ":species.doppelganger_rite_taglock", List.of(SpeciesConfig.TAGLOCK_DOPPELGANGER, SpeciesConfig.TAGLOCK_HUMAN));
        AdminSpecs.choices(SpeciesConfig.FILE + ":species.hybrid_transition_blood", List.of(SpeciesConfig.TRANSITION_BLOOD_ORIGINAL_HYBRID, SpeciesConfig.TRANSITION_BLOOD_ORIGINAL));
        AdminSpecs.choices(SpeciesConfig.FILE + ":species.full_moon_sleep", List.of(SpeciesConfig.SLEEP_OFF, SpeciesConfig.SLEEP_WEREWOLVES, SpeciesConfig.SLEEP_EVERYONE));
        AdminSpecs.lineCheck(SpeciesConfig.FILE + ":species.npc_line_weights", line -> !LineRules.parseWeights(List.of(line)).isEmpty());
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
        RiteFactoryRegistry.get().register(BrokenLineRiteFactory.ID, BrokenLineRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(ImprisonedOriginalRiteFactory.ID, ImprisonedOriginalRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(AncestralCallingRiteFactory.ID, AncestralCallingRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(LineageRiteFactory.ID, LineageRiteFactory.CODEC);
        if (ModList.get().isLoaded("werewolves")) {
            RiteFactoryRegistry.get().register(net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory.ID, net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory.CODEC);
        }
        RiteFactoryRegistry.get().register(DoppelgangerBloodRiteFactory.ID, DoppelgangerBloodRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(RevelationRiteFactory.ID, RevelationRiteFactory.CODEC);
        BloodlineAccess.useGeneVisibility(SpeciesConfig::genesAlwaysVisible);
        DoppelgangerGene.register(); // genes and doppelgänger villagers need no Werewolves
        DoppelgangerVillagers.register();
        net.ofatech.supernaturalbloodlines.shared.species.DoppelgangerIndex.set(DoppelgangerVillagers::loaded); // for the Vampire Compass
        SpeciesRegistries.register(modBus);
        SpeciesSupernatural.register();
        // what the supernatural part's tasks need from here, through shared hooks
        net.ofatech.supernaturalbloodlines.shared.supernatural.HumanLikeBlood.set(TurningHandler::isHumanLikeBlood);
        net.ofatech.supernaturalbloodlines.shared.supernatural.TurningFeed.set(
                p -> BloodlineAccess.get(p).inTurning() && TurningHandler.wouldComplete(p), TurningHandler::drankHumanBlood);
        net.ofatech.supernaturalbloodlines.shared.supernatural.HybridFeed.set(
                p -> HybridTraits.isHybrid(BloodlineAccess.get(p).species()) && ModList.get().isLoaded("werewolves")
                        && net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural.isWerewolf(p),
                p -> BloodThirst.addThirst(p, BloodThirst.GAIN));
        net.ofatech.supernaturalbloodlines.shared.supernatural.SireCount.set(
                p -> net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex.get(p.serverLevel()).childrenOf(p.getUUID()).size());
        SpeciesHub.register();
        OriginalRules.register();
        Siphoning.register();
        VampireJoin.register();
        HunterEvents.register(); // hunters need no Werewolves
        SireRecorder.register();
        SiresBlood.register();
        InfectionTracker.register();
        SireCall.register();
        Compulsion.register();
        Lines.register(); // after SireRecorder: it hears the sires SireRecorder writes down
        NpcLines.register();
        LineDeath.register();
        LineWarning.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BloodlineCommand.register(e.getDispatcher()));
        HybridTraits.register(); // always: it strips the saved modifier when Werewolves is gone
        RankEffects.register(); // always, for the same reason
        PackBonus.register(); // always, for the same reason
        Electricity.register(); // always: Shocked and the Taser work on every supernatural
        TurningHandler.register(); // always: the turning is vampires only, no Werewolves needed
        GhoulGates.register();
        net.ofatech.supernaturalbloodlines.parts.species.moon.FullMoonSleep.register(); // no Werewolves needed: "everyone" works without it
        net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.set(p -> new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(
                net.ofatech.supernaturalbloodlines.core.moon.SleepRules.appliesTo(SpeciesConfig.fullMoonSleep(),
                        net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural.isWerewolf(p)),
                net.ofatech.supernaturalbloodlines.parts.species.moon.AwakeningQueue.pending(p)));
        if (ModList.get().isLoaded("werewolves")) {
            net.ofatech.supernaturalbloodlines.parts.species.moon.DelayedAwakening.register();
            FormBlocks.register();
            LunarEclipse.register();
            Anchors.register();
            AlphaRoar.register();
            Emissary.register();
            TransitionHandler.register();
            GeneAwakening.register();
            GeneHandler.register();
            PackIndex.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.AllyProtection.register();
            LineageBackfill.register();
            AlphaBite.register();
            SparkHandler.register(); // after GeneHandler: an awakening comes before the innocent kill that caused it
            DefenderKills.register();
            EyeSync.register();
            // plan 15b: the NPC packs, the weaker spark and the two pack abilities
            net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.register();
            net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackBrain.register();
            net.ofatech.supernaturalbloodlines.parts.species.npc.WeakSpark.register();
            net.ofatech.supernaturalbloodlines.parts.species.npc.PackSkills.register();
            net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialBoss.register();
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory.LEDGER.clear());
            NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackCommand.register(e.getDispatcher()));
            NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> PackCommand.register(e.getDispatcher()));
            NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> PackCommand.forget(e.getEntity().getUUID()));
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
            // the Alpha Trial warning: the server sends it when the rite was refused for an open True Alpha path; the answer comes back as a confirm
            registrar.playToClient(net.ofatech.supernaturalbloodlines.core.network.TrialWarnPayload.TYPE, net.ofatech.supernaturalbloodlines.core.network.TrialWarnPayload.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(net.ofatech.supernaturalbloodlines.shared.hub.TrialWarnHook::fire));
            registrar.playToServer(net.ofatech.supernaturalbloodlines.core.network.TrialConfirmPayload.TYPE, net.ofatech.supernaturalbloodlines.core.network.TrialConfirmPayload.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (ctx.player() instanceof ServerPlayer p && ModList.get().isLoaded("werewolves")) {
                            net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory.answer(p);
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
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.CommandAliasGameTests.class);
            e.register(HunterGameTests.class);
            e.register(HunterMarkGameTests.class);
            e.register(HunterInstinctGameTests.class);
            e.register(SirelineGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.SpeciesHubGameTests.class);
            e.register(SirelineCommandGameTests.class);
            e.register(SirelineCompulsionGameTests.class);
            e.register(LineGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.LineHubGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.LineRiteGameTests.class);
            // the ring tests need the artifacts part's mountain ash block, found by id: the species part does not import it
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.ofatech.supernaturalbloodlines.parts.species.line.Imprisonment.RING_BLOCK)) {
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.LineRiteRingGameTests.class);
            }
            e.register(LineDeathGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.LineDiscoveryGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.LineWarningGameTests.class);
            e.register(SeveranceGameTests.class);
            e.register(SeveranceRiteGameTests.class);
            e.register(TurningGameTests.class);
            e.register(GhoulGameTests.class);
            e.register(GeneGameTests.class);
            e.register(DoppelgangerVillagerGameTests.class);
            e.register(RevelationGameTests.class);
            if (net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
                e.register(SkillRuleGameTests.class);
                if (ModList.get().isLoaded("werewolves")) {
                    e.register(WerewolfSkillRuleGameTests.class);
                }
            }
            e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.FullMoonSleepGameTests.class);
            if (ModList.get().isLoaded("werewolves")) {
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.MoonWerewolfGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.NpcPackGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.AlphaTrialGameTests.class);
                e.register(GeneWerewolfGameTests.class);
                e.register(DoppelgangerRiteGameTests.class);
                e.register(SirelineHybridGameTests.class);
                e.register(HunterWerewolfGameTests.class);
                e.register(HybridGameTests.class);
                e.register(TurningWerewolfGameTests.class);
                e.register(HybridTraitsGameTests.class);
                e.register(OriginalHybridGameTests.class);
                e.register(WerewolfPackGameTests.class);
                e.register(WerewolfBiteGameTests.class);
                e.register(WerewolfSparkGameTests.class);
                e.register(PackBonusGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.AlphaPackGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.PackReviewGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.SparkRulesGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.PackDuelGameTests.class);
                e.register(net.ofatech.supernaturalbloodlines.parts.species.gametest.FreeWillGameTests.class);
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
