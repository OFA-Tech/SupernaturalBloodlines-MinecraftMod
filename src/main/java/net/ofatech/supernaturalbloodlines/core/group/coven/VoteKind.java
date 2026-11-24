package net.ofatech.supernaturalbloodlines.core.group.coven;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** What a coven votes on. The electorate is the Regent and the Elders. */
public enum VoteKind {
    ELDER_ADD, ELDER_REMOVE, SHUN, REGENT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String token(UUID proposal) {
        return id() + "." + proposal;
    }

    public static Optional<VoteKind> ofAnswer(String answer) {
        int separator = answer.indexOf('.');
        return parse(separator < 0 ? answer : answer.substring(0, separator));
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
