package net.ofatech.supernaturalbloodlines.parts.celestials;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;

/** The part's common config: how much an Enhanced Celestials event changes. A factor of 1 turns that effect off. */
public final class CelestialsConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-celestials.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue BLOOD_MOON_PACK_CHANCE_FACTOR;
    private static final ModConfigSpec.DoubleValue BLOOD_MOON_BITE_BONUS;
    private static final ModConfigSpec.DoubleValue BLUE_MOON_RITE_COST;
    private static final ModConfigSpec.DoubleValue SUPER_MOON_RITE_COST;
    private static final ModConfigSpec.DoubleValue SUPER_MOON_BOSS_WEIGHT;
    public static final ModConfigSpec.BooleanValue SUPER_BLUE_SKIPS_FORCED_SHIFT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        BLOOD_MOON_PACK_CHANCE_FACTOR = b.comment("Reserved for NPC werewolf packs (not built yet): their chance on a Blood Moon, as a factor of the full-moon chance. Nothing reads it today.")
                .defineInRange("blood_moon_pack_chance_factor", 0.5, 0.0, 1.0);
        BLOOD_MOON_BITE_BONUS = b.comment("A vampire's bite gives this many times the blood on a Blood Moon. 1 = no bonus.")
                .defineInRange("blood_moon_bite_bonus", 1.25, 1.0, 5.0);
        BLUE_MOON_RITE_COST = b.comment("Altar power cost of every rite on a Blue Moon, as a factor. 1 = no discount. A Blue Moon never allows a full-moon rite on a night that is not a full moon.")
                .defineInRange("blue_moon_rite_cost", 0.8, 0.1, 1.0);
        SUPER_MOON_RITE_COST = b.comment("Altar power cost of the Rite of the Moon, the Silver Moon and Immortality on a super moon, as a factor (on top of the Blue Moon one). 1 = no discount.")
                .defineInRange("super_moon_rite_cost", 0.75, 0.1, 1.0);
        SUPER_MOON_BOSS_WEIGHT = b.comment("Reserved for the full-moon bosses (not built yet): their spawn weight on a super moon. Nothing reads it today.")
                .defineInRange("super_moon_boss_weight", 2.0, 1.0, 10.0);
        SUPER_BLUE_SKIPS_FORCED_SHIFT = b.comment("A Super Blue Moon (a full moon that is also calm) does not force the Werewolves full-moon shift on anybody. The shift at will still follows the moon-bound rule.")
                .define("super_blue_skips_forced_shift", true);
        SPEC = b.build();
    }

    private CelestialsConfig() {
    }

    /** The numbers as the lunar mood reads them. Before the config loads the defaults are used. */
    public static LunarMoods.Tuning tuning() {
        try {
            return new LunarMoods.Tuning(BLUE_MOON_RITE_COST.get(), SUPER_MOON_RITE_COST.get(), BLOOD_MOON_BITE_BONUS.get(),
                    BLOOD_MOON_PACK_CHANCE_FACTOR.get(), SUPER_MOON_BOSS_WEIGHT.get(), SUPER_BLUE_SKIPS_FORCED_SHIFT.get());
        } catch (IllegalStateException notLoaded) {
            return new LunarMoods.Tuning(BLUE_MOON_RITE_COST.getDefault(), SUPER_MOON_RITE_COST.getDefault(), BLOOD_MOON_BITE_BONUS.getDefault(),
                    BLOOD_MOON_PACK_CHANCE_FACTOR.getDefault(), SUPER_MOON_BOSS_WEIGHT.getDefault(), SUPER_BLUE_SKIPS_FORCED_SHIFT.getDefault());
        }
    }
}
