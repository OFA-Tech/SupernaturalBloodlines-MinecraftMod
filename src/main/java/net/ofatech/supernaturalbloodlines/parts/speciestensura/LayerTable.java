package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * The layer of each species, from the config list {@code layers}. An entry is
 * {@code KEY=skill,skill|maxMagiculeBonus|maxAuraBonus|treatAsUndead}; KEY is a species name or VAMPIRE (a plain
 * Vampirism vampire without a species, R20). A skill without a namespace is a Tensura one. No game classes.
 */
public final class LayerTable {
    public static final String VAMPIRE = "VAMPIRE";

    public static final List<String> DEFAULTS = List.of(
            "ORIGINAL=blood_mist,drain,charm,self_regeneration|0.20|0|true",
            "ORIGINAL_HYBRID=blood_mist,drain,charm,self_regeneration,beast_transformation,godwolf_sense|0.25|0|true",
            "HYBRID=drain,beast_transformation,self_regeneration|0.10|0|true",
            "WITCH=|0.30|0.10|false",
            "SIPHONER=drain|0.10|0|false",
            "HERETIC=drain,charm,self_regeneration|0.20|0|true",
            "TRIBRID=drain,beast_transformation,self_regeneration,charm|0.30|0|true",
            "HUNTER_OF_THE_FIVE=|0|0.10|false",
            VAMPIRE + "=drain|0|0|true");

    /** The usable table plus the config entries that could not be used. */
    public record Parsed(LayerTable table, List<String> rejected) {}

    private static final Pattern SKILL_ID = Pattern.compile("[a-z0-9_.-]+(:[a-z0-9_./-]+)?");

    private final Map<String, LayerSpec> specs;

    private LayerTable(Map<String, LayerSpec> specs) {
        this.specs = specs;
    }

    /** A species wins over the vampire faction; the VAMPIRE entry is only for a player with no species. */
    public LayerSpec specFor(Species species, boolean plainVampire) {
        if (species == Species.NONE) {
            return plainVampire ? specs.getOrDefault(VAMPIRE, LayerSpec.NONE) : LayerSpec.NONE;
        }
        return specs.getOrDefault(species.name(), LayerSpec.NONE);
    }

    /** The defaults, with every valid config entry replacing the one of the same key. */
    public static Parsed parse(List<String> entries) {
        Map<String, LayerSpec> specs = new HashMap<>(parseAll(DEFAULTS, new ArrayList<>()));
        List<String> rejected = new ArrayList<>();
        specs.putAll(parseAll(entries, rejected));
        return new Parsed(new LayerTable(Map.copyOf(specs)), List.copyOf(rejected));
    }

    private static Map<String, LayerSpec> parseAll(List<String> entries, List<String> rejected) {
        Map<String, LayerSpec> out = new HashMap<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            String key = eq > 0 ? entry.substring(0, eq).trim().toUpperCase(Locale.ROOT) : "";
            LayerSpec spec = knownKey(key) ? parseSpec(entry.substring(eq + 1)) : null;
            if (spec == null) {
                rejected.add(entry);
            } else {
                out.put(key, spec);
            }
        }
        return out;
    }

    private static boolean knownKey(String key) {
        if (key.equals(VAMPIRE)) {
            return true;
        }
        try {
            Species s = Species.valueOf(key);
            return s != Species.NONE && s != Species.POTENTIAL;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static LayerSpec parseSpec(String body) {
        String[] parts = body.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        List<String> skills = new ArrayList<>();
        for (String raw : parts[0].split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            if (!SKILL_ID.matcher(id).matches()) {
                return null;
            }
            String full = id.contains(":") ? id : "tensura:" + id;
            if (!skills.contains(full)) {
                skills.add(full);
            }
        }
        String undead = parts[3].trim().toLowerCase(Locale.ROOT);
        try {
            double magicule = Double.parseDouble(parts[1].trim());
            double aura = Double.parseDouble(parts[2].trim());
            boolean valid = magicule >= 0 && aura >= 0 && Double.isFinite(magicule) && Double.isFinite(aura)
                    && (undead.equals("true") || undead.equals("false"));
            return valid ? new LayerSpec(skills, magicule, aura, undead.equals("true")) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
