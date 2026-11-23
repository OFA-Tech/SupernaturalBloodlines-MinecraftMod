package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;

/** The evolution rites' own settings (plan 19, spec section 11): every key has an Admin row. */
public final class EvolutionRitesConfig {
    public static final String FILE = AdminEntries.EVOLUTION_RITES_FILE;
    public static final ModConfigSpec SPEC;

    public static final List<String> DEFAULT_REFUSED = List.of("ascension:the_system", "ascension:shadow_necromancer", "ascension:shadow_monarch");

    private static final ModConfigSpec.BooleanValue RITES_ENABLED;
    private static final ModConfigSpec.BooleanValue ULTIMATE_RITES_ENABLED;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_RECIPES;
    private static final ModConfigSpec.BooleanValue ANNOUNCE_ULTIMATES;
    private static final ModConfigSpec.BooleanValue MASTERY_ENABLED;
    private static final ModConfigSpec.DoubleValue MASTERY_EP_COMMON;
    private static final ModConfigSpec.DoubleValue MASTERY_EP_EXTRA;
    private static final ModConfigSpec.DoubleValue MASTERY_EP_UNIQUE;
    private static final ModConfigSpec.DoubleValue MASTERY_EP_ULTIMATE;
    private static final ModConfigSpec.IntValue MASTERY_COOLDOWN_DAYS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MASTERY_REFUSED_SKILLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RITES_ENABLED = b.comment("Whether the evolution rites (a witch-type performer opens a road to a skill that already exists) work at all. Off: they refuse, saying they are closed.")
                .define("rites_enabled", true);
        ULTIMATE_RITES_ENABLED = b.comment("Whether the rites of the lunar eclipse (the ones that grant an Ultimate skill) work.")
                .define("ultimate_rites_enabled", true);
        DISABLED_RECIPES = b.comment("Recipe ids that are switched off (for example great_sage), so a server can take one skill out of the rites. They refuse, saying the rite is closed.")
                .defineListAllowEmpty("disabled_recipes", List.of(), () -> "", o -> o instanceof String);
        ANNOUNCE_ULTIMATES = b.comment("Whether the server says, once, that someone has been given an Ultimate skill by a rite. It names the player, so it is off by default.")
                .define("announce_ultimates", false);
        MASTERY_ENABLED = b.comment("Whether the Rite of Mastery (brings one chosen skill the target holds to mastery) works.").define("mastery_enabled", true);
        MASTERY_EP_COMMON = b.comment("Base Max EP the Rite of Mastery spends to master a Common skill (a resistance, an intrinsic or a common skill).")
                .defineInRange("mastery_ep_common", 100_000.0, 0.0, 1.0e12);
        MASTERY_EP_EXTRA = b.comment("Base Max EP the Rite of Mastery spends to master an Extra skill.").defineInRange("mastery_ep_extra", 300_000.0, 0.0, 1.0e12);
        MASTERY_EP_UNIQUE = b.comment("Base Max EP the Rite of Mastery spends to master a Unique skill.").defineInRange("mastery_ep_unique", 750_000.0, 0.0, 1.0e12);
        MASTERY_EP_ULTIMATE = b.comment("Base Max EP the Rite of Mastery spends to master an Ultimate skill.").defineInRange("mastery_ep_ultimate", 2_000_000.0, 0.0, 1.0e12);
        MASTERY_COOLDOWN_DAYS = b.comment("In-game days before the same player may have another Rite of Mastery worked on them.").defineInRange("mastery_cooldown_days", 7, 0, 1000);
        MASTERY_REFUSED_SKILLS = b.comment("Skills the Rite of Mastery refuses. The default is Ascension's System skills, which the addon rewrites from the player's System level.")
                .defineListAllowEmpty("mastery_refused_skills", DEFAULT_REFUSED, () -> "", o -> o instanceof String);
        SPEC = b.build();
    }

    private static final Map<String, Object> OVERRIDES = new ConcurrentHashMap<>();

    private EvolutionRitesConfig() {
    }

    /** For tests, which cannot write the config: pins a key; null clears it. */
    public static void override(String key, Object value) {
        if (value == null) {
            OVERRIDES.remove(key);
        } else {
            OVERRIDES.put(key, value);
        }
    }

    private static <T> T read(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T pick(String key, ModConfigSpec.ConfigValue<T> v) {
        Object pinned = OVERRIDES.get(key);
        return pinned != null ? (T) pinned : read(v);
    }

    private static Set<String> set(String key, ModConfigSpec.ConfigValue<List<? extends String>> v) {
        return pick(key, v).stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    public static boolean ritesEnabled() {
        return pick("rites_enabled", RITES_ENABLED);
    }

    public static boolean ultimateRitesEnabled() {
        return pick("ultimate_rites_enabled", ULTIMATE_RITES_ENABLED);
    }

    public static Set<String> disabledRecipes() {
        return set("disabled_recipes", DISABLED_RECIPES);
    }

    public static boolean announceUltimates() {
        return pick("announce_ultimates", ANNOUNCE_ULTIMATES);
    }

    public static boolean masteryEnabled() {
        return pick("mastery_enabled", MASTERY_ENABLED);
    }

    public static double masteryEp(Tier tier) {
        return switch (tier) {
            case COMMON -> pick("mastery_ep_common", MASTERY_EP_COMMON);
            case EXTRA -> pick("mastery_ep_extra", MASTERY_EP_EXTRA);
            case UNIQUE -> pick("mastery_ep_unique", MASTERY_EP_UNIQUE);
            case ULTIMATE -> pick("mastery_ep_ultimate", MASTERY_EP_ULTIMATE);
        };
    }

    public static int masteryCooldownDays() {
        return pick("mastery_cooldown_days", MASTERY_COOLDOWN_DAYS);
    }

    public static Set<String> masteryRefusedSkills() {
        return set("mastery_refused_skills", MASTERY_REFUSED_SKILLS);
    }

    /** Whether this recipe may run now: the part's switches, not the addon's. */
    public static boolean recipeOpen(String recipeId, Tier tier) {
        return ritesEnabled() && !disabledRecipes().contains(recipeId) && (tier != Tier.ULTIMATE || ultimateRitesEnabled());
    }
}
