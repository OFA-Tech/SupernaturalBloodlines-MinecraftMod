package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Which hud mixins apply for the installed mods. Pure: the mixin plugin passes in what is loaded. */
public final class HudMixinRules {
    /** Mixin simple name to the mods it needs. */
    public static final Map<String, List<String>> REQUIRED_MODS = Map.of(
            "AbilityStorageNextPresetMixin", List.of("tensura"),
            "VampireRaceFeedMixin", List.of("tensura", "vampirism"));

    /**
     * Mixin simple name to the mods that already do the same job. Tensura Slots injects at RETURN of the same four
     * {@code AbilityStorage.isAbilityInActivePreset} overloads and answers true for a skill in the next preset, so with it installed ours stays out.
     */
    public static final Map<String, List<String>> SUPERSEDED_BY = Map.of(
            "AbilityStorageNextPresetMixin", List.of("tensuraslots"));

    private HudMixinRules() {}

    public static boolean applies(String simpleName, Predicate<String> installed) {
        return REQUIRED_MODS.getOrDefault(simpleName, List.of()).stream().allMatch(installed)
                && SUPERSEDED_BY.getOrDefault(simpleName, List.of()).stream().noneMatch(installed);
    }
}
