package net.ofatech.supernaturalbloodlines.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The COMMON config spec. Early (pre-config) reads of the part switches live in {@link EarlySwitches}. */
public final class Config {
    public static final ModConfigSpec COMMON;
    public static final int DEFAULT_FULL_FORM_LEVEL = 8;
    public static final ModConfigSpec.IntValue FULL_FORM_LEVEL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Switch a part off to keep it disabled even when its mods are installed. Restart required.")
                .push("parts");
        for (Part p : Part.values()) {
            b.define(p.configKey(), true);
        }
        b.pop();
        b.push("werewolves");
        FULL_FORM_LEVEL = b.comment("Werewolves level from which moon control (Moon Ring, hybrids) also allows the full forms (beast, four-legged beast). Below it only the half forms.")
                .defineInRange("full_form_level", DEFAULT_FULL_FORM_LEVEL, 1, 100);
        b.pop();
        COMMON = b.build();
    }

    private Config() {}
}
