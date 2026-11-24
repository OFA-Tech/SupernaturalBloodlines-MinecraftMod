package net.ofatech.supernaturalbloodlines.core.group;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Who leads a group whose leader stayed away (R244). A pack's heir is the spark rule's, not this one's. */
public final class GroupSuccession {
    private GroupSuccession() {
    }

    /** {@code mark} is -1 for a candidate who is not online. */
    public record Candidate(UUID id, GroupRank rank, long joinedAt, boolean here, int mark) {
    }

    public static Optional<UUID> heir(GroupType type, List<Candidate> members) {
        Comparator<Candidate> byTenure = Comparator.comparingLong(Candidate::joinedAt).thenComparing(Candidate::id);
        return switch (type) {
            case PACK -> Optional.empty();
            case CLAN -> members.stream().filter(c -> c.here() && c.rank() == GroupRank.DAY_WALKER).min(byTenure).map(Candidate::id);
            case COVEN -> members.stream().filter(c -> c.here() && c.rank() == GroupRank.ELDER).min(byTenure).map(Candidate::id);
            case BROTHERHOOD -> members.stream().filter(c -> c.here() && c.mark() >= 0)
                    .min(Comparator.comparingInt(Candidate::mark).reversed().thenComparing(byTenure)).map(Candidate::id);
        };
    }
}
