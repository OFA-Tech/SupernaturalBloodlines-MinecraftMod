package net.ofatech.supernaturalbloodlines.core.group;

import java.util.Optional;

/** What changes when the type derived from the leader differs from the stored one (spec 3.4). */
public final class GroupSwitch {
    private GroupSwitch() {
    }

    public record Switch(GroupType from, GroupType to) {
    }

    public static Optional<Switch> plan(GroupType stored, GroupType derived) {
        return stored == derived ? Optional.empty() : Optional.of(new Switch(stored, derived));
    }

    /** A row is dormant unless its type is the active one; a row with no type (a plain rank) is never dormant. */
    public static boolean dormant(GroupType active, GroupType rowType) {
        return rowType != null && rowType != active;
    }
}
