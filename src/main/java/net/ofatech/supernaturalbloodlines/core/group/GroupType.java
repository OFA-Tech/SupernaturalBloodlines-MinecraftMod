package net.ofatech.supernaturalbloodlines.core.group;

import java.util.Locale;
import java.util.Optional;

/** The four kinds of group; the type is derived from the leader, never chosen. */
public enum GroupType {
    PACK, CLAN, COVEN, BROTHERHOOD;

    /** Lower case: the NBT and lang form. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<GroupType> parse(String id) {
        for (GroupType t : values()) {
            if (t.id().equals(id)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
