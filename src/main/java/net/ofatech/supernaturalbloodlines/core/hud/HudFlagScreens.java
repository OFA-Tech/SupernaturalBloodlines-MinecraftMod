package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * The one screen that gets Tensura's HUD flags back. Root cause of "Tensura's skill HUD comes back in the Status Menu": the flags used to be handed back
 * while <i>any</i> {@code io.github.manasmods.*} screen was open, so {@code MainScreen} (the Status Menu) and every other Tensura screen raised
 * {@code abilities.render} again, and {@code OverlayHandler.pre_renderAbilities} drew its three big icons under them. javap of Tensura 2.0.1.5: the only
 * classes that save {@code HudConfig} or draw its preview are {@code SettingsScreen} (and {@code SettingsOptions}, which it calls); no other Tensura or
 * ManasCore screen references {@code OverlayHandler}, {@code saveAll}, {@code savePositionById} or {@code saveScaleById}, and ManasCore calls
 * {@code saveAllConfigs} from nowhere. So only that screen needs the player's real values. Pure.
 */
public final class HudFlagScreens {
    public static final String SETTINGS = "io.github.manasmods.tensura.client.screen.SettingsScreen";

    private HudFlagScreens() {
    }

    /** True for Tensura's HUD settings screen and its inner classes, false for every other screen. */
    public static boolean handsBack(String screenClassName) {
        return SETTINGS.equals(screenClassName) || screenClassName.startsWith(SETTINGS + "$");
    }
}
