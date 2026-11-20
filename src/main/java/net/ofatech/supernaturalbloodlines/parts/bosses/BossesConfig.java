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

    public static double mikaelNightChance() {
        return pick("mikael_night_chance", MIKAEL_NIGHT_CHANCE);
    }
}
