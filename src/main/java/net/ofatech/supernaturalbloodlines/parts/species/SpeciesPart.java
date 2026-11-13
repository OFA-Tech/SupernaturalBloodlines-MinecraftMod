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
import net.ofatech.supernaturalbloodlines.parts.species.rite.HybridBloodRiteFactory;
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
        SpeciesRegistries.register(modBus);
        OriginalRules.register();
        HybridTraits.register(); // always: it strips the saved modifier when Werewolves is gone
        if (ModList.get().isLoaded("werewolves")) {
            TransitionHandler.register();
            GeneAwakening.register();
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
        });
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(SpeciesGameTests.class);
            e.register(OriginalGameTests.class);
            if (ModList.get().isLoaded("werewolves")) {
                e.register(HybridGameTests.class);
                e.register(HybridTraitsGameTests.class);
                e.register(OriginalHybridGameTests.class);
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
