package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;

/** R270: a new member is a Night Walker; the leader promotes and demotes; the clock promotes after the days unless the leader demoted them. */
public final class ClanRanks {
    private static final long DAY = 24L * 60 * 60 * 1000;

    public enum Refusal {
        NOT_LEADER, IS_LEADER, NOT_A_MEMBER, ALREADY_DAY_WALKER, NOT_A_DAY_WALKER;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private ClanRanks() {
    }

    /** {@code days} 0 means only the leader promotes. */
    public static boolean autoPromotionDue(long joinedAtMillis, long nowMillis, int days, boolean demotedByLeader) {
        return days > 0 && !demotedByLeader && nowMillis - joinedAtMillis >= days * DAY;
    }

    public static Optional<Refusal> mayPromote(GroupRank callerRank, boolean callerIsLeader, GroupRank targetRank, boolean targetIsLeader, boolean sameClan) {
        Optional<Refusal> common = common(callerIsLeader, targetIsLeader, sameClan);
        if (common.isPresent()) {
            return common;
        }
        return targetRank == GroupRank.DAY_WALKER ? Optional.of(Refusal.ALREADY_DAY_WALKER) : Optional.empty();
    }

    public static Optional<Refusal> mayDemote(GroupRank callerRank, boolean callerIsLeader, GroupRank targetRank, boolean targetIsLeader, boolean sameClan) {
        Optional<Refusal> common = common(callerIsLeader, targetIsLeader, sameClan);
        if (common.isPresent()) {
            return common;
        }
        return targetRank == GroupRank.DAY_WALKER ? Optional.empty() : Optional.of(Refusal.NOT_A_DAY_WALKER);
    }

    private static Optional<Refusal> common(boolean callerIsLeader, boolean targetIsLeader, boolean sameClan) {
        if (!callerIsLeader) {
            return Optional.of(Refusal.NOT_LEADER);
        }
        if (targetIsLeader) {
            return Optional.of(Refusal.IS_LEADER);
        }
        return sameClan ? Optional.empty() : Optional.of(Refusal.NOT_A_MEMBER);
    }
}