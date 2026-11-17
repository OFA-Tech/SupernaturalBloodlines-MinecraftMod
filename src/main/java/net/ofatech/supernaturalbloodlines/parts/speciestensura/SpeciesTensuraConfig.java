package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RANK_SKILLS;
    private static final ModConfigSpec.BooleanValue UNDEAD_TREATMENT;
    private static final ModConfigSpec.BooleanValue APEX_ENABLED;
    private static final ModConfigSpec.DoubleValue EVOLUTION_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue SIRE_LINK;
    private static final ModConfigSpec.BooleanValue SIRE_NAMING;
    private static final ModConfigSpec.BooleanValue SEVERANCE_CLEARS_NAME;
    private static final ModConfigSpec.BooleanValue RESIRE_NAMING_REWARD;

    private static volatile Boolean severanceClearsNameOverride;
    private static volatile Boolean resireNamingRewardOverride;
    private static volatile Boolean undeadOverride;
    private static volatile Boolean apexOverride;
    private static volatile Boolean sireLinkOverride;
    private static volatile Boolean sireNamingOverride;

    /** A string-list option parsed into a table; parsed again only when the config hands out a new list (a reload). */
    private static final class Cached<T> {
        private final String key;
        private final String want;
        private final Supplier<List<? extends String>> raw;
        private final Function<List<String>, ParsedTable<T>> parser;
        private volatile List<? extends String> from;
        private volatile T table;
        private volatile List<String> rejected = List.of();

        Cached(String key, String want, Supplier<List<? extends String>> raw, Function<List<String>, ParsedTable<T>> parser) {
            this.key = key;
            this.want = want;
            this.raw = raw;
            this.parser = parser;
            this.table = parser.apply(List.of()).table();
        }

        T get() {
            List<? extends String> entries;
            try {
                entries = raw.get();
            } catch (IllegalStateException notLoaded) {
                return table;
            }
            if (entries != from) {
                ParsedTable<T> p = parser.apply(entries.stream().map(String.class::cast).toList());
                for (String bad : p.rejected()) {
                    SupernaturalBloodlines.LOGGER.error("{}: ignoring invalid {} entry '{}' (want {})", FILE, key, bad, want);
                }
                table = p.table();
                rejected = p.rejected();
                from = entries;
            }
            return table;
        }

        List<String> rejected() {
            get();
            return rejected;
        }
    }

    private static final Cached<LayerTable> LAYER_TABLE;
    private static final Cached<RankSkillTable> RANK_TABLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The Tensura layer of each species. The race is never replaced (R100).");
        LAYERS = b.comment("\"SPECIES=skill,skill|maxMagiculeBonus|maxAuraBonus|treatAsUndead\" (bonuses are fractions, 0.2 = +20%).",
                        "SPECIES is a species name or VAMPIRE (a Vampirism vampire with no species). A skill without a namespace is a Tensura one.",
                        "An entry replaces the default of the same key; a bad entry is ignored: it is logged at ERROR when the config loads and ops are told in chat on login.")
                .defineListAllowEmpty("layers", LayerTable.DEFAULTS, () -> "", o -> o instanceof String);
        RANK_SKILLS = b.comment("Tensura skills of a werewolf rank (Werewolves only): \"RANK=skill,skill\" with RANK ALPHA, TRUE_ALPHA or BORN_LEVEL5",
                        "(a born werewolf of level 5). A skill without a namespace is a Tensura one. The race never changes; only skills this mod granted are taken back.",
                        "An entry replaces the default of the same key; a bad entry is ignored: it is logged at ERROR when the config loads and ops are told in chat on login.")
                .defineListAllowEmpty("rank_skills", RankSkillTable.DEFAULTS, () -> "", o -> o instanceof String);
        UNDEAD_TREATMENT = b.comment("Tensura treats a player whose layer says undead as undead. That is wider than holy damage: Tensura's own undead rules apply,",
                        "such as holy and light damage x3, holy exposure, infection immunity and Enervation on food. A plain Vampirism vampire (VAMPIRE) counts too,",
                        "so it goes from x1.5 to Tensura's x3 holy damage and tensura_holy_vs_vampire (vampirism_tensura config) has no effect on it.",
                        "Common config: keep it equal on the server and the clients.",
                        "Off: the layers still grant skills and bonuses, but Tensura's undead checks are left alone.")
                .define("undead_treatment_enabled", true);
        APEX_ENABLED = b.comment("Offer the apex race of the player's species in Tensura's evolution screen (R100). Off: no apex option; species keep working.",
                        "Common config: the evolution screen is built on the client too, so keep it equal on the server and the clients.")
                .define("apex_evolutions_enabled", true);
        EVOLUTION_MULTIPLIER = b.comment("A player with a species gets this multiple of the EP Tensura credits them (kills, and everything else that raises max aura and magicule). 1.0 turns the bonus off.",
                        "It speeds up EP gain only: the evolution requirements and their progress are Tensura's own. The old name says 'progress' because it used to scale the progress; the key is kept so existing configs keep their value.",
                        "Applied as a modifier on Tensura's aura and magicule gain attributes, refreshed within 10 seconds of a change.")
                .defineInRange("evolution_progress_multiplier", 1.25, 1.0, 10.0);
        SIRE_LINK = b.comment("A sired creature or player becomes the sire's Tensura subordinate (its permanent owner is the sire). R101.",
                        "Tensura then treats the pair as owner and subordinate: the sired cannot hurt the sire or the sire's other sired (the sire can still hurt them),",
                        "they count as allied, and a demon lord sire's harvest festival gifts them. Off: no link is made or removed.")
                .define("sire_subordinate_link_enabled", true);
        SIRE_NAMING = b.comment("Siring names the sired player in Tensura, automatically (R101). The name is the sired's own current name, so it only",
                        "makes the two mods compatible. It needs no consent and Tensura's playerNaming game rule and the sired's nameable toggle do not block it.",
                        "The sire pays Tensura's naming cost and the sired gets Tensura's EP and stat reward, as for a real LOW naming: never a race evolution.",
                        "It is tried when the link is made and when the sired or the sire logs in, and needs the sire online with enough magicule.",
                        "It needs sire_subordinate_link_enabled. Off: siring names nobody.")
                .define("sire_naming_enabled", true);
        SEVERANCE_CLEARS_NAME = b.comment("The Rite of Severance (R101b) also clears the sired player's Tensura name when the severed sire is their Tensura owner,",
                        "so someone else can name them later. The EP and stats the naming gave are kept. An owner that is not the severed sire is left alone, and so is the name.",
                        "Off: the severance frees the owner and the name stays.")
                .define("severance_clears_name", true);
        RESIRE_NAMING_REWARD = b.comment("A sire naming a player whose name a severance cleared pays Tensura's EP and stat reward again, as a first naming does.",
                        "Off: that re-naming still names the player and the sire still pays Tensura's cost, but the sired gets no EP and stat reward a second time.",
                        "Only a sire's naming of their bound sired is affected; a naming by anyone else is Tensura's own.")
                .define("resire_naming_reward", true);
        SPEC = b.build();
        LAYER_TABLE = new Cached<>("layers", "SPECIES=skill,skill|magicule|aura|undead", LAYERS::get, LayerTable::parse);
        RANK_TABLE = new Cached<>("rank_skills", "ALPHA|TRUE_ALPHA|BORN_LEVEL5=skill,skill", RANK_SKILLS::get, RankSkillTable::parse);
    }

    private SpeciesTensuraConfig() {}

    public static LayerTable table() {
        return LAYER_TABLE.get();
    }

    public static RankSkillTable rankSkills() {
        return RANK_TABLE.get();
    }

    /** The config entries (layers, rank_skills) that could not be used. Parsing them also logs each one at ERROR, so call it when the config loads. */
    public static List<String> rejectedEntries() {
        List<String> all = new ArrayList<>(LAYER_TABLE.rejected());
        all.addAll(RANK_TABLE.rejected());
        return all;
    }

    /** For tests: null goes back to the config. */
    public static void overrideUndeadTreatment(Boolean value) {
        undeadOverride = value;
    }

    /** For tests: simulates the apex hook being off. Null goes back to the config. */
    public static void overrideApexEnabled(Boolean value) {
        apexOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideSireLink(Boolean value) {
        sireLinkOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideSireNaming(Boolean value) {
        sireNamingOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideSeveranceClearsName(Boolean value) {
        severanceClearsNameOverride = value;
    }

    /** For tests: null goes back to the config. */
    public static void overrideResireNamingReward(Boolean value) {
        resireNamingRewardOverride = value;
    }

    public static boolean severanceClearsName() {
        Boolean forced = severanceClearsNameOverride;
        return forced != null ? forced : read(SEVERANCE_CLEARS_NAME);
    }

    public static boolean resireNamingReward() {
        Boolean forced = resireNamingRewardOverride;
        return forced != null ? forced : read(RESIRE_NAMING_REWARD);
    }

    public static boolean sireLink() {
        Boolean forced = sireLinkOverride;
        return forced != null ? forced : read(SIRE_LINK);
    }

    public static boolean sireNaming() {
        Boolean forced = sireNamingOverride;
        return forced != null ? forced : read(SIRE_NAMING);
    }

    private static boolean read(ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
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
