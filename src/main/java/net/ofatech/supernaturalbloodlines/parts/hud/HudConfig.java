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
    public static final ModConfigSpec.BooleanValue NEXT_PRESET_ACTIVE;
    public static final ModConfigSpec.BooleanValue TENSURA_FEED_FILLS_BLOOD;
    public static final ModConfigSpec.BooleanValue HIDE_IN_CONTROLS_MIXIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RATE_LIMIT_PER_SECOND = b.comment("Most HUD payloads (bites, action triggers, loadout changes) one player may send per second; the rest are dropped.")
                .defineInRange("rate_limit_per_second", DEFAULT_RATE_LIMIT, 1, 100);
        BITE_REACH = b.comment("How far (blocks) a Bite action may reach its target, 1 to 16. Vampirism's own drinking is also limited to the player's interaction range (3), so a higher value only helps the other bites.")
                .defineInRange("bite_reach", DEFAULT_BITE_REACH, 1.0, 16.0);
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

    public static double biteReach() {
        try {
            return BITE_REACH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BITE_REACH;
        }
    }
}
