package net.ofatech.supernaturalbloodlines.core;

import java.util.List;
import java.util.Set;

public enum Part {
    ENCHANTED_TENSURA("enchantedTensura", "net.ofatech.supernaturalbloodlines.parts.enchantedtensura.EnchantedTensuraPart",
            List.of("enchanted", "tensura")),
    VAMPIRISM_ENCHANTED("vampirismEnchanted", "net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismEnchantedPart",
            List.of("vampirism", "enchanted")),
    VAMPIRISM_TENSURA("vampirismTensura", "net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampirismTensuraPart",
            List.of("vampirism", "tensura")),
    SPECIES("species", "net.ofatech.supernaturalbloodlines.parts.species.SpeciesPart",
            List.of("vampirism", "enchanted")),
    SPECIES_TENSURA("speciesTensura", "net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraPart",
            List.of("vampirism", "enchanted", "tensura")),
    ARTIFACTS("artifacts", "net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsPart",
            List.of("vampirism", "enchanted"));

    private final String configKey;
    private final String initClass;
    private final List<String> requiredMods;

    Part(String configKey, String initClass, List<String> requiredMods) {
        this.configKey = configKey;
        this.initClass = initClass;
        this.requiredMods = requiredMods;
    }

    public List<String> requiredMods() {
        return requiredMods;
    }

    public String initClass() {
        return initClass;
    }

    public String configKey() {
        return configKey;
    }

    public Set<Part> requiredParts() {
        return switch (this) {
            case SPECIES -> Set.of(VAMPIRISM_ENCHANTED); // reuses its FactionRiteFactory
            case SPECIES_TENSURA -> Set.of(SPECIES);
            case ARTIFACTS -> Set.of(VAMPIRISM_ENCHANTED); // reuses its RiteTargets and FactionRiteHooks
            default -> Set.of();
        };
    }
}
