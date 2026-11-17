package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.List;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * The CLIENT config keys: each player's own, never sent to the server. They belong on the Settings page, not the Admin page.
 * {@code AdminCoverageTest} fails the build for a CLIENT key that is not listed here.
 */
public final class PlayerSettings {
    public static final String HUB_FILE = SupernaturalBloodlines.MODID + "-hub-client.toml";
    public static final String HUD_FILE = SupernaturalBloodlines.MODID + "-hud-client.toml";

    public static final List<String> ALL = List.of(
            HUB_FILE + ":hub_theme",
            HUD_FILE + ":hud_enabled",
            HUD_FILE + ":hide_covered_keys",
            HUD_FILE + ":hide_tensura_skill_hud",
            HUD_FILE + ":blood_in_food_spot",
            HUD_FILE + ":mode",
            HUD_FILE + ":tracker_anchor");

    private PlayerSettings() {}
}
