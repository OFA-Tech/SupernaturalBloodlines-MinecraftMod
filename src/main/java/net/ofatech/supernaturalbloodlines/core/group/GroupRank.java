package net.ofatech.supernaturalbloodlines.core.group;

import java.util.Locale;

/** A stored rank below the leader: a clan's Day Walker, a coven's Elder. A row exists only for a rank that is not {@code NONE}. */
public enum GroupRank {
    NONE, DAY_WALKER, ELDER;

    /** The type this rank belongs to, or null for {@code NONE}. */
    public GroupType type() {
        return switch (this) {
            case DAY_WALKER -> GroupType.CLAN;
            case ELDER -> GroupType.COVEN;
            case NONE -> null;
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** An unknown string is {@code NONE}. */
    public static GroupRank parse(String id) {
        for (GroupRank r : values()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return NONE;
    }
}
