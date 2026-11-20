package net.ofatech.supernaturalbloodlines.parts.bosses;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;

/** The bosses part's common config (plan 17). Every key has an Admin row ({@code AdminEntries}). Before the config loads the defaults answer. */
public final class BossesConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-bosses.toml";
    public static final String REWARD_FIRST_DEFEAT = "first_defeat";
    public static final String REWARD_DEATH = "death";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue BOSS_CHECK_SECONDS;
    private static final ModConfigSpec.BooleanValue BOSS_BARS_ENABLED;
    private static final ModConfigSpec.IntValue ORIGINAL_RETURN_DAYS;
    private static final ModConfigSpec.BooleanValue NPC_ORIGINAL_STAKE_NEEDS_OPEN;
    private static final ModConfigSpec.ConfigValue<String> BOSS_REWARD_ON;
    private static final ModConfigSpec.DoubleValue BOSS_HEALTH_FACTOR;
    private static final ModConfigSpec.DoubleValue BOSS_DAMAGE_FACTOR;
    private static final ModConfigSpec.IntValue MIKAEL_MIN_RUNG;
    private static final ModConfigSpec.DoubleValue MIKAEL_NIGHT_CHANCE;
    private static final ModConfigSpec.BooleanValue MIKAEL_STAKES_ORIGINALS;
    private static final ModConfigSpec.DoubleValue KLAUS_CHANCE_PER_FULL_MOON;
    private static final ModConfigSpec.BooleanValue KLAUS_HUNTS_PLAYERS;
    private static final ModConfigSpec.IntValue KLAUS_WARNING_SECONDS;
    private static final ModConfigSpec.DoubleValue ELIJAH_CHANCE_PER_DAY;
    private static final ModConfigSpec.IntValue ELIJAH_OATH_DAYS;
    private static final ModConfigSpec.DoubleValue ELIXIR_HEAL_FRACTION;
    private static final ModConfigSpec.IntValue REBEKAH_DELAY_SECONDS;
    private static final ModConfigSpec.IntValue KOL_MIN_MARK;
    private static final ModConfigSpec.IntValue KOL_CLOAK_MINUTES;
    private static final ModConfigSpec.IntValue KOL_GRIMOIRE_SP;
    private static final ModConfigSpec.DoubleValue FINN_CHANCE_PER_ECLIPSE;
    private static final ModConfigSpec.IntValue FINN_RITUAL_SECONDS;
    private static final ModConfigSpec.IntValue LINK_SECONDS;
    private static final ModConfigSpec.DoubleValue LINK_DAMAGE_SHARE;
    private static final ModConfigSpec.IntValue DEUCALION_MIN_SPARKS;
    private static final ModConfigSpec.IntValue DEUCALION_SPARKS;
    private static final ModConfigSpec.IntValue DEUCALION_PACK_ALPHAS;
    private static final ModConfigSpec.BooleanValue DEUCALION_RESPAWN;
    private static final ModConfigSpec.IntValue DEUCALION_RESPAWN_DAYS;
    private static final ModConfigSpec.BooleanValue PETER_HOUSE_ENABLED;
    private static final ModConfigSpec.DoubleValue PETER_HOUSE_CHANCE;
    private static final ModConfigSpec.BooleanValue PETER_FERAL_FULL_MOON;
    private static final ModConfigSpec.BooleanValue PETER_RESPAWN;
    private static final ModConfigSpec.IntValue PETER_RESPAWN_DAYS;
    private static final java.util.Map<BossId, ModConfigSpec.BooleanValue> ENABLED = new java.util.EnumMap<>(BossId.class);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        BOSS_CHECK_SECONDS = b.comment("How often the boss director looks at the world, in seconds.")
                .defineInRange("boss_check_seconds", 5, 1, 120);
        BOSS_BARS_ENABLED = b.comment("Show a boss bar to players near a boss.").define("boss_bars_enabled", true);
        ORIGINAL_RETURN_DAYS = b.comment("In-game days a desiccated NPC Original stays down before they get up again. Their blood is dropped at most once per window.")
                .defineInRange("original_return_days", 5, 1, 100);
        NPC_ORIGINAL_STAKE_NEEDS_OPEN = b.comment("White oak kills an NPC Original only while they are desiccated. Off: white oak kills a standing one too.")
                .define("npc_original_stake_needs_open", true);
        BOSS_REWARD_ON = b.comment("When a boss pays its reward (once): first_defeat = the first time it is brought down or killed, death = only when it dies.")
                .define("boss_reward_on", REWARD_FIRST_DEFEAT, v -> REWARD_FIRST_DEFEAT.equals(v) || REWARD_DEATH.equals(v));
        for (BossId boss : BossId.values()) {
            ENABLED.put(boss, b.comment("Whether " + boss.id() + " can appear. The three werewolf bosses and Klaus are inert without Werewolves.")
                    .define(boss.id() + "_enabled", true));
        }
        BOSS_HEALTH_FACTOR = b.comment("Every boss's health is multiplied by this.").defineInRange("boss_health_factor", 1.0, 0.1, 10.0);
        BOSS_DAMAGE_FACTOR = b.comment("Every boss's attack damage is multiplied by this.").defineInRange("boss_damage_factor", 1.0, 0.1, 10.0);
        MIKAEL_MIN_RUNG = b.comment("The lowest rung of the vampire ladder (0 to 6) at which Mikael starts to hunt a vampire player.")
                .defineInRange("mikael_min_rung", 3, 0, 6);
        MIKAEL_NIGHT_CHANCE = b.comment("The chance, once each night, that Mikael comes for a vampire player of that rung.")
                .defineInRange("mikael_night_chance", 0.15, 0.0, 1.0);
        MIKAEL_STAKES_ORIGINALS = b.comment("Off (the default): Mikael does not hunt Originals or Original hybrids, and his white oak never counts as a white oak kill, so he cannot cause line death. On: he hunts them too, his stake kills an Original outright (R32) and the line dies with them. The hunted player is warned.")
                .define("mikael_stakes_originals", false);
        KLAUS_CHANCE_PER_FULL_MOON = b.comment("The chance, once each full moon night, that Klaus comes for a doppelganger (needs Werewolves). A villager doppelganger must be within 64 blocks of a player.")
                .defineInRange("klaus_chance_per_full_moon", 0.35, 0.0, 1.0);
        KLAUS_HUNTS_PLAYERS = b.comment("Whether Klaus hunts players who have the doppelganger gene, not only villagers. A player is warned first.")
                .define("klaus_hunts_players", true);
        KLAUS_WARNING_SECONDS = b.comment("How long before he arrives a hunted player is warned (a villager is never warned). 0 = he arrives at once.")
                .defineInRange("klaus_warning_seconds", 90, 0, 600);
        ELIJAH_CHANCE_PER_DAY = b.comment("The chance, once each in-game day, that Elijah comes to a player who has discovered an Original or holds an Original's blood (by day or by night).")
                .defineInRange("elijah_chance_per_day", 0.05, 0.0, 1.0);
        ELIJAH_OATH_DAYS = b.comment("In-game days the oath of Elijah's deal lasts: not to harm or desiccate any NPC Original but Mikael, and not to attack Elijah. Breaking it makes him hostile for good.")
                .defineInRange("elijah_oath_days", 7, 1, 100);
        ELIXIR_HEAL_FRACTION = b.comment("The part of their maximum health the Elixir gives back to the one it saves.")
                .defineInRange("elixir_heal_fraction", 0.5, 0.05, 1.0);
        REBEKAH_DELAY_SECONDS = b.comment("How long after The Cure is made at the rite Rebekah arrives for whoever made it. 0 = at once.")
                .defineInRange("rebekah_delay_seconds", 60, 0, 3600);
        KOL_MIN_MARK = b.comment("How many prey kills (the mark) a Hunter of the Five needs before Kol comes for them, by night.")
                .defineInRange("kol_min_mark", 12, 1, 20);
        KOL_CLOAK_MINUTES = b.comment("Minutes the Rite of Cloaking hides its performer from the Vampire Compass and from a hunter's sense.")
                .defineInRange("kol_cloak_minutes", 30, 1, 1440);
        KOL_GRIMOIRE_SP = b.comment("Skill points (the one shared pool) Kol's Grimoire gives when it is read, once per grimoire.")
                .defineInRange("kol_grimoire_sp", 5, 0, 100);
        FINN_CHANCE_PER_ECLIPSE = b.comment("The chance, once on each lunar eclipse night, that Finn comes to a witch-type or vampire player and begins his ritual.")
                .defineInRange("finn_chance_per_eclipse", 0.5, 0.0, 1.0);
        FINN_RITUAL_SECONDS = b.comment("Seconds Finn's ritual takes. Left alone it hurts nobody: he withdraws and the people near are told.")
                .defineInRange("finn_ritual_seconds", 120, 5, 3600);
        LINK_SECONDS = b.comment("Seconds a Linking Spell holds two creatures together.")
                .defineInRange("link_seconds", 60, 5, 3600);
        LINK_DAMAGE_SHARE = b.comment("The part of the damage one linked creature takes that the other takes too. Never passed on again.")
                .defineInRange("link_damage_share", 0.5, 0.0, 1.0);
        DEUCALION_MIN_SPARKS = b.comment("How many stacked sparks a player Alpha needs before Deucalion comes for them, on a full moon night (needs Werewolves).")
                .defineInRange("deucalion_min_sparks", 3, 1, 10);
        DEUCALION_SPARKS = b.comment("The sparks Deucalion's killer takes, by the rules of a stolen spark: nothing for a True Alpha or somebody who is no werewolf, destroyed for an open protected path, never above max_stacked_sparks.")
                .defineInRange("deucalion_sparks", 4, 1, 10);
        DEUCALION_PACK_ALPHAS = b.comment("How many Alphas of his own pack come with him. They give no weak spark and no drops, and go when he does.")
                .defineInRange("deucalion_pack_alphas", 3, 0, 8);
        DEUCALION_RESPAWN = b.comment("Whether Deucalion may come again after he died, and pay his reward again, once deucalion_respawn_days in-game days have passed. Off: he dies for good.")
                .define("deucalion_respawn", false);
        DEUCALION_RESPAWN_DAYS = b.comment("In-game days after his death before he may come again (only with deucalion_respawn on).")
                .defineInRange("deucalion_respawn_days", 7, 1, 1000);
        PETER_HOUSE_ENABLED = b.comment("Whether new chunks may generate the burned Hale house (Peter Hale waits in one of them). Needs Werewolves. Houses already generated stay.")
                .define("peter_house_enabled", true);
        PETER_HOUSE_CHANCE = b.comment("The chance that a house generates where the world has room for one (the places are far apart already, and only in forests and taigas). 0 to 1.")
                .defineInRange("peter_house_chance", 0.5, 0.0, 1.0);
        PETER_FERAL_FULL_MOON = b.comment("Peter shifts on a full moon night, and as the wolf he attacks anything alive on one (not other bosses, not players who cannot be hurt). Off: he shifts only at half health and fights only who hurts him or comes within 10 blocks.")
                .define("peter_feral_full_moon", true);
        PETER_RESPAWN = b.comment("Whether Peter may be found again after he died, and pay his reward again, once peter_respawn_days in-game days have passed. Off: he dies for good.")
                .define("peter_respawn", false);
        PETER_RESPAWN_DAYS = b.comment("In-game days after his death before he may be found again (only with peter_respawn on).")
                .defineInRange("peter_respawn_days", 7, 1, 1000);
        SPEC = b.build();
    }

    private BossesConfig() {
    }

    private static <T> T read(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    private static final java.util.Map<String, Object> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

    /** For tests, which cannot write the config: pins a key ({@code "original_return_days"}, {@code "boss_reward_on"}, {@code "mikael_enabled"} and so on); null clears it. */
    public static void override(String key, Object value) {
        if (value == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T pick(String key, ModConfigSpec.ConfigValue<T> v) {
        Object pinned = OVERRIDES.get(key);
        return pinned != null ? (T) pinned : read(v);
    }

    public static List<String> rewardChoices() {
        return List.of(REWARD_FIRST_DEFEAT, REWARD_DEATH);
    }

    public static int checkSeconds() {
        return pick("boss_check_seconds", BOSS_CHECK_SECONDS);
    }

    public static boolean barsEnabled() {
        return pick("boss_bars_enabled", BOSS_BARS_ENABLED);
    }

    public static int originalReturnDays() {
        return pick("original_return_days", ORIGINAL_RETURN_DAYS);
    }

    public static boolean stakeNeedsOpen() {
        return pick("npc_original_stake_needs_open", NPC_ORIGINAL_STAKE_NEEDS_OPEN);
    }

    public static RewardRules.When rewardOn() {
        return RewardRules.parse(pick("boss_reward_on", BOSS_REWARD_ON));
    }

    public static boolean enabled(BossId boss) {
        return pick(boss.id() + "_enabled", ENABLED.get(boss));
    }

    public static double healthFactor() {
        return pick("boss_health_factor", BOSS_HEALTH_FACTOR);
    }

    public static double damageFactor() {
        return pick("boss_damage_factor", BOSS_DAMAGE_FACTOR);
    }

    public static int mikaelMinRung() {
        return pick("mikael_min_rung", MIKAEL_MIN_RUNG);
    }

    public static boolean mikaelStakesOriginals() {
        return pick("mikael_stakes_originals", MIKAEL_STAKES_ORIGINALS);
    }

    public static double klausChance() {
        return pick("klaus_chance_per_full_moon", KLAUS_CHANCE_PER_FULL_MOON);
    }

    public static boolean klausHuntsPlayers() {
        return pick("klaus_hunts_players", KLAUS_HUNTS_PLAYERS);
    }

    public static int klausWarningSeconds() {
        return pick("klaus_warning_seconds", KLAUS_WARNING_SECONDS);
    }

    public static double elijahChance() {
        return pick("elijah_chance_per_day", ELIJAH_CHANCE_PER_DAY);
    }

    public static int elijahOathDays() {
        return pick("elijah_oath_days", ELIJAH_OATH_DAYS);
    }

    public static double elixirHealFraction() {
        return pick("elixir_heal_fraction", ELIXIR_HEAL_FRACTION);
    }

    public static int kolMinMark() {
        return pick("kol_min_mark", KOL_MIN_MARK);
    }

    public static int kolCloakMinutes() {
        return pick("kol_cloak_minutes", KOL_CLOAK_MINUTES);
    }

    public static int kolGrimoireSp() {
        return pick("kol_grimoire_sp", KOL_GRIMOIRE_SP);
    }

    public static double finnChance() {
        return pick("finn_chance_per_eclipse", FINN_CHANCE_PER_ECLIPSE);
    }

    public static int finnRitualSeconds() {
        return pick("finn_ritual_seconds", FINN_RITUAL_SECONDS);
    }

    public static int linkSeconds() {
        return pick("link_seconds", LINK_SECONDS);
    }

    public static int deucalionMinSparks() {
        return pick("deucalion_min_sparks", DEUCALION_MIN_SPARKS);
    }

    public static int deucalionSparks() {
        return pick("deucalion_sparks", DEUCALION_SPARKS);
    }

    public static int deucalionPackAlphas() {
        return pick("deucalion_pack_alphas", DEUCALION_PACK_ALPHAS);
    }

    public static boolean deucalionRespawn() {
        return pick("deucalion_respawn", DEUCALION_RESPAWN);
    }

    public static int deucalionRespawnDays() {
        return pick("deucalion_respawn_days", DEUCALION_RESPAWN_DAYS);
    }

    public static boolean peterHouseEnabled() {
        return pick("peter_house_enabled", PETER_HOUSE_ENABLED);
    }

    public static double peterHouseChance() {
        return pick("peter_house_chance", PETER_HOUSE_CHANCE);
    }

    public static boolean peterFeral() {
        return pick("peter_feral_full_moon", PETER_FERAL_FULL_MOON);
    }

    public static boolean peterRespawn() {
        return pick("peter_respawn", PETER_RESPAWN);
    }

    public static int peterRespawnDays() {
        return pick("peter_respawn_days", PETER_RESPAWN_DAYS);
    }

    public static double linkDamageShare() {
        return pick("link_damage_share", LINK_DAMAGE_SHARE);
    }

    public static int rebekahDelaySeconds() {
        return pick("rebekah_delay_seconds", REBEKAH_DELAY_SECONDS);
    }

    public static double mikaelNightChance() {
        return pick("mikael_night_chance", MIKAEL_NIGHT_CHANCE);
    }
}
