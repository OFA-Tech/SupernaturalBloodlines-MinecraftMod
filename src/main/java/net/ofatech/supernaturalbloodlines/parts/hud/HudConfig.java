package net.ofatech.supernaturalbloodlines.parts.hud;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's common config. The three mixin switches are reserved: each guards a mixin that is only added when its task shows no event or API works. */
public final class HudConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-hud.toml";
    public static final ModConfigSpec SPEC;
    public static final int DEFAULT_RATE_LIMIT = 10;
    public static final double DEFAULT_BITE_REACH = 4.0;

    private static final ModConfigSpec.IntValue RATE_LIMIT_PER_SECOND;
    private static final ModConfigSpec.DoubleValue BITE_REACH;
    public static final ModConfigSpec.BooleanValue WHEEL_ENABLED;
    private static final ModConfigSpec.IntValue WHEEL_CATALOGUE_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue WHEEL_UNSLOTTED_TENSURA;
    public static final ModConfigSpec.BooleanValue NEXT_PRESET_ACTIVE;
    public static final ModConfigSpec.BooleanValue TENSURA_FEED_FILLS_BLOOD;
    public static final ModConfigSpec.BooleanValue HIDE_IN_CONTROLS_MIXIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RATE_LIMIT_PER_SECOND = b.comment("Most HUD payloads (bites, action triggers, loadout changes) one player may send per second; the rest are dropped.")
                .defineInRange("rate_limit_per_second", DEFAULT_RATE_LIMIT, 1, 100);
        BITE_REACH = b.comment("How far (blocks) a Bite action may reach its target, 1 to 16. Vampirism's own drinking is also limited to the player's interaction range (3), so a higher value only helps the other bites.")
                .defineInRange("bite_reach", DEFAULT_BITE_REACH, 1.0, 16.0);
        WHEEL_ENABLED = b.comment("The skill wheel's server side: with it off the wheel is sent an empty list and every use from it is refused (the six slots are unaffected).")
                .define("wheel_enabled", true);
        WHEEL_CATALOGUE_INTERVAL_TICKS = b.comment("How often (ticks) the server checks whether the list of an open wheel changed, 1 to 40. Only a player with the wheel open is checked.")
                .defineInRange("wheel_catalogue_interval_ticks", 5, 1, 40);
        WHEEL_UNSLOTTED_TENSURA = b.comment("Let the wheel use a Tensura skill, spell or art that is in no preset slot (through the next-preset hook, only while it runs). Off: such powers are listed locked, put it in a slot first.")
                .define("wheel_unslotted_tensura", true);
        b.comment("Switches of the hud part's mixins (each is applied only with the mods it targets). next_preset_active and tensura_feed_fills_blood guard a mixin each; hide_in_controls_mixin is reserved and does nothing.").push("mixins");
        NEXT_PRESET_ACTIVE = b.define("next_preset_active", true);
        TENSURA_FEED_FILLS_BLOOD = b.define("tensura_feed_fills_blood", true);
        HIDE_IN_CONTROLS_MIXIN = b.define("hide_in_controls_mixin", true);
        b.pop();
        SPEC = b.build();
    }

    private HudConfig() {}

    public static int rateLimitPerSecond() {
        try {
            return RATE_LIMIT_PER_SECOND.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_RATE_LIMIT;
        }
    }

    /** The switch of reserved mixin (1); read when a check runs, since the mixin is applied before the config loads. */
    public static boolean nextPresetActive() {
        try {
            return NEXT_PRESET_ACTIVE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static boolean tensuraFeedFillsBlood() {
        try {
            return TENSURA_FEED_FILLS_BLOOD.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static boolean wheelEnabled() {
        try {
            return WHEEL_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static int wheelCatalogueIntervalTicks() {
        try {
            return WHEEL_CATALOGUE_INTERVAL_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return 5;
        }
    }

    public static boolean wheelUnslottedTensura() {
        try {
            return WHEEL_UNSLOTTED_TENSURA.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static double biteReach() {
        try {
            return BITE_REACH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BITE_REACH;
        }
    }
}
