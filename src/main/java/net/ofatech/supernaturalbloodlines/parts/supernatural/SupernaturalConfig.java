package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. The ladders and skills are datapack JSON, not config. */
public final class SupernaturalConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-supernatural.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue RITE_COMPLETE_SIGNAL;
    private static volatile Boolean riteCompleteOverride;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RITE_COMPLETE_SIGNAL = b.comment("A rite that worked counts for ladder tasks (the RiteCompleteMixin hook on Enchanted's Rite.start). Off: no rite counts for any task.")
                .define("rite_complete_signal", true);
        SPEC = b.build();
    }

    private SupernaturalConfig() {}

    public static boolean riteCompleteSignal() {
        Boolean pinned = riteCompleteOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return RITE_COMPLETE_SIGNAL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** For tests: pins {@link #riteCompleteSignal()}; null clears it. */
    public static void overrideRiteCompleteSignal(Boolean value) {
        riteCompleteOverride = value;
    }
}
