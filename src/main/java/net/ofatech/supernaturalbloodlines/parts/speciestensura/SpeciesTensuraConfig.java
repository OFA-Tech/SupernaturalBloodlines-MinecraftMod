package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The part's own config file. Values are readable once the config has loaded; before that the defaults count. */
public final class SpeciesTensuraConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-species_tensura.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> LAYERS;
    private static final ModConfigSpec.BooleanValue UNDEAD_TREATMENT;
    private static final Set<String> WARNED = new HashSet<>();

    private static volatile Boolean undeadOverride;
    private static volatile List<? extends String> parsedFrom;
    private static volatile LayerTable parsed = LayerTable.parse(LayerTable.DEFAULTS).table();

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Tensura layer of each species. The race is never replaced (R100).");
        LAYERS = b.comment("\"SPECIES=skill,skill|maxMagiculeBonus|maxAuraBonus|treatAsUndead\" (bonuses are fractions, 0.2 = +20%).",
                        "SPECIES is a species name or VAMPIRE (a Vampirism vampire with no species). A skill without a namespace is a Tensura one.",
                        "An entry replaces the default of the same key; a bad entry is ignored with a warning.")
                .defineListAllowEmpty("layers", LayerTable.DEFAULTS, () -> "", o -> o instanceof String);
        UNDEAD_TREATMENT = b.comment("Tensura treats a player whose layer says undead as undead (holy damage x3, holy exposure).",
                        "Off: the layers still grant skills and bonuses, but Tensura's undead checks are left alone.")
                .define("undead_treatment_enabled", true);
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
