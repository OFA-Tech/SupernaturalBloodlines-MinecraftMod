package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionHookProbeGameTests;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionUnlockGameTests;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.MasteryGameTests;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryCommand;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.EvolutionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;

/**
 * The evolution rites (plan 19): a witch-type performer opens a road to a skill that already exists, on a full moon or an eclipse, and the Rite of
 * Mastery. No compile dependency on any addon, no mixin: evolutionRites 0 of 3.
 */
public final class EvolutionRitesPart {
    private EvolutionRitesPart() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, EvolutionRitesConfig.SPEC, EvolutionRitesConfig.FILE);
        AdminSpecs.register(EvolutionRitesConfig.FILE, EvolutionRitesConfig.SPEC);
        // before the datapacks load, so the rite JSONs can name these factories
        RiteFactoryRegistry.get().register(EvolutionRiteFactory.ID, EvolutionRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(MasteryRiteFactory.ID, MasteryRiteFactory.CODEC);
        // last, so the species part's /sb bloodline redirect exists when the node is added to its target
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (RegisterCommandsEvent e) -> MasteryCommand.register(e.getDispatcher()));
        net.ofatech.supernaturalbloodlines.parts.evolutionrites.merge.RaphaelMerge.register();
        net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable.dormant = rite -> rite.equals("evolve_raphael")
                && !net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Probes.of(net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Probes.GREAT_SAGE_RAPHAEL)
                        .map(net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AddonProbe::roadOpen).orElse(false);
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(EvolutionHookProbeGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.FreeGrantGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.RaphaelProbeGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.RaphaelMergeGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.RaphaelMergeAddonGameTests.class);
            e.register(EvolutionRiteGameTests.class);
            e.register(EvolutionUnlockGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionAscensionGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionOthersGameTests.class);
            e.register(MasteryGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.CasterLeavesGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvolutionCommandGameTests.class);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.ofatech.supernaturalbloodlines.parts.evolutionrites.client.MasteryClient.install();
        }
        SupernaturalBloodlines.LOGGER.info("EvolutionRitesPart enabled");
    }
}
