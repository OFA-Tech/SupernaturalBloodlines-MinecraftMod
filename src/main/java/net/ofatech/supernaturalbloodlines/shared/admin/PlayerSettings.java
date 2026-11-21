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

    /** The CLIENT keys the Settings page edits, in page order; the player's own, never sent to the server. */
    public static final List<SettingEntry> ENTRIES = List.of(
            SettingEntry.of(HUB_FILE, "look", "hub_theme", SettingKind.CHOICE),
            SettingEntry.of(HUB_FILE, "look", "open_on", SettingKind.CHOICE),
            SettingEntry.of(HUD_FILE, "hud", "hud_enabled", SettingKind.BOOL),
            SettingEntry.of(HUD_FILE, "hud", "hud_visibility", SettingKind.CHOICE),
            SettingEntry.of(HUD_FILE, "hud", "sprint_leap", SettingKind.BOOL),
            SettingEntry.of(HUD_FILE, "hud", "hide_covered_keys", SettingKind.BOOL),
            SettingEntry.of(HUD_FILE, "hud", "hide_tensura_skill_hud", SettingKind.BOOL),
            SettingEntry.of(HUD_FILE, "hud", "blood_in_food_spot", SettingKind.BOOL),
            SettingEntry.of(HUD_FILE, "hud", "mode", SettingKind.CHOICE),
            SettingEntry.of(HUD_FILE, "hud", "tracker_anchor", SettingKind.CHOICE));

    public static final List<String> ALL = ENTRIES.stream().map(SettingEntry::id).toList();

    private PlayerSettings() {}
}
