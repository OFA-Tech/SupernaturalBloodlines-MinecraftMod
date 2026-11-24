package net.ofatech.supernaturalbloodlines.core.group.circle;

import java.util.Locale;

/** How much a member lets a rite ask of them beyond their channel meter. Stored per player, never per circle. */
public enum Consent {
    NONE, NOSEBLEED, DAMAGE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Unknown text answers {@code NONE}: the safe level. */
    public static Consent parse(String id) {
        for (Consent c : values()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return NONE;
    }
}