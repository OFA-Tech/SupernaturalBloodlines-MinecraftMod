package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.ArtifactSlotsGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.AshDaggerGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.CompassHearingGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.CuriosSlotsGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.MoonRingGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.MountainAshGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.MountainAshWerewolfGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.ResurrectionVervainGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.RingGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.WolfsbaneGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.BindItemRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.RequiresDesiccatedRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.MoonRing;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneRules;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains;

public final class ArtifactsPart {
    private ArtifactsPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        boolean curios = ModList.get().isLoaded("curios");
        boolean werewolves = ModList.get().isLoaded("werewolves");
        // before the datapacks load, so the rite JSONs can name this factory
        RiteFactoryRegistry.get().register(BindItemRiteFactory.ID, BindItemRiteFactory.CODEC);
        RiteFactoryRegistry.get().register(RequiresDesiccatedRiteFactory.ID, RequiresDesiccatedRiteFactory.CODEC);
        ArtifactItems.register(modBus);
        MountainAsh.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(ArtifactsClient::onClientSetup);
        }
        AshDagger.register();
        SunRing.register();
        ResurrectionRing.register();
        VervainRules.register();
        if (werewolves) {
            MoonRing.register();
            WolfsbaneStrains.register(modBus);
            WolfsbaneRules.register(modBus);
        }
        // the Curios and Werewolves tests link those mods, so they are registered only when loaded
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(ArtifactSlotsGameTests.class);
            e.register(AshDaggerGameTests.class);
            e.register(RingGameTests.class);
            e.register(ResurrectionVervainGameTests.class);
            e.register(CompassHearingGameTests.class);
            e.register(MountainAshGameTests.class);
            if (curios) {
                e.register(CuriosSlotsGameTests.class);
            }
            if (werewolves) {
                e.register(MountainAshWerewolfGameTests.class);
                e.register(MoonRingGameTests.class);
                e.register(WolfsbaneGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("ArtifactsPart enabled");
    }
}
