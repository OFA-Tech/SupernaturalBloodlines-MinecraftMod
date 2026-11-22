package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.HudVisibility;

/** The HUD's client settings. Each player picks their own. */
public final class HudClientConfig {
    public enum Mode { AUTO, TENSURA, SUPERNATURAL }

    public static final String FILE = SupernaturalBloodlines.MODID + "-hud-client.toml";
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue HUD_ENABLED;
    private static final ModConfigSpec.EnumValue<HudVisibility> VISIBILITY;
    private static final ModConfigSpec.BooleanValue SPRINT_LEAP;
    private static final ModConfigSpec.BooleanValue HIDE_COVERED_KEYS;
    private static final ModConfigSpec.BooleanValue HIDE_TENSURA_SKILL_HUD;
    private static final ModConfigSpec.BooleanValue BLOOD_IN_FOOD_SPOT;
    private static final ModConfigSpec.EnumValue<Mode> MODE;
    private static final ModConfigSpec.EnumValue<net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor> TRACKER_ANCHOR;
    private static final ModConfigSpec.EnumValue<net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor> TOAST_ANCHOR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HUD_ENABLED = b.comment("Draw the ability HUD.").define("hud_enabled", true);
        VISIBILITY = b.comment("When the ability HUD shows. AUTO: while you use it, and it fades out after ten idle seconds (no slot key, no cooldown running, no active toggle, no mode switch, Alt not held).",
                "ALWAYS: never fades. HIDDEN: never drawn.").defineEnum("hud_visibility", HudVisibility.AUTO);
        SPRINT_LEAP = b.comment("A werewolf in a transformed form leaps when jumping while sprinting (Werewolves' own leap, with its cooldown and checks). Its Left Control key is covered by the HUD.")
                .define("sprint_leap", true);
        HIDE_COVERED_KEYS = b.comment("Unbind and hide the keys of other mods that the HUD covers. Off restores the binds recorded before.")
                .define("hide_covered_keys", true);
        HIDE_TENSURA_SKILL_HUD = b.comment("Hide Tensura's own skill HUD while ours is on.").define("hide_tensura_skill_hud", true);
        BLOOD_IN_FOOD_SPOT = b.comment("Draw a Vampirism vampire's blood in Tensura's food spot (only with Tensura and Vampirism).")
                .define("blood_in_food_spot", true);
        MODE = b.comment("Which source the six slots show. AUTO: Tensura when it is installed, else Supernatural.")
                .defineEnum("mode", Mode.AUTO);
        TRACKER_ANCHOR = b.comment("Where the tracked task shows: TOP_CENTER (under the boss bars), TOP_RIGHT (under the full moon warning), TOP_LEFT (under the corner",
                "where Tensura and Vampirism draw your status) or ABOVE_MODE_PILL.").defineEnum("tracker_anchor", net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_CENTER);
        TOAST_ANCHOR = b.comment("Where the request toast shows: TOP_LEFT (the default: the top left, under the status Tensura and Vampirism draw there), TOP_CENTER, TOP_RIGHT or ABOVE_MODE_PILL.",
                "It moves under the tracked task's pill when the two would overlap.").defineEnum("toast_anchor", net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_LEFT);
        SPEC = b.build();
    }

    private HudClientConfig() {}

    public static boolean hudEnabled() {
        return read(HUD_ENABLED, true);
    }

    public static HudVisibility visibility() {
        try {
            return VISIBILITY.get();
        } catch (IllegalStateException notLoaded) {
            return HudVisibility.AUTO;
        }
    }

    public static boolean sprintLeap() {
        return read(SPRINT_LEAP, true);
    }

    public static boolean hideCoveredKeys() {
        return read(HIDE_COVERED_KEYS, true);
    }

    public static boolean hideTensuraSkillHud() {
        return read(HIDE_TENSURA_SKILL_HUD, true);
    }

    public static boolean bloodInFoodSpot() {
        return read(BLOOD_IN_FOOD_SPOT, true);
    }

    public static net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor trackerAnchor() {
        try {
            return TRACKER_ANCHOR.get();
        } catch (IllegalStateException notLoaded) {
            return net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_CENTER;
        }
    }

    public static net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor toastAnchor() {
        try {
            return TOAST_ANCHOR.get();
        } catch (IllegalStateException notLoaded) {
            return net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_LEFT;
        }
    }

    public static Mode mode() {
        try {
            return MODE.get();
        } catch (IllegalStateException notLoaded) {
            return Mode.AUTO;
        }
    }

    /** Remembers the mode the player chose with the mode key, for the next session. */
    public static void rememberMode(Mode mode) {
        try {
            MODE.set(mode);
            MODE.save();
        } catch (RuntimeException notLoaded) {
            // the config is not loaded yet (a test run): the choice only lasts this session
        }
    }

    private static boolean read(ModConfigSpec.BooleanValue v, boolean fallback) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }
}
