package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client only: load it only when {@code FMLEnvironment.dist} is CLIENT. */
final class ArtifactsClient {
    private ArtifactsClient() {
    }

    /** Vanilla gives the {@code angle} property to its own compasses only; ours reads the same tracker the server fills. */
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ArtifactItems.VAMPIRE_COMPASS.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    // no tracker, or an empty one, is no target: the needle spins
                    return tracker == null ? null : tracker.target().orElse(null);
                })));
    }
}
