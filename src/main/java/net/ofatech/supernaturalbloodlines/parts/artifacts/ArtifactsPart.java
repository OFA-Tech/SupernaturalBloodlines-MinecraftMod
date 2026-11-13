package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.ArtifactSlotsGameTests;
import net.ofatech.supernaturalbloodlines.parts.artifacts.gametest.CuriosSlotsGameTests;

public final class ArtifactsPart {
    private ArtifactsPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        boolean curios = ModList.get().isLoaded("curios");
        // the Curios tests link Curios classes, so they are registered only when it is loaded
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(ArtifactSlotsGameTests.class);
            if (curios) {
                e.register(CuriosSlotsGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("ArtifactsPart enabled");
    }
}
