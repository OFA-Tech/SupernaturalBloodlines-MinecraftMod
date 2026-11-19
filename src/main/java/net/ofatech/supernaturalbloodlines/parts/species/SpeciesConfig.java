package net.ofatech.supernaturalbloodlines.parts.species;

import java.util.List;
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
    private static final ModConfigSpec.BooleanValue ONLY_ALPHAS_TURN;
    private static volatile Boolean onlyAlphasTurnOverride;
    public static final int DEFAULT_MAX_STACKED_SPARKS = 5;
    private static final ModConfigSpec.IntValue MAX_STACKED_SPARKS;
    private static volatile Integer maxStackedSparksOverride;
    public static final int DEFAULT_DUEL_RECONNECT_SECONDS = 60;
    private static final ModConfigSpec.IntValue DUEL_RECONNECT_SECONDS;
    private static volatile Integer duelReconnectOverride;
    public static final int DEFAULT_LEFT_PACK_GRACE_MINUTES = 10;
    private static final ModConfigSpec.IntValue LEFT_PACK_GRACE_MINUTES;
    private static volatile Integer leftPackGraceOverride;
    public static final int DEFAULT_ABSENT_LEADER_DAYS = 7;
    private static final ModConfigSpec.IntValue ABSENT_LEADER_DAYS;
    private static volatile Integer absentLeaderDaysOverride;
    public static final boolean DEFAULT_OPAC_LINK = true;
    private static final ModConfigSpec.BooleanValue OPAC_LINK_ENABLED;
    private static volatile Boolean opacLinkOverride;
    public static final boolean DEFAULT_WITCH_BONUS = true;
    public static final boolean DEFAULT_ONLY_ALPHAS_TURN = true;
    public static final double DEFAULT_SIPHONER_CHANCE = 0.25;

    private static final ModConfigSpec.IntValue TRANSITION_TICKS;
    private static final ModConfigSpec.IntValue VAMPIRE_BLOOD_TICKS;
    private static final ModConfigSpec.IntValue BITE_VAMPIRE_COST;
    private static final ModConfigSpec.BooleanValue WITCH_BONUS;
    private static final ModConfigSpec.DoubleValue SIPHONER_CHANCE;
    public static final boolean DEFAULT_ORIGINAL_LINE_DEATH = true;
    private static final ModConfigSpec.BooleanValue ORIGINAL_LINE_DEATH;
    public static final String LINE_DISCOVERED = "discovered";
    public static final String LINE_ALWAYS = "always";
    private static final ModConfigSpec.ConfigValue<String> ORIGINAL_VISIBILITY;
    public static final boolean DEFAULT_LINE_RITES = true;
    private static final ModConfigSpec.BooleanValue LINE_RITES_ENABLED;
    public static final List<String> DEFAULT_NPC_LINE_WEIGHTS = List.of("mikael=1", "finn=1", "elijah=1", "kol=1", "rebekah=1", "klaus=1");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NPC_LINE_WEIGHTS;
    public static final int DEFAULT_RING_RADIUS = 8;
    private static final ModConfigSpec.IntValue RING_MAX_RADIUS;
    public static final int DEFAULT_IMPRISONMENT_SECONDS = 30;
    private static final ModConfigSpec.IntValue IMPRISONMENT_SECONDS;
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
    public static final String SLEEP_OFF = "off";
    public static final String SLEEP_WEREWOLVES = "werewolves";
    public static final String SLEEP_EVERYONE = "everyone";
    private static final ModConfigSpec.ConfigValue<String> FULL_MOON_SLEEP;
    public static final int DEFAULT_DELAYED_AWAKENING_NIGHTS = 1;
    private static final ModConfigSpec.IntValue DELAYED_AWAKENING_NIGHTS;

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
        ONLY_ALPHAS_TURN = b.comment("R92: only an Alpha's bite turns a human (the Werewolves effect lupus_sanguinem is refused from a werewolf player below Alpha). Off: Werewolves' own rule, any werewolf's bite can infect (it then records no pack or sire).")
                .define("only_alphas_turn", DEFAULT_ONLY_ALPHAS_TURN);
        MAX_STACKED_SPARKS = b.comment("Most Alpha sparks one werewolf can hold at once (the Deucalion factor): an Alpha who kills an Alpha from outside their pack takes the victim's spark on top of their own. Each spark beyond the first adds 10% to the Alpha rank's attack and health bonuses and one tier to the Alpha Roar's Strength. 1 turns stacking off.")
                .defineInRange("max_stacked_sparks", DEFAULT_MAX_STACKED_SPARKS, 1, 20);
        DUEL_RECONNECT_SECONDS = b.comment("A duelist who loses their connection during a leadership duel has this many seconds to come back: the duel waits for them (its 2 minute clock stops) and the other one is told. After that they forfeit it, as if they had logged out. 0 makes a disconnect an instant forfeit. A server that is stopping settles nothing.")
                .defineInRange("duel_reconnect_seconds", DEFAULT_DUEL_RECONNECT_SECONDS, 0, 600);
        LEFT_PACK_GRACE_MINUTES = b.comment("Someone who left a pack by choice (with /pack leave, or by leaving its party) in the last this many minutes still counts as a member of it for what a kill does (no spark steal, no blue eyes, no gene awakening, no Hunter mark), so leaving just before a kill gains nothing, and joining another pack does not free it either. Someone who was kicked, and the members of a pack that dissolved, get no grace: they are outsiders at once. Kept in memory only: a restart forgets it. 0 turns it off.")
                .defineInRange("left_pack_grace_minutes", DEFAULT_LEFT_PACK_GRACE_MINUTES, 0, 1440);
        ABSENT_LEADER_DAYS = b.comment("A pack leader who has been offline this many real days loses the lead to the strongest second Alpha who is here (online, or seen within the same days); they keep their spark and stay a second Alpha. The time the server was off does not count, and a restart does not erase an absence. 0 turns it off.")
                .defineInRange("absent_leader_days", DEFAULT_ABSENT_LEADER_DAYS, 0, 3650);
        OPAC_LINK_ENABLED = b.comment("With Open Parties and Claims (or Team Claims) installed, a pack gets a party of its own: the leader owns it, everyone else is a member, and leaving the party leaves the pack. Only a party the pack made itself is ever touched, never a party anyone made by hand. Off: packs and parties are separate.")
                .define("opac_link_enabled", DEFAULT_OPAC_LINK);
        WITCH_BONUS = b.comment("Witches, heretics and tribrids get altar range x1.5 and rite power cost x0.75.")
                .define("witch_bonus_enabled", DEFAULT_WITCH_BONUS);
        SIPHONER_CHANCE = b.comment("Chance that the Rite of Ancestral Calling makes a siphoner instead of a witch.")
                .defineInRange("siphoner_chance", DEFAULT_SIPHONER_CHANCE, 0.0, 1.0);
        ORIGINAL_LINE_DEATH = b.comment("When an Original is killed with white oak, everyone in their line dies with them (vampires, hybrids and creatures; offline players on their next login). Someone freed from the line by a rite is spared.")
                .define("original_line_death", DEFAULT_ORIGINAL_LINE_DEATH);
        ORIGINAL_VISIBILITY = b.comment("discovered: a player sees the Original of their line as \"?\" until the Rite of Revelation finds it. always: a player always sees the Original of their line.")
                .defineInList("original_visibility", LINE_DISCOVERED, new java.util.ArrayList<>(List.of(LINE_DISCOVERED, LINE_ALWAYS)));
        LINE_RITES_ENABLED = b.comment("Switches the Rite of the Broken Line and the Rite of the Imprisoned Original on or off.")
                .define("line_rites_enabled", DEFAULT_LINE_RITES);
        NPC_LINE_WEIGHTS = b.comment("How likely each Original is to be the root of a new NPC vampire's line: name=weight. A bad entry is skipped; a weight below 0 counts as 0.")
                .defineList("npc_line_weights", DEFAULT_NPC_LINE_WEIGHTS, () -> "klaus=1", o -> o instanceof String);
        RING_MAX_RADIUS = b.comment("The mountain ash circle of the Rite of the Imprisoned Original must close within this many blocks of the rite's centre.")
                .defineInRange("imprisonment_ring_max_radius", DEFAULT_RING_RADIUS, 2, 16);
        IMPRISONMENT_SECONDS = b.comment("How long the Original must stay trapped in the circle for the Rite of the Imprisoned Original to work, in seconds.")
                .defineInRange("imprisonment_rite_seconds", DEFAULT_IMPRISONMENT_SECONDS, 5, 300);
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
        FULL_MOON_SLEEP = b.comment("R141: who cannot use a bed in the Overworld on a full-moon night, from dusk (tick 12000) to the morning. off: nobody. werewolves: players in the werewolf faction, in human form too, so a werewolf cannot sleep through the full moon before the forced shift. everyone: all players.")
                .defineInList("full_moon_sleep", SLEEP_OFF, new java.util.ArrayList<>(java.util.List.of(SLEEP_OFF, SLEEP_WEREWOLVES, SLEEP_EVERYONE)));
        DELAYED_AWAKENING_NIGHTS = b.comment("R142: a human who carries the hidden werewolf gene and kills a villager or a player awakens at the next dusk, or this many dusks later, not at once. 0 awakens at once, as before. The Rite of Lineage and a wild bite are not delayed.")
                .defineInRange("delayed_awakening_nights", DEFAULT_DELAYED_AWAKENING_NIGHTS, 0, 3);
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

    /** For tests: pins {@link #onlyAlphasTurn()}; null clears it. */
    public static void overrideOnlyAlphasTurn(Boolean value) {
        onlyAlphasTurnOverride = value;
    }

    /** For tests: pins {@link #opacLinkEnabled()}; null clears it. */
    public static void overrideOpacLink(Boolean value) {
        opacLinkOverride = value;
    }

    public static boolean opacLinkEnabled() {
        Boolean pinned = opacLinkOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return OPAC_LINK_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_OPAC_LINK;
        }
    }

    /** For tests: pins {@link #maxStackedSparks()}; null clears it. */
    public static void overrideMaxStackedSparks(Integer value) {
        maxStackedSparksOverride = value;
    }

    public static int maxStackedSparks() {
        Integer pinned = maxStackedSparksOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return MAX_STACKED_SPARKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_MAX_STACKED_SPARKS;
        }
    }

    /** For tests: pins {@link #duelReconnectSeconds()}; null clears it. */
    public static void overrideDuelReconnectSeconds(Integer value) {
        duelReconnectOverride = value;
    }

    public static int duelReconnectSeconds() {
        Integer pinned = duelReconnectOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return DUEL_RECONNECT_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_DUEL_RECONNECT_SECONDS;
        }
    }

    /** For tests: pins {@link #leftPackGraceMinutes()}; null clears it. */
    public static void overrideLeftPackGrace(Integer value) {
        leftPackGraceOverride = value;
    }

    public static int leftPackGraceMinutes() {
        Integer pinned = leftPackGraceOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return LEFT_PACK_GRACE_MINUTES.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_LEFT_PACK_GRACE_MINUTES;
        }
    }

    /** For tests: pins {@link #absentLeaderDays()}; null clears it. */
    public static void overrideAbsentLeaderDays(Integer value) {
        absentLeaderDaysOverride = value;
    }

    public static int absentLeaderDays() {
        Integer pinned = absentLeaderDaysOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return ABSENT_LEADER_DAYS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_ABSENT_LEADER_DAYS;
        }
    }

    public static boolean onlyAlphasTurn() {
        Boolean pinned = onlyAlphasTurnOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return ONLY_ALPHAS_TURN.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_ONLY_ALPHAS_TURN;
        }
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

    private static volatile net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode fullMoonSleepOverride;
    private static volatile Integer delayedAwakeningOverride;

    /** For tests: pins {@link #fullMoonSleep()}; null clears it. */
    @VisibleForTesting
    public static void overrideFullMoonSleep(net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode value) {
        fullMoonSleepOverride = value;
    }

    /** {@code full_moon_sleep}. */
    public static net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode fullMoonSleep() {
        var pinned = fullMoonSleepOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode.parse(FULL_MOON_SLEEP.get());
        } catch (IllegalStateException notLoaded) {
            return net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode.OFF;
        }
    }

    /** For tests: pins {@link #delayedAwakeningNights()}; null clears it. */
    @VisibleForTesting
    public static void overrideDelayedAwakeningNights(Integer value) {
        delayedAwakeningOverride = value;
    }

    /** {@code delayed_awakening_nights}. */
    public static int delayedAwakeningNights() {
        Integer pinned = delayedAwakeningOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return DELAYED_AWAKENING_NIGHTS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_DELAYED_AWAKENING_NIGHTS;
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

    private static volatile Boolean originalLineDeathOverride;

    /** For tests: pins {@link #originalLineDeath()}; null clears it. */
    @VisibleForTesting
    public static void overrideOriginalLineDeath(Boolean value) {
        originalLineDeathOverride = value;
    }

    public static boolean originalLineDeath() {
        Boolean pinned = originalLineDeathOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return ORIGINAL_LINE_DEATH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_ORIGINAL_LINE_DEATH;
        }
    }

    private static volatile Boolean originalVisibilityAlwaysOverride;

    /** For tests: pins {@link #originalVisibilityAlways()}; null clears it. */
    @VisibleForTesting
    public static void overrideOriginalVisibility(Boolean always) {
        originalVisibilityAlwaysOverride = always;
    }

    /** {@code original_visibility = always}; false is the default {@code discovered}. */
    public static boolean originalVisibilityAlways() {
        Boolean pinned = originalVisibilityAlwaysOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return LINE_ALWAYS.equals(ORIGINAL_VISIBILITY.get());
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    private static volatile Boolean lineRitesOverride;

    /** For tests: pins {@link #lineRitesEnabled()}; null clears it. */
    @VisibleForTesting
    public static void overrideLineRites(Boolean value) {
        lineRitesOverride = value;
    }

    public static boolean lineRitesEnabled() {
        Boolean pinned = lineRitesOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return LINE_RITES_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_LINE_RITES;
        }
    }

    private static volatile List<String> npcLineWeightsOverride;

    /** For tests: pins {@link #npcLineWeights()}; null clears it. */
    @VisibleForTesting
    public static void overrideNpcLineWeights(List<String> value) {
        npcLineWeightsOverride = value;
    }

    public static List<String> npcLineWeights() {
        List<String> pinned = npcLineWeightsOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return List.copyOf(NPC_LINE_WEIGHTS.get());
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NPC_LINE_WEIGHTS;
        }
    }

    private static volatile Integer ringRadiusOverride;

    /** For tests: pins {@link #ringRadius()}; null clears it. */
    @VisibleForTesting
    public static void overrideRingRadius(Integer value) {
        ringRadiusOverride = value;
    }

    public static int ringRadius() {
        Integer pinned = ringRadiusOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return RING_MAX_RADIUS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_RING_RADIUS;
        }
    }

    private static volatile Integer imprisonmentSecondsOverride;

    /** For tests: pins {@link #imprisonmentSeconds()}; null clears it. */
    @VisibleForTesting
    public static void overrideImprisonmentSeconds(Integer value) {
        imprisonmentSecondsOverride = value;
    }

    public static int imprisonmentSeconds() {
        Integer pinned = imprisonmentSecondsOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return IMPRISONMENT_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_IMPRISONMENT_SECONDS;
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
