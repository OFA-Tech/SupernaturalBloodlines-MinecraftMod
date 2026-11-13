package net.ofatech.supernaturalbloodlines.parts.species;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
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
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(SpeciesGameTests.class));
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
