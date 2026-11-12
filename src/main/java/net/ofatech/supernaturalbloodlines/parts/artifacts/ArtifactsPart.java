package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class ArtifactsPart {
    private ArtifactsPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        SupernaturalBloodlines.LOGGER.info("ArtifactsPart enabled");
    }
}
