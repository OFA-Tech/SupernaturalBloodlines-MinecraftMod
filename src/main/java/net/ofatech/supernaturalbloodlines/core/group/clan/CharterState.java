package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.Locale;

/** Where a clan's Charter is: in a clan member's inventory, out of every inventory we read, on the ground, or gone. */
public enum CharterState {
    HELD, STORED, DROPPED, LOST;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** An unknown string is {@code STORED}: the next scan decides. */
    public static CharterState parse(String id) {
        for (CharterState s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return STORED;
    }
}