package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.TensuraHudFlags;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;

/** Client registrations of the HUD part. Called only from {@code HudPart} behind a {@code Dist.CLIENT} check. */
public final class HudClient {
    private HudClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> HudKeys.register(e));
        LoadoutAccess.install(new ClientLoadout());
        HudSlots.register();
        HudInput.register();
        if (ModList.get().isLoaded("tensura")) {
            // nothing is hidden until the HUD layer of 13b Task 4 says that ours is on screen
            TensuraHudFlags.register(() -> false);
        }
        KeyCover.register();
        KeyHiding.register();
    }
}
