package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** R291: the lead moves to an online brother whose mark is strictly higher than the leader's; a tie keeps the leader. */
public final class LeadByMark {
    public record Candidate(UUID id, int mark, boolean online, boolean fits, boolean breaker, long joinedAt) {
    }

    private LeadByMark() {
    }

    /** The highest mark, then the longest in the group, then the lower id. */
    public static Optional<UUID> pick(UUID leader, int leaderMark, List<Candidate> members) {
        return members.stream()
                .filter(c -> !c.id().equals(leader) && c.online() && c.fits() && !c.breaker() && c.mark() > leaderMark)
                .min(Comparator.comparingInt(Candidate::mark).reversed().thenComparingLong(Candidate::joinedAt).thenComparing(Candidate::id))
                .map(Candidate::id);
    }
}