package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. The ladders and skills are datapack JSON, not config. */
public final class SupernaturalConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-supernatural.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue RITE_COMPLETE_SIGNAL;
    private static final ModConfigSpec.IntValue VAMPIRE_FEED_BLOOD;
    public static final int DEFAULT_VAMPIRE_FEED_BLOOD = 3;
    private static volatile Boolean riteCompleteOverride;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RITE_COMPLETE_SIGNAL = b.comment("A rite that worked counts for ladder tasks (the RiteCompleteMixin hook on Enchanted's Rite.start). Off: no rite counts for any task.")
                .define("rite_complete_signal", true);
        VAMPIRE_FEED_BLOOD = b.comment("Predator's Thirst (S-VAM-6): the Vampirism blood a bite on a vampire gives when Vampirism gives none for that victim (it is also what a vampire player loses).")
                .defineInRange("vampire_feed_blood", DEFAULT_VAMPIRE_FEED_BLOOD, 1, 20);
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

    public static int vampireFeedBlood() {
        try {
            return VAMPIRE_FEED_BLOOD.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VAMPIRE_FEED_BLOOD;
        }
    }

    /** For tests: pins {@link #riteCompleteSignal()}; null clears it. */
    public static void overrideRiteCompleteSignal(Boolean value) {
        riteCompleteOverride = value;
    }
}
