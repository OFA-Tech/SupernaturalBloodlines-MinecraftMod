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
            List.of("vampirism")),
    SUPERNATURAL("supernatural", "net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalPart",
            List.of(), List.of("vampirism", "tensura", "werewolves")),
    SUPERNATURAL_TENSURA("supernaturalTensura", "net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.SupernaturalTensuraPart",
            List.of("tensura")),
    HUD("hud", "net.ofatech.supernaturalbloodlines.parts.hud.HudPart", List.of(), List.of("vampirism", "tensura")),
    CELESTIALS("celestials", "net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsPart", List.of(), List.of("enhancedcelestials2core")),
    BOSSES("bosses", "net.ofatech.supernaturalbloodlines.parts.bosses.BossesPart", List.of("vampirism", "enchanted", "geckolib")),
    EVOLUTION_RITES("evolutionRites", "net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesPart", List.of("vampirism", "enchanted", "tensura"));

    private final String configKey;
    private final String initClass;
    private final List<String> requiredMods;
    private final List<String> anyOfMods;

    Part(String configKey, String initClass, List<String> requiredMods) {
        this(configKey, initClass, requiredMods, List.of());
    }

    Part(String configKey, String initClass, List<String> requiredMods, List<String> anyOfMods) {
        this.configKey = configKey;
        this.initClass = initClass;
        this.requiredMods = requiredMods;
        this.anyOfMods = anyOfMods;
    }

    public List<String> requiredMods() { return requiredMods; }
    public List<String> anyOfMods() { return anyOfMods; }
    public String initClass() { return initClass; }
    public String configKey() { return configKey; }

    public Set<Part> requiredParts() {
        return switch (this) {
            case SPECIES -> Set.of();
            case SPECIES_TENSURA -> Set.of(SPECIES);
            case EVOLUTION_RITES -> Set.of(SPECIES);
            case BOSSES -> Set.of(SPECIES);
            case ARTIFACTS -> Set.of();
            case SUPERNATURAL_TENSURA -> Set.of(SUPERNATURAL);
            default -> Set.of();
        };
    }
}