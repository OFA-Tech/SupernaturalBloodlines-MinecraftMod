package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The HUD's client settings. Each player picks their own. */
public final class HudClientConfig {
    public enum Mode { AUTO, TENSURA, SUPERNATURAL }

    public static final String FILE = SupernaturalBloodlines.MODID + "-hud-client.toml";
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue HUD_ENABLED;
    private static final ModConfigSpec.BooleanValue HIDE_COVERED_KEYS;
    private static final ModConfigSpec.BooleanValue HIDE_TENSURA_SKILL_HUD;
    private static final ModConfigSpec.BooleanValue BLOOD_IN_FOOD_SPOT;
    private static final ModConfigSpec.EnumValue<Mode> MODE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HUD_ENABLED = b.comment("Draw the ability HUD.").define("hud_enabled", true);
        HIDE_COVERED_KEYS = b.comment("Unbind and hide the keys of other mods that the HUD covers. Off restores the binds recorded before.")
                .define("hide_covered_keys", true);
        HIDE_TENSURA_SKILL_HUD = b.comment("Hide Tensura's own skill HUD while ours is on.").define("hide_tensura_skill_hud", true);
        BLOOD_IN_FOOD_SPOT = b.comment("Draw a Vampirism vampire's blood in Tensura's food spot (only with Tensura and Vampirism).")
                .define("blood_in_food_spot", true);
        MODE = b.comment("Which source the six slots show. AUTO: Tensura when it is installed, else Supernatural.")
                .defineEnum("mode", Mode.AUTO);
        SPEC = b.build();
    }

    private HudClientConfig() {}

    public static boolean hudEnabled() {
        return read(HUD_ENABLED, true);
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

    public static Mode mode() {
        try {
            return MODE.get();
        } catch (IllegalStateException notLoaded) {
            return Mode.AUTO;
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
