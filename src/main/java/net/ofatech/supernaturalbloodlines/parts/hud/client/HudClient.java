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
        modBus.addListener(AbilityHudLayer::register);
        LoadoutAccess.install(new ClientLoadout());
        HudSlots.register();
        HudInput.register();
        if (ModList.get().isLoaded("tensura")) {
            TensuraHudFlags.register(AbilityHudLayer::shown);
        }
        KeyCover.register();
        KeyHiding.register();
    }
}
