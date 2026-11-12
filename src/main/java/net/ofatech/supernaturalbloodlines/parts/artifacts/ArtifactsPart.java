package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.neoforged.bus.api.IEventBus;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class ArtifactsPart {
    private ArtifactsPart() {}

    public static void init(IEventBus modBus) {
        SupernaturalBloodlines.LOGGER.info("ArtifactsPart enabled");
    }
}
