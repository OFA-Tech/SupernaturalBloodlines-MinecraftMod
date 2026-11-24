package net.ofatech.supernaturalbloodlines.core.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the hub may know about the group the viewer is in, and nothing about anybody outside it: the type, the viewer's own place,
 * who leads, how long ago the type was last stamped and the member rows. Never a meter, a consent or a vote.
 *
 * @param viewerRank one of the constants below
 * @param members    at most {@link GroupWire#MAX_MEMBERS}, the leader first
 */
public record GroupView(GroupType type, boolean hasGroup, byte viewerRank, String leaderName, int sinceDays, List<Row> members) {
    public static final byte NOT_IN = 0;
    public static final byte LEADER = 1;
    public static final byte MEMBER = 2;
    public static final byte DAY_WALKER = 3;
    public static final byte ELDER = 4;

    /** One member as the hub draws them: {@code rank} is the page's label key suffix, {@code groupRank} the stored rank. */
    public record Row(String name, String rank, GroupRank groupRank, boolean online, Optional<UUID> id) {
    }

    public static final GroupView NONE = new GroupView(GroupType.PACK, false, NOT_IN, "", 0, List.of());

    public GroupView {
        members = List.copyOf(members.subList(0, Math.min(members.size(), GroupWire.MAX_MEMBERS)));
    }

    /** A name or a rank longer than the wire allows is cut, never refused. */
    public static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** No group yet: the type is the one the viewer's own kind would found, for the page's hint. */
    public static GroupView none(GroupType kind) {
        return new GroupView(kind, false, NOT_IN, "", 0, List.of());
    }
}
