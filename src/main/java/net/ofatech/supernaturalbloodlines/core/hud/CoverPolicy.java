package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * When the HUD may take other mods' keys (13d H3). A key is covered only while the HUD can really take over: the HUD is on, the player asked for it
 * ({@code hide_covered_keys}), a world is open and its server has the HUD's channel. Anything else (the title screen, a server without this mod or
 * with the hud part off, {@code hud_enabled = false}) gives every recorded key back, so a player is never left without their keys. Pure.
 */
public final class CoverPolicy {
    private CoverPolicy() {
    }

    public static boolean cover(boolean hudEnabled, boolean hideCoveredKeys, boolean inWorld, boolean serverHasHud) {
        return hudEnabled && hideCoveredKeys && inWorld && serverHasHud;
    }
}
