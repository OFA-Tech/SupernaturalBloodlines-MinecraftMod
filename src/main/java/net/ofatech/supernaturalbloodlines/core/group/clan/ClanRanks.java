package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;

/** R270: a new member is a Night Walker; the leader promotes and demotes; the clock promotes after the days unless the leader demoted them. */
public final class ClanRanks {

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
        return days > 0 && !demotedByLeader && nowMillis - joinedAtMillis >= days * ClanTime.DAY;
    }

    /** Who may be sent away: only the leader, never themselves, only somebody of this clan. */
    public static Optional<Refusal> mayExile(boolean callerIsLeader, boolean targetIsLeader, boolean targetInClan) {
        return common(callerIsLeader, targetIsLeader, targetInClan);
    }

    public static Optional<Refusal> mayPromote(boolean callerIsLeader, GroupRank targetRank, boolean targetIsLeader, boolean targetInClan) {
        Optional<Refusal> common = common(callerIsLeader, targetIsLeader, targetInClan);
        if (common.isPresent()) {
            return common;
        }
        return targetRank == GroupRank.DAY_WALKER ? Optional.of(Refusal.ALREADY_DAY_WALKER) : Optional.empty();
    }

    public static Optional<Refusal> mayDemote(boolean callerIsLeader, GroupRank targetRank, boolean targetIsLeader, boolean targetInClan) {
        Optional<Refusal> common = common(callerIsLeader, targetIsLeader, targetInClan);
        if (common.isPresent()) {
            return common;
        }
        return targetRank == GroupRank.DAY_WALKER ? Optional.empty() : Optional.of(Refusal.NOT_A_DAY_WALKER);
    }

    private static Optional<Refusal> common(boolean callerIsLeader, boolean targetIsLeader, boolean targetInClan) {
        if (!callerIsLeader) {
            return Optional.of(Refusal.NOT_LEADER);
        }
        if (targetIsLeader) {
            return Optional.of(Refusal.IS_LEADER);
        }
        return targetInClan ? Optional.empty() : Optional.of(Refusal.NOT_A_MEMBER);
    }
}