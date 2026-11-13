package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.favouriteless.enchanted.api.circle_magic.RiteFactoryRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.ArtifactSlotsGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.CuriosSlotsGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.MoonRingGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.RingGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.BindItemRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.MoonRing;

public final class ArtifactsPart {
    private ArtifactsPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        boolean curios = ModList.get().isLoaded("curios");
        boolean werewolves = ModList.get().isLoaded("werewolves");
        // before the datapacks load, so the rite JSONs can name this factory
        RiteFactoryRegistry.get().register(BindItemRiteFactory.ID, BindItemRiteFactory.CODEC);
        ArtifactItems.register(modBus);
        SunRing.register();
        if (werewolves) {
            MoonRing.register();
        }
        // the Curios and Werewolves tests link those mods, so they are registered only when loaded
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(ArtifactSlotsGameTests.class);
            e.register(RingGameTests.class);
            if (curios) {
                e.register(CuriosSlotsGameTests.class);
            }
            if (werewolves) {
                e.register(MoonRingGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("ArtifactsPart enabled");
    }
}
