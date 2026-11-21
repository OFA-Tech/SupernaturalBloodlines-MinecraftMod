package net.ofatech.supernaturalbloodlines.core.blood;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.jetbrains.annotations.Nullable;

/** What a bottle of vampire blood was taken from. Only {@link #COMMON_VAMPIRE} may be named; every other kind is a secret. */
public enum BloodKind {
    COMMON_VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, TRIBRID, HERETIC;

    /** Every species that is not one of the five kinds below (none, a witch, an Augustine...) gives common vampire blood. */
    public static BloodKind of(Species s) {
        return switch (s) {
            case ORIGINAL -> ORIGINAL;
            case HYBRID -> HYBRID;
            case ORIGINAL_HYBRID -> ORIGINAL_HYBRID;
            case TRIBRID -> TRIBRID;
            case HERETIC -> HERETIC;
            default -> COMMON_VAMPIRE;
        };
    }

    /** From the {@code Species} name a bottle stores; a bottle that stored none, or a name nobody knows, counts as common vampire blood. */
    public static BloodKind parse(@Nullable String speciesName) {
        if (speciesName == null || speciesName.isEmpty()) {
            return COMMON_VAMPIRE;
        }
        try {
            return of(Species.valueOf(speciesName));
        } catch (IllegalArgumentException unknown) {
            return COMMON_VAMPIRE;
        }
    }

    /** The {@code Species} name stored on a bottle: {@code NONE} for a common vampire. */
    public String speciesName() {
        return this == COMMON_VAMPIRE ? Species.NONE.name() : name();
    }

    /** Only an Original's blood names the drinker's pending sire. */
    public boolean namesSire() {
        return this == ORIGINAL || this == ORIGINAL_HYBRID;
    }

    /** Tooltips and synced data must not show it. */
    public boolean secret() {
        return this != COMMON_VAMPIRE;
    }
}
