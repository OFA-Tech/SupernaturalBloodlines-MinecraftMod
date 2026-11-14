package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. Empty for now: the ladders and skills are datapack JSON, not config. */
public final class SupernaturalConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-supernatural.toml";
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder().build();

    private SupernaturalConfig() {}
}
