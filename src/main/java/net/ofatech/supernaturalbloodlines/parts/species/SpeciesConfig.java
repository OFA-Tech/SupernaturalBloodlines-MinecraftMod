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
    public static final int DEFAULT_VAMPIRE_BLOOD_TICKS = 24000;
    public static final int DEFAULT_BITE_VAMPIRE_COST = 1;

    public static final int DEFAULT_MAX_FIVE = 5;

    private static final ModConfigSpec.IntValue MAX_ORIGINALS;
    private static final ModConfigSpec.IntValue MAX_FIVE;
    public static final boolean DEFAULT_WITCH_BONUS = true;
    public static final double DEFAULT_SIPHONER_CHANCE = 0.25;

    private static final ModConfigSpec.IntValue TRANSITION_TICKS;
    private static final ModConfigSpec.IntValue VAMPIRE_BLOOD_TICKS;
    private static final ModConfigSpec.IntValue BITE_VAMPIRE_COST;
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
    public static final double DEFAULT_DOPPELGANGER_GENE_CHANCE = 0.05;
    private static final ModConfigSpec.DoubleValue DOPPELGANGER_GENE_CHANCE;
    public static final double DEFAULT_DOPPELGANGER_VILLAGER_CHANCE = 0.005;
    private static final ModConfigSpec.DoubleValue DOPPELGANGER_VILLAGER_CHANCE;
    public static final String GENES_DISCOVERED = "discovered";
    public static final String GENES_ALWAYS = "always";
    private static final ModConfigSpec.ConfigValue<String> GENE_VISIBILITY;
    public static final String TAGLOCK_DOPPELGANGER = "doppelganger";
    public static final String TAGLOCK_HUMAN = "human";
    private static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_RITE_TAGLOCK;
    public static final String TRANSITION_BLOOD_ORIGINAL_HYBRID = "original_hybrid";
    public static final String TRANSITION_BLOOD_ORIGINAL = "original";
    private static final ModConfigSpec.ConfigValue<String> HYBRID_TRANSITION_BLOOD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("species");
        MAX_ORIGINALS = b.comment("How many Originals (Original and Original hybrid) the world allows. 0 = unlimited.")
                .defineInRange("max_originals_per_world", DEFAULT_MAX_ORIGINALS, 0, 10_000);
        TRANSITION_TICKS = b.comment("Play time a werewolf has to drink doppelgänger blood after dying with Original's Blood, in ticks.")
                .defineInRange("transition_ticks", DEFAULT_TRANSITION_TICKS, 20, 24_000_000);
        VAMPIRE_BLOOD_TICKS = b.comment("How long vampire blood stays in a player's system, in game ticks. Dying while it is there starts the turning (the same transition_ticks to drink human blood).")
                .defineInRange("vampire_blood_ticks", DEFAULT_VAMPIRE_BLOOD_TICKS, 20, 24_000_000);
        BITE_VAMPIRE_COST = b.comment("Biting a vampire never feeds the biter and costs them this much of their own blood (0 = nothing).")
                .defineInRange("bite_vampire_cost", DEFAULT_BITE_VAMPIRE_COST, 0, 20);
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
        DOPPELGANGER_GENE_CHANCE = b.comment("Chance that a player carries the hidden doppelgänger gene, rolled once at their first login after it existed. It can sit next to the werewolf gene.")
                .defineInRange("doppelganger_gene_chance", DEFAULT_DOPPELGANGER_GENE_CHANCE, 0.0, 1.0);
        DOPPELGANGER_VILLAGER_CHANCE = b.comment("Chance that a villager is a doppelgänger, rolled once the first time the villager is loaded (a new spawn, or an old villager after the update). Unlimited per world.")
                .defineInRange("doppelganger_villager_chance", DEFAULT_DOPPELGANGER_VILLAGER_CHANCE, 0.0, 1.0);
        GENE_VISIBILITY = b.comment("discovered: a gene shows as Unknown to its player until the Rite of Revelation, a Doppelgänger Blood rite with their taglock, or a werewolf awakening finds it. always: a player always sees their own genes.")
                .defineInList("gene_visibility", GENES_DISCOVERED, new java.util.ArrayList<>(java.util.List.of(GENES_DISCOVERED, GENES_ALWAYS)));
        DOPPELGANGER_RITE_TAGLOCK = b.comment("Whose taglock the Rite of Doppelgänger Blood accepts. doppelganger: a doppelgänger villager or a player with the gene. human: any human-like villager or player (the old behaviour).")
                .defineInList("doppelganger_rite_taglock", TAGLOCK_DOPPELGANGER, new java.util.ArrayList<>(java.util.List.of(TAGLOCK_DOPPELGANGER, TAGLOCK_HUMAN)));
        HYBRID_TRANSITION_BLOOD = b.comment("Whose Original's Blood starts the hybrid transition (R40). original_hybrid: only blood taken from an Original hybrid. original: blood from any Original or Original hybrid. A bottle made before the bottles recorded their source counts under either setting.")
                .defineInList("hybrid_transition_blood", TRANSITION_BLOOD_ORIGINAL_HYBRID, new java.util.ArrayList<>(java.util.List.of(TRANSITION_BLOOD_ORIGINAL_HYBRID, TRANSITION_BLOOD_ORIGINAL)));
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

    private static volatile Double doppelgangerGeneChanceOverride;

    /** For tests: pins {@link #doppelgangerGeneChance()}; null clears it. */
    @VisibleForTesting
    public static void overrideDoppelgangerGeneChance(Double value) {
        doppelgangerGeneChanceOverride = value;
    }

    public static double doppelgangerGeneChance() {
        Double pinned = doppelgangerGeneChanceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return DOPPELGANGER_GENE_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_DOPPELGANGER_GENE_CHANCE;
        }
    }

    private static volatile Double doppelgangerVillagerChanceOverride;

    /** For tests: pins {@link #doppelgangerVillagerChance()}; null clears it. */
    @VisibleForTesting
    public static void overrideDoppelgangerVillagerChance(Double value) {
        doppelgangerVillagerChanceOverride = value;
    }

    public static double doppelgangerVillagerChance() {
        Double pinned = doppelgangerVillagerChanceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return DOPPELGANGER_VILLAGER_CHANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_DOPPELGANGER_VILLAGER_CHANCE;
        }
    }

    private static volatile Boolean genesAlwaysVisibleOverride;

    /** For tests: pins {@link #genesAlwaysVisible()}; null clears it. */
    @VisibleForTesting
    public static void overrideGenesAlwaysVisible(Boolean value) {
        genesAlwaysVisibleOverride = value;
    }

    /** {@code gene_visibility = always}. */
    public static boolean genesAlwaysVisible() {
        Boolean pinned = genesAlwaysVisibleOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return GENES_ALWAYS.equals(GENE_VISIBILITY.get());
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    private static volatile Boolean humanTaglockOverride;

    /** For tests: pins {@link #doppelgangerRiteAcceptsHumans()}; null clears it. */
    @VisibleForTesting
    public static void overrideDoppelgangerRiteHuman(Boolean value) {
        humanTaglockOverride = value;
    }

    /** {@code doppelganger_rite_taglock = human}; false is the default {@code doppelganger}. */
    public static boolean doppelgangerRiteAcceptsHumans() {
        Boolean pinned = humanTaglockOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return TAGLOCK_HUMAN.equals(DOPPELGANGER_RITE_TAGLOCK.get());
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    private static volatile Boolean transitionBloodAnyOriginalOverride;

    /** For tests: pins {@link #transitionBloodAnyOriginal()}; null clears it. */
    @VisibleForTesting
    public static void overrideTransitionBloodAnyOriginal(Boolean value) {
        transitionBloodAnyOriginalOverride = value;
    }

    /** {@code hybrid_transition_blood = original}; false is the default {@code original_hybrid}. */
    public static boolean transitionBloodAnyOriginal() {
        Boolean pinned = transitionBloodAnyOriginalOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return TRANSITION_BLOOD_ORIGINAL.equals(HYBRID_TRANSITION_BLOOD.get());
        } catch (IllegalStateException notLoaded) {
            return false;
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

    public static long vampireBloodTicks() {
        try {
            return VAMPIRE_BLOOD_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VAMPIRE_BLOOD_TICKS;
        }
    }

    private static volatile Integer biteVampireCostOverride;

    /** For tests: pins {@link #biteVampireCost()}; null clears it. */
    @VisibleForTesting
    public static void overrideBiteVampireCost(Integer value) {
        biteVampireCostOverride = value;
    }

    public static int biteVampireCost() {
        Integer pinned = biteVampireCostOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return BITE_VAMPIRE_COST.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BITE_VAMPIRE_COST;
        }
    }
}
