package net.ofatech.supernaturalbloodlines.core.group.coven;

import java.util.Locale;
import java.util.Optional;

/** What a coven votes on. The electorate is the Regent and the Elders. */
public enum VoteKind {
    ELDER_ADD, ELDER_REMOVE, SHUN, REGENT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<VoteKind> parse(String id) {
        for (VoteKind k : values()) {
            if (k.id().equals(id)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}