package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * The part's own config file. Values are readable once the config has loaded; before that the defaults count.
 * It is a COMMON config: Tensura's undead checks also run on the client (the player's own screens and effects), so the
 * server's and the clients' "undead_treatment_enabled" and "layers" should match.
 */
public final class SpeciesTensuraConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-species_tensura.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> LAYERS;
    private static final ModConfigSpec.BooleanValue UNDEAD_TREATMENT;
    private static final ModConfigSpec.BooleanValue APEX_ENABLED;
    private static final ModConfigSpec.DoubleValue EVOLUTION_MULTIPLIER;
    private static final Set<String> WARNED = new HashSet<>();

    private static volatile Boolean undeadOverride;
    private static volatile Boolean apexOverride;
    private static volatile List<? extends String> parsedFrom;
    private static volatile LayerTable parsed = LayerTable.parse(LayerTable.DEFAULTS).table();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Tensura layer of each species. The race is never replaced (R100).");
        LAYERS = b.comment("\"SPECIES=skill,skill|maxMagiculeBonus|maxAuraBonus|treatAsUndead\" (bonuses are fractions, 0.2 = +20%).",
                        "SPECIES is a species name or VAMPIRE (a Vampirism vampire with no species). A skill without a namespace is a Tensura one.",
                        "An entry replaces the default of the same key; a bad entry is ignored with a warning.")
                .defineListAllowEmpty("layers", LayerTable.DEFAULTS, () -> "", o -> o instanceof String);
        UNDEAD_TREATMENT = b.comment("Tensura treats a player whose layer says undead as undead. That is wider than holy damage: Tensura's own undead rules apply,",
                        "such as holy and light damage x3, holy exposure, infection immunity and Enervation on food. A plain Vampirism vampire (VAMPIRE) counts too,",
                        "so it goes from x1.5 to Tensura's x3 holy damage and tensura_holy_vs_vampire (vampirism_tensura config) has no effect on it.",
                        "Common config: keep it equal on the server and the clients.",
                        "Off: the layers still grant skills and bonuses, but Tensura's undead checks are left alone.")
                .define("undead_treatment_enabled", true);
        APEX_ENABLED = b.comment("Offer the apex race of the player's species in Tensura's evolution screen (R100). Off: no apex option; species keep working.",
                        "Common config: the evolution screen is built on the client too, so keep it equal on the server and the clients.")
                .define("apex_evolutions_enabled", true);
        EVOLUTION_MULTIPLIER = b.comment("A player with a species gets this multiple of the progress Tensura counts toward any evolution (capped at 100%).")
                .defineInRange("evolution_progress_multiplier", 1.25, 1.0, 10.0);
        SPEC = b.build();
    }

    private SpeciesTensuraConfig() {}

    public static LayerTable table() {
        List<? extends String> entries;
        try {
            entries = LAYERS.get();
        } catch (IllegalStateException notLoaded) {
            return parsed;
        }
        // get() hands back the same list until the config reloads, so the parse is redone only then
        if (entries != parsedFrom) {
            LayerTable.Parsed p = LayerTable.parse(entries.stream().map(String.class::cast).toList());
            for (String bad : p.rejected()) {
                if (WARNED.add(bad)) {
                    SupernaturalBloodlines.LOGGER.warn("{}: ignoring invalid layers entry '{}' (want SPECIES=skill,skill|magicule|aura|undead)", FILE, bad);
                }
            }
            parsed = p.table();
            parsedFrom = entries;
        }
        return parsed;
    }

    /** For tests: null goes back to the config. */
    public static void overrideUndeadTreatment(Boolean value) {
        undeadOverride = value;
    }

    /** For tests: simulates the apex hook being off. Null goes back to the config. */
    public static void overrideApexEnabled(Boolean value) {
        apexOverride = value;
    }

    public static boolean apexEnabled() {
        Boolean forced = apexOverride;
        if (forced != null) {
            return forced;
        }
        try {
            return APEX_ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static double evolutionMultiplier() {
        try {
            return EVOLUTION_MULTIPLIER.get();
        } catch (IllegalStateException notLoaded) {
            return 1.25;
        }
    }

    public static boolean undeadTreatment() {
        Boolean forced = undeadOverride;
        if (forced != null) {
            return forced;
        }
        try {
            return UNDEAD_TREATMENT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
