package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. Values are readable once the config has loaded. */
public final class VampireSyncConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-vampirism_tensura.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> CURE_RACE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RACE_LEVEL_FLOOR;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> WEREWOLF_SKILL_GRANTS;
    private static final ModConfigSpec.DoubleValue TENSURA_HOLY_VS_VAMPIRE;
    public static final double DEFAULT_HOLY_VS_VAMPIRE = 1.5;
    private static final Set<String> WARNED = new HashSet<>();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Tensura <-> Vampirism sync. Joining Vampirism never changes the Tensura race.");
        CURE_RACE = b.comment("Race set when a cured player's current race is in the Tensura vampire line.")
                .define("cure_race", VampireMapping.DEFAULT_CURE_RACE,
                        o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        RACE_LEVEL_FLOOR = b.comment("\"race=level\": a Tensura vampire-line race raises the Vampirism level to at least",
                        "level (capped at the maximum); -1 means the maximum vampire level. A race not listed does not join the faction.")
                .defineListAllowEmpty("race_level_floor", VampireMapping.DEFAULT_FLOORS, () -> "", o -> o instanceof String);
        WEREWOLF_SKILL_GRANTS = b.comment("\"level=skillId\": a werewolf at that Werewolves level or higher is given the Tensura skill, and loses it again",
                        "when the level drops below it or they leave the faction. -1 means the maximum werewolf level.")
                .defineListAllowEmpty("werewolf_skill_grants", SkillGrantSpec.DEFAULTS, () -> "", o -> o instanceof String);
        TENSURA_HOLY_VS_VAMPIRE = b.comment("Tensura holy damage (not Vampirism's holy water) to a Vampirism vampire is multiplied by this.",
                        "No effect while species_tensura makes Tensura treat that vampire as undead (its undead_treatment_enabled, on by default):",
                        "Tensura then applies its own x3 and this multiplier is skipped so the two do not stack.")
                .defineInRange("tensura_holy_vs_vampire", DEFAULT_HOLY_VS_VAMPIRE, 0.0, 100.0);
        SPEC = b.build();
    }

    private VampireSyncConfig() {}

    /** Needs a loaded config; falls back to defaults before that. Each rejected entry is warned about once. */
    public static VampireMapping mapping(int maxLevel) {
        try {
            VampireMapping.Parsed p = VampireMapping.parse(
                    CURE_RACE.get(), RACE_LEVEL_FLOOR.get().stream().map(String.class::cast).toList(), maxLevel);
            for (String bad : p.rejected()) {
                if (WARNED.add(bad)) {
                    SupernaturalBloodlines.LOGGER.warn("{}: ignoring invalid race_level_floor entry '{}' (want race=level, level >= 1 or -1)", FILE, bad);
                }
            }
            return p.mapping();
        } catch (IllegalStateException notLoaded) {
            return VampireMapping.defaults(maxLevel);
        }
    }

    public static List<SkillGrantSpec> werewolfSkillGrants() {
        List<String> entries;
        try {
            entries = WEREWOLF_SKILL_GRANTS.get().stream().map(String.class::cast).toList();
        } catch (IllegalStateException notLoaded) {
            entries = SkillGrantSpec.DEFAULTS;
        }
        SkillGrantSpec.Parsed p = SkillGrantSpec.parse(entries);
        for (String bad : p.rejected()) {
            if (WARNED.add(bad)) {
                SupernaturalBloodlines.LOGGER.warn("{}: ignoring invalid werewolf_skill_grants entry '{}' (want level=namespace:skill, level >= 1 or -1)", FILE, bad);
            }
        }
        return p.grants();
    }

    public static double holyVsVampire() {
        try {
            return TENSURA_HOLY_VS_VAMPIRE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_HOLY_VS_VAMPIRE;
        }
    }
}
