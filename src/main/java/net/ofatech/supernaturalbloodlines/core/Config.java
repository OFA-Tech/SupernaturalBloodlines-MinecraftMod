package net.ofatech.supernaturalbloodlines.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The COMMON config spec. Early (pre-config) reads of the part switches live in {@link EarlySwitches}. */
public final class Config {
    public static final ModConfigSpec COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Switch a part off to keep it disabled even when its mods are installed. Restart required.")
                .push("parts");
        for (Part p : Part.values()) {
            b.define(p.configKey(), true);
        }
        b.pop();
        COMMON = b.build();
    }

    private Config() {}
}
