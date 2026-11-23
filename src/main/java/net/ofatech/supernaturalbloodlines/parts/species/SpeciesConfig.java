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
    private static final ModConfigSpec.BooleanValue NPC_PACKS_ENABLED;
    private static final ModConfigSpec.DoubleValue NPC_PACK_CHANCE;
    private static final ModConfigSpec.DoubleValue NPC_LONE_WOLF_CHANCE;
    private static final ModConfigSpec.DoubleValue NPC_PACK_ALPHA_CHANCE;
    private static final ModConfigSpec.IntValue NPC_PACK_MIN_SIZE;
    private static final ModConfigSpec.IntValue NPC_PACK_MAX_SIZE;
    private static final ModConfigSpec.IntValue NPC_MAX_PACKS_PER_NIGHT;
    private static final ModConfigSpec.BooleanValue NPC_PACK_DAWN_DESPAWN;
    private static final ModConfigSpec.DoubleValue FERAL_ATTACK_FACTOR;
    private static final ModConfigSpec.DoubleValue FERAL_SPEED_FACTOR;
    private static final ModConfigSpec.BooleanValue NPC_ALPHA_SPARK_ENABLED;
    private static final ModConfigSpec.IntValue WEAK_SPARK_GROW_DAYS;
    private static final ModConfigSpec.IntValue WEAK_SPARK_MAX_HELD;
    private static final ModConfigSpec.IntValue WEAK_SPARK_COOLDOWN_DAYS;
    private static final ModConfigSpec.BooleanValue MOONBLOOD_SURGE_ENABLED;
    private static final ModConfigSpec.BooleanValue HYBRID_PACK_MOON_CONTROL;
    private static final ModConfigSpec.BooleanValue LOCATOR_ENABLED;
    private static final ModConfigSpec.IntValue LOCATOR_MINUTES;
    private static final ModConfigSpec.IntValue LOCATOR_RANGE;
    private static final ModConfigSpec.IntValue LOCATOR_BLUR;
    private static final ModConfigSpec.BooleanValue LOCATOR_TELL_TARGET;
    private static final java.util.Map<String, Object> LOCATOR_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile Boolean hybridPackMoonOverride;
    private static final ModConfigSpec.BooleanValue WOLF_SPIRITS_ENABLED;
    private static final ModConfigSpec.BooleanValue NPC_RECRUIT_ENABLED;
    private static final ModConfigSpec.IntValue MAX_NPC_MEMBERS_PER_PLAYER;
    private static final ModConfigSpec.IntValue MAX_NPC_MEMBERS_PER_PACK;
    private static final ModConfigSpec.BooleanValue NPC_HUNT_TRIPS;
    private static final ModConfigSpec.IntValue NPC_HUNT_PERIOD_MINUTES;
    private static final ModConfigSpec.BooleanValue NPC_CHEST_SORT;
    private static final ModConfigSpec.DoubleValue NPC_WEIGHT_BROTHER;
    private static final ModConfigSpec.DoubleValue NPC_WEIGHT_TENSURA;
    private static final ModConfigSpec.DoubleValue NPC_BONUS_SHARE;
    private static volatile MemberTuning memberOverride;
    /** The shortest and longest a snapped neck may keep a target down (R214), for every one of the four wake-time keys. */
    public static final double DEFAULT_HYBRID_LEAP_RANGE = 12.0;
    public static final double DEFAULT_HYBRID_LEAP_DAMAGE = 6.0;
    public static final int DEFAULT_HYBRID_LEAP_WEAKNESS_TICKS = 100;
    public static final int DEFAULT_HYBRID_LEAP_COOLDOWN_SECONDS = 20;
    public static final int MIN_SNAP_SECONDS = 5;
    public static final int MAX_SNAP_SECONDS = 1800;
    public static final int DEFAULT_NECK_SNAP_GRACE_SECONDS = 30;
    private static final ModConfigSpec.DoubleValue HYBRID_LEAP_RANGE;
    private static final ModConfigSpec.DoubleValue HYBRID_LEAP_DAMAGE;
    private static final ModConfigSpec.IntValue HYBRID_LEAP_WEAKNESS_TICKS;
    private static final ModConfigSpec.IntValue HYBRID_LEAP_COOLDOWN_SECONDS;
    private static final ModConfigSpec.IntValue NECK_SNAP_GRACE_SECONDS;
    private static volatile SnapSeconds snapOverride;

    /** The four wake times, in seconds, by kind. */
    public record SnapSeconds(int vampire, int augustine, int hybrid, int original) {
        public int longest() {
            return Math.max(Math.max(vampire, augustine), Math.max(hybrid, original));
        }
    }

    /** A test's own wake times (null: back to the config). */
    public static void overrideSnapSeconds(@org.jetbrains.annotations.Nullable SnapSeconds seconds) {
        snapOverride = seconds;
    }

    /** The four wake times in force: a test's override, else the config. */
    public static SnapSeconds snapSeconds() {
        SnapSeconds o = snapOverride;
        return o != null ? o : new SnapSeconds(neckSnapVampireSeconds(), neckSnapAugustineSeconds(), neckSnapHybridSeconds(), neckSnapOriginalSeconds());
    }

    /** {@code hybrid_leap_range}. */
    public static double hybridLeapRange() {
        try {
            return HYBRID_LEAP_RANGE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_HYBRID_LEAP_RANGE;
        }
    }

    /** {@code hybrid_leap_damage}. */
    public static double hybridLeapDamage() {
        try {
            return HYBRID_LEAP_DAMAGE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_HYBRID_LEAP_DAMAGE;
        }
    }

    /** {@code hybrid_leap_weakness_ticks}. */
    public static int hybridLeapWeaknessTicks() {
        try {
            return HYBRID_LEAP_WEAKNESS_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_HYBRID_LEAP_WEAKNESS_TICKS;
        }
    }

    /** {@code hybrid_leap_cooldown_seconds}. */
    public static int hybridLeapCooldownSeconds() {
        try {
            return HYBRID_LEAP_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_HYBRID_LEAP_COOLDOWN_SECONDS;
        }
    }

    /** {@code neck_snap_grace_seconds}. */
    public static int neckSnapGraceSeconds() {
        try {
            return NECK_SNAP_GRACE_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_GRACE_SECONDS;
        }
    }
    public static final int DEFAULT_BOTTLE_BLOOD_COOLDOWN_SECONDS = 60;
    public static final double DEFAULT_BOTTLE_BLOOD_HEALTH_COST = 3.0;
    private static final ModConfigSpec.IntValue BOTTLE_BLOOD_COOLDOWN_SECONDS;
    private static final ModConfigSpec.DoubleValue BOTTLE_BLOOD_HEALTH_COST;
    public static final int DEFAULT_BOTTLE_BLOOD_DAILY_LIMIT = 8;
    private static final ModConfigSpec.IntValue BOTTLE_BLOOD_DAILY_LIMIT;
    public static final int DEFAULT_GIVE_BLOOD_COOLDOWN_SECONDS = 120;
    public static final double DEFAULT_FORCE_FEED_HEALTH_LIMIT = 0.30;
    public static final boolean DEFAULT_VAMPIRE_BLOOD_BUFF_ENABLED = true;
    public static final int DEFAULT_VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS = 600;
    private static final ModConfigSpec.IntValue VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS;
    private static final ModConfigSpec.IntValue GIVE_BLOOD_COOLDOWN_SECONDS;
    private static final ModConfigSpec.DoubleValue FORCE_FEED_HEALTH_LIMIT;
    private static final ModConfigSpec.BooleanValue VAMPIRE_BLOOD_BUFF_ENABLED;

    public static final double DEFAULT_AUGUSTINE_ATTACK_BONUS = 0.10;
    public static final double DEFAULT_AUGUSTINE_EXTRA_HEALTH = 2.0;
    public static final double DEFAULT_AUGUSTINE_BLOOD_FROM_VAMPIRE = 3.0;
    public static final double DEFAULT_AUGUSTINE_BLOOD_FROM_OTHER = 1.0 / 3.0;
    private static final ModConfigSpec.DoubleValue AUGUSTINE_ATTACK_BONUS;
    private static final ModConfigSpec.DoubleValue AUGUSTINE_EXTRA_HEALTH;
    private static final ModConfigSpec.DoubleValue AUGUSTINE_BLOOD_FROM_VAMPIRE;
    private static final ModConfigSpec.DoubleValue AUGUSTINE_BLOOD_FROM_OTHER;
    public static final double DEFAULT_RIP_HEART_HEALTH = 10.0;
    public static final int DEFAULT_RIP_HEART_COOLDOWN_SECONDS = 60;
    public static final int DEFAULT_NECK_SNAP_COOLDOWN_SECONDS = 60;
    public static final double DEFAULT_NECK_SNAP_KILL_HEALTH = 10.0;
    public static final int DEFAULT_NECK_SNAP_STAGGER_TICKS = 60;
    public static final int DEFAULT_NECK_SNAP_VAMPIRE_SECONDS = 60;
    public static final int DEFAULT_NECK_SNAP_AUGUSTINE_SECONDS = 60;
    public static final int DEFAULT_NECK_SNAP_HYBRID_SECONDS = 60;
    public static final int DEFAULT_NECK_SNAP_ORIGINAL_SECONDS = 60;
    private static final ModConfigSpec.DoubleValue RIP_HEART_HEALTH;
    private static final ModConfigSpec.IntValue RIP_HEART_COOLDOWN_SECONDS;
    private static final ModConfigSpec.IntValue NECK_SNAP_COOLDOWN_SECONDS;
    private static final ModConfigSpec.DoubleValue NECK_SNAP_KILL_HEALTH;
    private static final ModConfigSpec.IntValue NECK_SNAP_STAGGER_TICKS;
    private static final ModConfigSpec.IntValue NECK_SNAP_VAMPIRE_SECONDS;
    private static final ModConfigSpec.IntValue NECK_SNAP_AUGUSTINE_SECONDS;
    private static final ModConfigSpec.IntValue NECK_SNAP_HYBRID_SECONDS;
    private static final ModConfigSpec.IntValue NECK_SNAP_ORIGINAL_SECONDS;
    public static final int DEFAULT_VENOM_SICK_AFTER_TICKS = 4000;
    private static final ModConfigSpec.IntValue VENOM_SICK_AFTER_TICKS;
    private static volatile Integer venomSickAfterOverride;

    /** For tests: pins {@link #venomSickAfterTicks()}; null clears it. */
    public static void overrideVenomSickAfterTicks(Integer value) {
        venomSickAfterOverride = value;
    }

    /** {@code venom_sick_after_ticks}. */
    public static int venomSickAfterTicks() {
        Integer pinned = venomSickAfterOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return VENOM_SICK_AFTER_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VENOM_SICK_AFTER_TICKS;
        }
    }

    /** The numbers of the pack's NPC members (plan 15d, R148). */
    public record MemberTuning(boolean recruit, int perPlayer, int perPack, boolean huntTrips, int huntPeriodMinutes, boolean chestSort, double weightBrother,
                               double weightTensura, double bonusShare) {
        public static final MemberTuning DEFAULT = new MemberTuning(true, 2, 6, true, 10, true, 0.5, 0.25, 0.4);
    }

    /** For tests: pins {@link #members()}; null clears it. */
    @VisibleForTesting
    public static void overrideMembers(MemberTuning value) {
        memberOverride = value;
    }

    public static MemberTuning members() {
        MemberTuning pinned = memberOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return new MemberTuning(NPC_RECRUIT_ENABLED.get(), MAX_NPC_MEMBERS_PER_PLAYER.get(), MAX_NPC_MEMBERS_PER_PACK.get(), NPC_HUNT_TRIPS.get(),
                    NPC_HUNT_PERIOD_MINUTES.get(), NPC_CHEST_SORT.get(), NPC_WEIGHT_BROTHER.get(), NPC_WEIGHT_TENSURA.get(), NPC_BONUS_SHARE.get());
        } catch (IllegalStateException notLoaded) {
            return MemberTuning.DEFAULT;
        }
    }

    private static final ModConfigSpec.BooleanValue ALPHA_TRIAL_ENABLED;
    private static final ModConfigSpec.IntValue ALPHA_TRIAL_MIN_LEVEL;
    private static final ModConfigSpec.DoubleValue ALPHA_TRIAL_HP_FACTOR;
    private static volatile TrialTuning trialOverride;

    /** The numbers of the Alpha Trial (plan 15c). */
    public record TrialTuning(boolean enabled, int minLevel, double hpFactor) {
        public static final TrialTuning DEFAULT = new TrialTuning(true, 10, 3.0);
    }

    /** For tests: pins {@link #trial()}; null clears it. */
    @VisibleForTesting
    public static void overrideTrial(TrialTuning value) {
        trialOverride = value;
    }

    /** R96: whether the werewolf members of a pack led by a hybrid-like leader have moon control ({@code hybrid_pack_moon_control}). */
    public static boolean hybridPackMoonControl() {
        Boolean pinned = hybridPackMoonOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return HYBRID_PACK_MOON_CONTROL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** For tests: pins one locator setting by its key ({@code locator_range}, ...); null clears it. */
    @VisibleForTesting
    public static void overrideLocator(String key, Object value) {
        if (value == null) {
            LOCATOR_OVERRIDES.remove(key);
        } else {
            LOCATOR_OVERRIDES.put(key, value);
        }
    }

    private static boolean locatorBool(String key, ModConfigSpec.BooleanValue value, boolean fallback) {
        if (LOCATOR_OVERRIDES.get(key) instanceof Boolean pinned) {
            return pinned;
        }
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }

    private static int locatorInt(String key, ModConfigSpec.IntValue value, int fallback) {
        if (LOCATOR_OVERRIDES.get(key) instanceof Integer pinned) {
            return pinned;
        }
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }

    public static boolean locatorEnabled() {
        return locatorBool("locator_enabled", LOCATOR_ENABLED, true);
    }

    public static int locatorMinutes() {
        return locatorInt("locator_minutes", LOCATOR_MINUTES, 30);
    }

    public static int locatorRange() {
        return locatorInt("locator_range", LOCATOR_RANGE, 2000);
    }

    public static int locatorBlur() {
        return locatorInt("locator_blur", LOCATOR_BLUR, 32);
    }

    public static boolean locatorTellTarget() {
        return locatorBool("locator_tell_target", LOCATOR_TELL_TARGET, true);
    }

    /** For tests: pins {@link #hybridPackMoonControl()}; null clears it. */
    @VisibleForTesting
    public static void overrideHybridPackMoon(Boolean value) {
        hybridPackMoonOverride = value;
    }

    public static TrialTuning trial() {
        TrialTuning pinned = trialOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return new TrialTuning(ALPHA_TRIAL_ENABLED.get(), ALPHA_TRIAL_MIN_LEVEL.get(), ALPHA_TRIAL_HP_FACTOR.get());
        } catch (IllegalStateException notLoaded) {
            return TrialTuning.DEFAULT;
        }
    }

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
        NPC_PACKS_ENABLED = b.comment("R143: wild NPC werewolf packs (Werewolves' own Alpha leading wolves) and lone wolves spawn near players on full-moon nights. Off: none, and the Alphas of the Werewolves mod give no weaker spark either way (that has its own switch).")
                .define("npc_packs_enabled", true);
        NPC_PACK_CHANCE = b.comment("Chance per player per full-moon night that a pack spawns near them. A Blood Moon night that is not a full moon rolls at blood_moon_pack_chance_factor of it (celestials config).")
                .defineInRange("npc_pack_chance", 0.35, 0.0, 1.0);
        NPC_LONE_WOLF_CHANCE = b.comment("Chance per player per full-moon night, apart from the pack, that a lone werewolf spawns near them.")
                .defineInRange("npc_lone_wolf_chance", 0.5, 0.0, 1.0);
        NPC_PACK_ALPHA_CHANCE = b.comment("Chance that a spawned pack is led by an Alpha (which can drop a weaker spark). Without one it is leaderless wolves.")
                .defineInRange("npc_pack_alpha_chance", 0.7, 0.0, 1.0);
        NPC_PACK_MIN_SIZE = b.comment("Fewest wolves in a spawned pack (not counting the Alpha).")
                .defineInRange("npc_pack_min_size", 3, 1, 8);
        NPC_PACK_MAX_SIZE = b.comment("Most wolves in a spawned pack (not counting the Alpha). An Alpha can lead only as many as the Werewolves mod lets it.")
                .defineInRange("npc_pack_max_size", 5, 1, 8);
        NPC_MAX_PACKS_PER_NIGHT = b.comment("Most packs one dimension may spawn in one night, so a big server is not buried.")
                .defineInRange("npc_max_packs_per_night", 3, 0, 20);
        NPC_PACK_DAWN_DESPAWN = b.comment("Wild NPC wolves with no player within 24 blocks leave at dawn. Recruited ones never do.")
                .define("npc_pack_dawn_despawn", true);
        FERAL_ATTACK_FACTOR = b.comment("R144: a wild werewolf on a dark full-moon night hits this many times as hard. 1 = no change.")
                .defineInRange("feral_attack_factor", 1.2, 1.0, 3.0);
        FERAL_SPEED_FACTOR = b.comment("R144: a wild werewolf on a dark full-moon night moves this many times as fast. 1 = no change.")
                .defineInRange("feral_speed_factor", 1.1, 1.0, 2.0);
        NPC_ALPHA_SPARK_ENABLED = b.comment("R145: killing an Alpha of the Werewolves mod (a natural forest one or a pack's) gives a werewolf a weaker spark that grows while they are online. Off: it gives nothing.")
                .define("npc_alpha_spark_enabled", true);
        WEAK_SPARK_GROW_DAYS = b.comment("Online days a weaker spark takes to grow into a normal one.")
                .defineInRange("weak_spark_grow_days", 3, 1, 30);
        WEAK_SPARK_MAX_HELD = b.comment("Most weaker sparks one player may hold at a time.")
                .defineInRange("weak_spark_max_held", 2, 1, 5);
        WEAK_SPARK_COOLDOWN_DAYS = b.comment("Game days between two weaker sparks for one player, so a forest Alpha is not farmed. 0 = none.")
                .defineInRange("weak_spark_cooldown_days", 3, 0, 30);
        MOONBLOOD_SURGE_ENABLED = b.comment("R146: Moonblood Surge, a werewolf in a pack buffs the pack within 24 blocks and pays for it afterwards (/pack surge).")
                .define("moonblood_surge_enabled", true);
        WOLF_SPIRITS_ENABLED = b.comment("R146: Wolf Spirits, an Alpha calls three spirit wolves that fight for the pack for three minutes (/pack spirits).")
                .define("wolf_spirits_enabled", true);
        HYBRID_PACK_MOON_CONTROL = b.comment("R96: while the pack's current leader is a hybrid, an Original hybrid or a tribrid, every werewolf member of the pack has moon control (as with a Moon Ring). Lost on leaving the pack or when the lead passes to someone else. Off: no pack gift.")
                .define("hybrid_pack_moon_control", true);
        LOCATOR_ENABLED = b.comment("R315: the Rites of the Locator (a witch finds somebody by their taglock or blood) and the Vampire Compass's third mode. Off: the rites are refused and an attuned compass spins.")
                .define("locator_enabled", true);
        LOCATOR_MINUTES = b.comment("R316: how long a compass stays attuned to somebody, in real minutes.")
                .defineInRange("locator_minutes", 30, 1, 1440);
        LOCATOR_RANGE = b.comment("R316: how far away, in blocks, the locator can find somebody (same dimension). 0: the whole dimension.")
                .defineInRange("locator_range", 2000, 0, 30_000_000);
        LOCATOR_BLUR = b.comment("R316: the needle points at the middle of a square this many blocks wide, never at the exact spot.")
                .defineInRange("locator_blur", 32, 8, 512);
        LOCATOR_TELL_TARGET = b.comment("R317: the person being looked for is told once ('Somebody is looking for you.', no name, no place) and again when it ends.")
                .define("locator_tell_target", true);
        ALPHA_TRIAL_ENABLED = b.comment("R147: the Alpha Trial rite (a full-moon night, a transformed Beta or Omega) calls a boss; winning gives a new Alpha spark. Off: the rite refuses.")
                .define("alpha_trial_enabled", true);
        ALPHA_TRIAL_MIN_LEVEL = b.comment("Lowest werewolf level that may try the Alpha Trial.")
                .defineInRange("alpha_trial_min_level", 10, 1, 14);
        ALPHA_TRIAL_HP_FACTOR = b.comment("The trial boss (an Alpha of the Werewolves mod) has this many times the health of an ordinary one.")
                .defineInRange("alpha_trial_hp_factor", 3.0, 1.0, 10.0);
        NPC_RECRUIT_ENABLED = b.comment("R148: a werewolf may take a beaten wild wolf into their pack with the Rite of the Werewolf Brother. Off: the rite refuses; members already recruited stay.")
                .define("npc_recruit_enabled", true);
        MAX_NPC_MEMBERS_PER_PLAYER = b.comment("Most wolves one werewolf may recruit with the Rite of the Werewolf Brother.")
                .defineInRange("max_npc_members_per_player", 2, 0, 8);
        MAX_NPC_MEMBERS_PER_PACK = b.comment("Most recruited wolves in one whole pack, whoever recruited them.")
                .defineInRange("max_npc_members_per_pack", 6, 0, 24);
        NPC_HUNT_TRIPS = b.comment("A Brother with Hunt on, resting or on guard at night, kills a few animals near its post now and then and puts what they drop in a chest within 12 blocks. Off: none.")
                .define("npc_hunt_trips", true);
        NPC_HUNT_PERIOD_MINUTES = b.comment("Minutes between two hunting trips of one Brother.")
                .defineInRange("npc_hunt_period_minutes", 10, 1, 120);
        NPC_CHEST_SORT = b.comment("A Brother with Sort on tidies the chest it stores a hunt in: partial stacks merged, then ordered. Off: it only drops what it carries in.")
                .define("npc_chest_sort", true);
        NPC_WEIGHT_BROTHER = b.comment("What a recruited Brother counts for toward the pack bonus (a player counts 1).")
                .defineInRange("npc_weight_brother", 0.5, 0.0, 1.0);
        NPC_WEIGHT_TENSURA = b.comment("What a Tensura subordinate of a pack member counts for toward the pack bonus (a player counts 1).")
                .defineInRange("npc_weight_tensura", 0.25, 0.0, 1.0);
        NPC_BONUS_SHARE = b.comment("The most of the pack bonus's cap that NPC members may supply: 0.4 means two of the five 5% steps. So wolves alone never reach the full bonus.")
                .defineInRange("npc_bonus_share", 0.4, 0.0, 1.0);
        BOTTLE_BLOOD_COOLDOWN_SECONDS = b.comment("R211: seconds before a vampire (not an Original or an Original hybrid) can bottle their own blood again with the Bottle my blood action.")
                .defineInRange("bottle_blood_cooldown_seconds", DEFAULT_BOTTLE_BLOOD_COOLDOWN_SECONDS, 1, 3600);
        BOTTLE_BLOOD_HEALTH_COST = b.comment("R211: health (in half hearts) Bottle my blood costs. It never kills: it stops at 1 health.")
                .defineInRange("bottle_blood_health_cost", DEFAULT_BOTTLE_BLOOD_HEALTH_COST, 0.0, 19.0);
        BOTTLE_BLOOD_DAILY_LIMIT = b.comment("R211: how many times one player may use Bottle my blood in one game day, counted in world time; sleeping or /time does not reset it. Vampire blood bottles are an ingredient (pure blood, vampire blood oil) and a task item of Vampirism, so they are not free.")
                .defineInRange("bottle_blood_daily_limit", DEFAULT_BOTTLE_BLOOD_DAILY_LIMIT, 1, 1000);
        GIVE_BLOOD_COOLDOWN_SECONDS = b.comment("R208: seconds before a vampire can use Force-feed my blood again, whatever came of it.")
                .defineInRange("give_blood_cooldown_seconds", DEFAULT_GIVE_BLOOD_COOLDOWN_SECONDS, 1, 3600);
        FORCE_FEED_HEALTH_LIMIT = b.comment("R208: an unwilling player can be fed vampire blood only while PvP is on and they are down or at or under this fraction of their health (0.30 = 30%). A target who sneaks accepts at any health.")
                .defineInRange("force_feed_health_limit", DEFAULT_FORCE_FEED_HEALTH_LIMIT, 0.0, 1.0);
        VAMPIRE_BLOOD_BUFF_ENABLED = b.comment("R207: vampire blood in a player's system eases their hunger for the whole window (vampire_blood_ticks) and, once per vampire_blood_regen_cooldown_seconds, gives 90 seconds of Regeneration II. Anyone the blood marks gets it, a bottle included. Off: the blood only marks them.")
                .define("vampire_blood_buff_enabled", DEFAULT_VAMPIRE_BLOOD_BUFF_ENABLED);
        VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS = b.comment("R207: a player gets the 90 second Regeneration II pulse of vampire blood once per this many seconds, whatever the source (a forced feed, a bottle, a bite), so drinking more does not keep it going. 0 = every drink gives it.")
                .defineInRange("vampire_blood_regen_cooldown_seconds", DEFAULT_VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS, 0, 86400);
        AUGUSTINE_ATTACK_BONUS = b.comment("R200: an Augustine's extra attack damage, as a fraction (0.10 = +10%; an Original has +20%).")
                .defineInRange("augustine_attack_bonus", DEFAULT_AUGUSTINE_ATTACK_BONUS, 0.0, 1.0);
        AUGUSTINE_EXTRA_HEALTH = b.comment("R200: an Augustine's extra maximum health, in half hearts (an Original has 4).")
                .defineInRange("augustine_extra_health", DEFAULT_AUGUSTINE_EXTRA_HEALTH, 0.0, 20.0);
        AUGUSTINE_BLOOD_FROM_VAMPIRE = b.comment("R203: how much more blood an Augustine takes from a vampire (3 = triple of what Vampirism gives).")
                .defineInRange("augustine_blood_from_vampire_factor", DEFAULT_AUGUSTINE_BLOOD_FROM_VAMPIRE, 0.0, 10.0);
        AUGUSTINE_BLOOD_FROM_OTHER = b.comment("R203: how much blood an Augustine takes from anyone who is not a vampire (a third of what Vampirism gives, never less than 1 from a bite that gives any).")
                .defineInRange("augustine_blood_from_other_factor", DEFAULT_AUGUSTINE_BLOOD_FROM_OTHER, 0.0, 10.0);
        RIP_HEART_HEALTH = b.comment("R204: a target at or under this many health (half hearts) has its heart torn out by Rip Heart; above it the action is refused with no cooldown. Originals never die of it.")
                .defineInRange("rip_heart_health", DEFAULT_RIP_HEART_HEALTH, 1.0, 40.0);
        RIP_HEART_COOLDOWN_SECONDS = b.comment("R204: seconds before Rip Heart can be used again (a target that is too strong costs nothing).")
                .defineInRange("rip_heart_cooldown_seconds", DEFAULT_RIP_HEART_COOLDOWN_SECONDS, 1, 3600);
        NECK_SNAP_COOLDOWN_SECONDS = b.comment("R214: seconds before Neck Snap can be used again, whatever it did. It is never shorter than the longest of the four wake times (a snap must not be able to be renewed while it lasts), whatever is set here.")
                .defineInRange("neck_snap_cooldown_seconds", DEFAULT_NECK_SNAP_COOLDOWN_SECONDS, 1, 3600);
        NECK_SNAP_GRACE_SECONDS = b.comment("R214: seconds after a snap ends in which the same player or creature cannot be snapped again (a refusal like any other, so it says nothing about the target). Kept through a logout. 0 turns it off.")
                .defineInRange("neck_snap_grace_seconds", DEFAULT_NECK_SNAP_GRACE_SECONDS, 0, 3600);
        HYBRID_LEAP_RANGE = b.comment("R212: how far (blocks) Hybrid Leap reaches. Further than this is refused as too far; the leap itself is a push, so a wall or a drop on the way still counts.")
                .defineInRange("hybrid_leap_range", DEFAULT_HYBRID_LEAP_RANGE, 2.0, net.ofatech.supernaturalbloodlines.core.hud.LeapMath.MAX_RANGE);
        HYBRID_LEAP_DAMAGE = b.comment("R212: the damage Hybrid Leap deals when it lands (half hearts), as an ordinary attack: PvP, claims, armour and totems apply. 0 deals none and keeps the Weakness.")
                .defineInRange("hybrid_leap_damage", DEFAULT_HYBRID_LEAP_DAMAGE, 0.0, 40.0);
        HYBRID_LEAP_WEAKNESS_TICKS = b.comment("R212: how long the Weakness lasts that a landed Hybrid Leap gives (ticks). Only when the blow landed.")
                .defineInRange("hybrid_leap_weakness_ticks", DEFAULT_HYBRID_LEAP_WEAKNESS_TICKS, 0, 1200);
        HYBRID_LEAP_COOLDOWN_SECONDS = b.comment("R212: seconds before Hybrid Leap can be used again. A refusal costs nothing.")
                .defineInRange("hybrid_leap_cooldown_seconds", DEFAULT_HYBRID_LEAP_COOLDOWN_SECONDS, 1, 600);
        NECK_SNAP_KILL_HEALTH = b.comment("R214: a mortal at or under this much health dies of a snapped neck; a healthier one only staggers.")
                .defineInRange("neck_snap_kill_health", DEFAULT_NECK_SNAP_KILL_HEALTH, 1.0, 40.0);
        NECK_SNAP_STAGGER_TICKS = b.comment("R214: how long a healthy mortal staggers (Slowness III and Weakness) after a snapped neck, in ticks.")
                .defineInRange("neck_snap_stagger_ticks", DEFAULT_NECK_SNAP_STAGGER_TICKS, 0, 600);
        NECK_SNAP_VAMPIRE_SECONDS = b.comment("R214: how long a snapped vampire (plain, heretic, Ghoul) lies as if dead. The four wake times are equal by default, so the length does not say what the target is.")
                .defineInRange("neck_snap_vampire_seconds", DEFAULT_NECK_SNAP_VAMPIRE_SECONDS, MIN_SNAP_SECONDS, MAX_SNAP_SECONDS);
        NECK_SNAP_AUGUSTINE_SECONDS = b.comment("R214: how long a snapped Augustine lies unconscious.")
                .defineInRange("neck_snap_augustine_seconds", DEFAULT_NECK_SNAP_AUGUSTINE_SECONDS, MIN_SNAP_SECONDS, MAX_SNAP_SECONDS);
        NECK_SNAP_HYBRID_SECONDS = b.comment("R214: how long a snapped hybrid or tribrid lies in temporary death.")
                .defineInRange("neck_snap_hybrid_seconds", DEFAULT_NECK_SNAP_HYBRID_SECONDS, MIN_SNAP_SECONDS, MAX_SNAP_SECONDS);
        NECK_SNAP_ORIGINAL_SECONDS = b.comment("R214: how long a snapped Original or Original hybrid lies down. Keep it equal to the others, or an observer can tell an Original by the time.")
                .defineInRange("neck_snap_original_seconds", DEFAULT_NECK_SNAP_ORIGINAL_SECONDS, MIN_SNAP_SECONDS, MAX_SNAP_SECONDS);
        VENOM_SICK_AFTER_TICKS = b.comment("R206: how long into the 12000-tick werewolf venom the vampire starts to be sick (Nausea, Slowness I, hallucinations), in ticks. Before that only the 1 damage a pulse. 0 is sick from the start, 12000 never sick. Originals have no stages.")
                .defineInRange("venom_sick_after_ticks", DEFAULT_VENOM_SICK_AFTER_TICKS, 0, 12000);
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
    private static volatile NpcTuning npcOverride;

    /** The numbers of plan 15b: what the NPC packs, the weaker spark and the two pack abilities read. */
    public record NpcTuning(boolean packs, net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules.Tuning spawn, boolean dawnDespawn, double feralAttack,
                            double feralSpeed, boolean alphaSpark, int growDays, int maxHeld, int cooldownDays, boolean surge, boolean spirits) {
        public static final NpcTuning DEFAULT = new NpcTuning(true, new net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules.Tuning(0.35, 0.5, 0.7, 3, 5, 3),
                true, 1.2, 1.1, true, 3, 2, 3, true, true);
    }

    /** For tests: pins {@link #npc()}; null clears it. */
    @VisibleForTesting
    public static void overrideNpc(NpcTuning value) {
        npcOverride = value;
    }

    /** The NPC pack numbers; the defaults before the config loads. */
    public static NpcTuning npc() {
        NpcTuning pinned = npcOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return new NpcTuning(NPC_PACKS_ENABLED.get(), new net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules.Tuning(NPC_PACK_CHANCE.get(),
                    NPC_LONE_WOLF_CHANCE.get(), NPC_PACK_ALPHA_CHANCE.get(), NPC_PACK_MIN_SIZE.get(), NPC_PACK_MAX_SIZE.get(), NPC_MAX_PACKS_PER_NIGHT.get()),
                    NPC_PACK_DAWN_DESPAWN.get(), FERAL_ATTACK_FACTOR.get(), FERAL_SPEED_FACTOR.get(), NPC_ALPHA_SPARK_ENABLED.get(), WEAK_SPARK_GROW_DAYS.get(),
                    WEAK_SPARK_MAX_HELD.get(), WEAK_SPARK_COOLDOWN_DAYS.get(), MOONBLOOD_SURGE_ENABLED.get(), WOLF_SPIRITS_ENABLED.get());
        } catch (IllegalStateException notLoaded) {
            return NpcTuning.DEFAULT;
        }
    }

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

    /** {@code bottle_blood_cooldown_seconds}. */
    public static int bottleBloodCooldownSeconds() {
        try {
            return BOTTLE_BLOOD_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BOTTLE_BLOOD_COOLDOWN_SECONDS;
        }
    }

    /** {@code give_blood_cooldown_seconds}. */
    public static int giveBloodCooldownSeconds() {
        try {
            return GIVE_BLOOD_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_GIVE_BLOOD_COOLDOWN_SECONDS;
        }
    }

    /** {@code force_feed_health_limit}. */
    public static double forceFeedHealthLimit() {
        try {
            return FORCE_FEED_HEALTH_LIMIT.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_FORCE_FEED_HEALTH_LIMIT;
        }
    }

    private static volatile Boolean bloodBuffOverride;

    /** For tests: pins {@link #vampireBloodBuffEnabled()}; null clears it. */
    public static void overrideVampireBloodBuff(Boolean value) {
        bloodBuffOverride = value;
    }

    /** {@code vampire_blood_regen_cooldown_seconds}. */
    public static int vampireBloodRegenCooldownSeconds() {
        Integer pinned = regenCooldownOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VAMPIRE_BLOOD_REGEN_COOLDOWN_SECONDS;
        }
    }

    private static volatile Integer regenCooldownOverride;

    /** For tests: pins {@link #vampireBloodRegenCooldownSeconds()}; null clears it. */
    public static void overrideVampireBloodRegenCooldown(Integer value) {
        regenCooldownOverride = value;
    }

    /** {@code vampire_blood_buff_enabled}. */
    public static boolean vampireBloodBuffEnabled() {
        Boolean pinned = bloodBuffOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return VAMPIRE_BLOOD_BUFF_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_VAMPIRE_BLOOD_BUFF_ENABLED;
        }
    }

    /** {@code bottle_blood_daily_limit}. */
    public static int bottleBloodDailyLimit() {
        try {
            return BOTTLE_BLOOD_DAILY_LIMIT.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BOTTLE_BLOOD_DAILY_LIMIT;
        }
    }

    /** {@code augustine_attack_bonus}. */
    public static double augustineAttackBonus() {
        try {
            return AUGUSTINE_ATTACK_BONUS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_AUGUSTINE_ATTACK_BONUS;
        }
    }

    /** {@code augustine_extra_health}. */
    public static double augustineExtraHealth() {
        try {
            return AUGUSTINE_EXTRA_HEALTH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_AUGUSTINE_EXTRA_HEALTH;
        }
    }

    /** {@code augustine_blood_from_vampire_factor}. */
    public static double augustineBloodFromVampire() {
        try {
            return AUGUSTINE_BLOOD_FROM_VAMPIRE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_AUGUSTINE_BLOOD_FROM_VAMPIRE;
        }
    }

    /** {@code augustine_blood_from_other_factor}. */
    public static double augustineBloodFromOther() {
        try {
            return AUGUSTINE_BLOOD_FROM_OTHER.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_AUGUSTINE_BLOOD_FROM_OTHER;
        }
    }

    /** {@code rip_heart_health}. */
    public static double ripHeartHealth() {
        try {
            return RIP_HEART_HEALTH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_RIP_HEART_HEALTH;
        }
    }

    /** {@code rip_heart_cooldown_seconds}. */
    public static int ripHeartCooldownSeconds() {
        try {
            return RIP_HEART_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_RIP_HEART_COOLDOWN_SECONDS;
        }
    }

    /** {@code neck_snap_cooldown_seconds}. */
    public static int neckSnapCooldownSeconds() {
        try {
            return NECK_SNAP_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_COOLDOWN_SECONDS;
        }
    }

    /** {@code neck_snap_kill_health}. */
    public static double neckSnapKillHealth() {
        try {
            return NECK_SNAP_KILL_HEALTH.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_KILL_HEALTH;
        }
    }

    /** {@code neck_snap_stagger_ticks}. */
    public static int neckSnapStaggerTicks() {
        try {
            return NECK_SNAP_STAGGER_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_STAGGER_TICKS;
        }
    }

    /** {@code neck_snap_vampire_seconds}. */
    public static int neckSnapVampireSeconds() {
        try {
            return NECK_SNAP_VAMPIRE_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_VAMPIRE_SECONDS;
        }
    }

    /** {@code neck_snap_augustine_seconds}. */
    public static int neckSnapAugustineSeconds() {
        try {
            return NECK_SNAP_AUGUSTINE_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_AUGUSTINE_SECONDS;
        }
    }

    /** {@code neck_snap_hybrid_seconds}. */
    public static int neckSnapHybridSeconds() {
        try {
            return NECK_SNAP_HYBRID_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_HYBRID_SECONDS;
        }
    }

    /** {@code neck_snap_original_seconds}. */
    public static int neckSnapOriginalSeconds() {
        try {
            return NECK_SNAP_ORIGINAL_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_NECK_SNAP_ORIGINAL_SECONDS;
        }
    }

    /** {@code bottle_blood_health_cost}. */
    public static double bottleBloodHealthCost() {
        try {
            return BOTTLE_BLOOD_HEALTH_COST.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_BOTTLE_BLOOD_HEALTH_COST;
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
