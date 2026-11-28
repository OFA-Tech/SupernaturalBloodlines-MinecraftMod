package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Plan 23 Task 10: config switches for race mapping from Tensura reincarnation picks to supernatural kinds.
 * Each mapping can be turned off independently. Level floors for Bloodfiends follow the existing
 * VampireSyncConfig.race_level_floor pattern when bloodfiend_to_vampire is on.
 */
public final class RaceMapperConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-race_mapper.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue BLOODFIEND_TO_VAMPIRE;
    private static final ModConfigSpec.BooleanValue DIREWOLF_TO_WEREWOLF;
    private static final ModConfigSpec.BooleanValue SPIRIT_WOLF_TO_WEREWOLF;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOODFIEND_LEVEL_FLOORS;

    private static volatile Boolean bloodfiendOverride;
    private static volatile Boolean direwolfOverride;
    private static volatile Boolean spiritWolfOverride;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Race mapping: Tensura reincarnation picks -> supernatural factions.",
                "Only a player's own pick (reincarnation menu open) triggers mapping; admin commands and mod writes never do.");
        BLOODFIEND_TO_VAMPIRE = b.comment("Ascension Bloodfiend races join the Vampirism vampire faction at level floors (see bloodfiend_level_floors).",
                        "Off: Bloodfiend picks keep their Tensura race with no faction change.")
                .define("bloodfiend_to_vampire", true);
        DIREWOLF_TO_WEREWOLF = b.comment("Mysticism direwolf joins the Werewolves werewolf faction as an Omega (level 1, no rank).",
                        "Off: direwolf picks keep their Tensura race with no faction change.")
                .define("direwolf_to_werewolf", true);
        SPIRIT_WOLF_TO_WEREWOLF = b.comment("Mysticism spirit wolf races join the Werewolves werewolf faction as an Omega (level 1, no rank).",
                        "Off: spirit wolf picks keep their Tensura race with no faction change.")
                .define("spirit_wolf_to_werewolf", true);
        BLOODFIEND_LEVEL_FLOORS = b.comment("\"race=level\": a Bloodfiend race raises the Vampirism level to at least level when picked.",
                        "Defaults: fledgling=1, kindred=4, noble=7, elder=10, progenitor=max (-1).")
                .defineListAllowEmpty("bloodfiend_level_floors", List.of(
                        "ascension:fledgling_bloodfiend=1",
                        "ascension:kindred_bloodfiend=4",
                        "ascension:blood_noble=7",
                        "ascension:elder_bloodfiend=10",
                        "ascension:progenitor_bloodfiend=-1"), () -> "", o -> o instanceof String);
        SPEC = b.build();
    }

    private RaceMapperConfig() {}

    public static boolean bloodfiendToVampire() {
        Boolean forced = bloodfiendOverride;
        return forced != null ? forced : read(BLOODFIEND_TO_VAMPIRE);
    }

    public static boolean direwolfToWerewolf() {
        Boolean forced = direwolfOverride;
        return forced != null ? forced : read(DIREWOLF_TO_WEREWOLF);
    }

    public static boolean spiritWolfToWerewolf() {
        Boolean forced = spiritWolfOverride;
        return forced != null ? forced : read(SPIRIT_WOLF_TO_WEREWOLF);
    }

    public static List<String> bloodfiendLevelFloors() {
        try {
            return BLOODFIEND_LEVEL_FLOORS.get().stream().map(String.class::cast).toList();
        } catch (IllegalStateException notLoaded) {
            return List.of("ascension:fledgling_bloodfiend=1", "ascension:kindred_bloodfiend=4",
                    "ascension:blood_noble=7", "ascension:elder_bloodfiend=10", "ascension:progenitor_bloodfiend=-1");
        }
    }

    private static boolean read(ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** For tests: null goes back to the config. */
    public static void overrideBloodfiendToVampire(Boolean value) {
        bloodfiendOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideDirewolfToWerewolf(Boolean value) {
        direwolfOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideSpiritWolfToWerewolf(Boolean value) {
        spiritWolfOverride = value;
    }
}