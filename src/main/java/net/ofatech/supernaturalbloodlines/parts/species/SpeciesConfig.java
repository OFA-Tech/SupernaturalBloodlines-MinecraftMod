package net.ofatech.supernaturalbloodlines.parts.species;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** The part's own config file. Values are readable once the config has loaded. */
public final class SpeciesConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-species.toml";
    public static final ModConfigSpec SPEC;
    public static final int DEFAULT_MAX_ORIGINALS = 0;
    public static final int DEFAULT_TRANSITION_TICKS = 24000;

    public static final int DEFAULT_MAX_FIVE = 5;

    private static final ModConfigSpec.IntValue MAX_ORIGINALS;
    private static final ModConfigSpec.IntValue MAX_FIVE;
    public static final boolean DEFAULT_WITCH_BONUS = true;
    public static final double DEFAULT_SIPHONER_CHANCE = 0.25;

    private static final ModConfigSpec.IntValue TRANSITION_TICKS;
    private static final ModConfigSpec.BooleanValue WITCH_BONUS;
    private static final ModConfigSpec.DoubleValue SIPHONER_CHANCE;
    public static final boolean DEFAULT_SIRELINE_DEATH = false;
    private static final ModConfigSpec.BooleanValue SIRELINE_DEATH;
    public static final double DEFAULT_LATENT_GENE_CHANCE = 0.15;
    private static final ModConfigSpec.DoubleValue LATENT_GENE_CHANCE;
    public static final double DEFAULT_ALPHA_BITE_DEATH_CHANCE = 0.10;
    private static final ModConfigSpec.DoubleValue ALPHA_BITE_DEATH_CHANCE;
    public static final double DEFAULT_LUNAR_ECLIPSE_CHANCE = 0.03;
    private static final ModConfigSpec.DoubleValue LUNAR_ECLIPSE_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("species");
        MAX_ORIGINALS = b.comment("How many Originals (Original and Original hybrid) the world allows. 0 = unlimited.")
                .defineInRange("max_originals_per_world", DEFAULT_MAX_ORIGINALS, 0, 10_000);
        TRANSITION_TICKS = b.comment("Play time a werewolf has to drink doppelgänger blood after dying with Original's Blood, in ticks.")
                .defineInRange("transition_ticks", DEFAULT_TRANSITION_TICKS, 20, 24_000_000);
        MAX_FIVE = b.comment("How many Hunters of the Five the world allows. Lowering it later does not remove anyone: the Five can stay above this number until marks are lost, and no new Rite of the Five works until there are fewer than this.")
                .defineInRange("max_five", DEFAULT_MAX_FIVE, 1, 10_000);
        WITCH_BONUS = b.comment("Witches, heretics and tribrids get altar range x1.5 and rite power cost x0.75.")
                .define("witch_bonus_enabled", DEFAULT_WITCH_BONUS);
        SIPHONER_CHANCE = b.comment("Chance that the Rite of Ancestral Calling makes a siphoner instead of a witch.")
                .defineInRange("siphoner_chance", DEFAULT_SIPHONER_CHANCE, 0.0, 1.0);
        SIRELINE_DEATH = b.comment("When an Original is killed with white oak, everyone they sired, and everyone those sired, dies with them (offline players on their next login).")
                .define("sireline_death", DEFAULT_SIRELINE_DEATH);
        LATENT_GENE_CHANCE = b.comment("Chance that a player carries the hidden werewolf gene, rolled once at first login.")
                .defineInRange("latent_gene_chance", DEFAULT_LATENT_GENE_CHANCE, 0.0, 1.0);
        ALPHA_BITE_DEATH_CHANCE = b.comment("Chance that an Alpha's bite kills the human instead of turning them.")
                .defineInRange("alpha_bite_death_chance", DEFAULT_ALPHA_BITE_DEATH_CHANCE, 0.0, 1.0);
        LUNAR_ECLIPSE_CHANCE = b.comment("Chance that a full-moon night is a lunar eclipse: werewolves lose their form actions, rank perks and pack bonus until dawn.")
                .defineInRange("lunar_eclipse_chance", DEFAULT_LUNAR_ECLIPSE_CHANCE, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private SpeciesConfig() {}

    private static volatile Boolean witchBonusOverride;
    private static volatile Double siphonerChanceOverride;

    /** For tests: pins {@link #witchBonusEnabled()}; null clears it. */
    @VisibleForTesting
    public static void overrideWitchBonus(Boolean value) {
        witchBonusOverride = value;
    }

    /** For tests: pins {@link #siphonerChance()}; null clears it. */
    @VisibleForTesting
    public static void overrideSiphonerChance(Double value) {
        siphonerChanceOverride = value;
    }

    public static boolean witchBonusEnabled() {
        Boolean pinned = witchBonusOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return WITCH_BONUS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_WITCH_BONUS;
        }
    }

    public static double siphonerChance() {
        Double pinned = siphonerChanceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return SIPHONER_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_SIPHONER_CHANCE;
        }
    }

    private static volatile Double latentGeneChanceOverride;

    /** For tests: pins {@link #latentGeneChance()}; null clears it. */
    @VisibleForTesting
    public static void overrideLatentGeneChance(Double value) {
        latentGeneChanceOverride = value;
    }

    public static double latentGeneChance() {
        Double pinned = latentGeneChanceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return LATENT_GENE_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_LATENT_GENE_CHANCE;
        }
    }

    private static volatile Double alphaBiteDeathChanceOverride;

    /** For tests: pins {@link #alphaBiteDeathChance()}; null clears it. */
    @VisibleForTesting
    public static void overrideAlphaBiteDeathChance(Double value) {
        alphaBiteDeathChanceOverride = value;
    }

    public static double alphaBiteDeathChance() {
        Double pinned = alphaBiteDeathChanceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return ALPHA_BITE_DEATH_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_ALPHA_BITE_DEATH_CHANCE;
        }
    }
    public static double lunarEclipseChance() {
        try {
            return LUNAR_ECLIPSE_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_LUNAR_ECLIPSE_CHANCE;
        }
    }

    private static volatile Boolean sirelineDeathOverride;

    /** For tests: pins {@link #sirelineDeath()}; null clears it. */
    @VisibleForTesting
    public static void overrideSirelineDeath(Boolean value) {
        sirelineDeathOverride = value;
    }

    public static boolean sirelineDeath() {
        Boolean pinned = sirelineDeathOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return SIRELINE_DEATH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_SIRELINE_DEATH;
        }
    }

    private static volatile Integer maxOriginalsOverride;

    /** For tests: pins {@link #maxOriginals()} without touching the config file; null clears it. */
    @VisibleForTesting
    public static void overrideMaxOriginals(Integer value) {
        maxOriginalsOverride = value;
    }

    public static int maxOriginals() {
        Integer pinned = maxOriginalsOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return MAX_ORIGINALS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_MAX_ORIGINALS;
        }
    }

    private static volatile Integer maxFiveOverride;

    /** For tests: pins {@link #maxFive()} without touching the config file; null clears it. */
    @VisibleForTesting
    public static void overrideMaxFive(Integer value) {
        maxFiveOverride = value;
    }

    public static int maxFive() {
        Integer pinned = maxFiveOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return MAX_FIVE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_MAX_FIVE;
        }
    }

    public static long transitionTicks() {
        try {
            return TRANSITION_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_TRANSITION_TICKS;
        }
    }
}
