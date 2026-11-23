package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.Locale;
import java.util.Optional;

/** The kind of skill a rite is about, and so the night it needs (plan 19, E3): an Ultimate needs a lunar eclipse, everything below it a full moon. */
public enum Tier {
    COMMON, EXTRA, UNIQUE, ULTIMATE;

    public enum Night { FULL_MOON, ECLIPSE }

    public Night night() {
        return this == ULTIMATE ? Night.ECLIPSE : Night.FULL_MOON;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Tier> parse(String id) {
        for (Tier t : values()) {
            if (t.id().equals(id)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /**
     * The tier of a Tensura skill by its {@code SkillType} name. Resistances, intrinsics and commons are Common; a skill that is not a Tensura
     * {@code Skill} (a magic, an art) counts as Extra. A skill that passes several types is the highest of them (the caller passes the highest).
     */
    public static Tier ofSkillType(String skillTypeName, boolean tensuraSkill) {
        if (!tensuraSkill) {
            return EXTRA;
        }
        return switch (skillTypeName) {
            case "ULTIMATE" -> ULTIMATE;
            case "UNIQUE" -> UNIQUE;
            case "EXTRA" -> EXTRA;
            default -> COMMON;
        };
    }
}
