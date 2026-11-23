package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import io.github.manasmods.tensura.handler.client.OverlayHandler;
import net.ofatech.supernaturalbloodlines.core.wheel.LowerWhileOpen;

/**
 * Tensura draws its status, bars and skill HUD from its own mixin into {@code Gui} ({@code OverlayHandler.renderHud}), not as a layer, so a layer event cannot hide them. Its
 * own public switch {@code setShouldHudRender} can: lowered while the wheel is open, given back as it was. Linked to Tensura: reached only after
 * {@code ModList.isLoaded("tensura")}.
 */
final class TensuraHudHold {
    private static final LowerWhileOpen HOLD = new LowerWhileOpen();

    private TensuraHudHold() {
    }

    static void open() {
        HOLD.open(OverlayHandler.isShouldHudRender(), false).ifPresent(OverlayHandler::setShouldHudRender);
    }

    static void close() {
        HOLD.close(OverlayHandler.isShouldHudRender(), false).ifPresent(OverlayHandler::setShouldHudRender);
    }
}
