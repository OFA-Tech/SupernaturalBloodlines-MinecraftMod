package net.ofatech.supernaturalbloodlines.core.group.coven;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;

/**
 * What the hub may know about the viewer's own place in a coven, and nothing about anybody else's: the circle they are in or are called to (names of the
 * viewer's own coven only), their own meter, consent and share, how long they are shunned and the coven's open vote as counts (never who voted what; only
 * the viewer's own ballot).
 *
 * @param tier       0 to 3, from the coven's size
 * @param size       the coven's members, the Regent counted
 * @param sharePower an example share based on the highest listed rite and the current circle
 */
public record CovenView(int tier, int size, Circle circle, String anchorName, List<String> circleNames, int meter, int meterCap, int regenSeconds, Consent consent,
                        int sharePower, int shunnedDaysLeft, Optional<Vote> vote, String callToken) {
    /** The viewer's relation to a circle. */
    public enum Circle { NONE, ANCHOR, JOINED, CALLED }

    /** The viewer's own ballot on the open vote. */
    public enum Ballot { NONE, YES, NO }

    public record Vote(VoteKind kind, UUID proposal, String subjectName, int yes, int no, int electors, Ballot mine, int hoursLeft) {
        public Vote {
            subjectName = clip(subjectName, CovenWire.MAX_NAME);
        }

        public String token() {
            return kind.token(proposal);
        }
    }

    public static final CovenView NONE = new CovenView(0, 0, Circle.NONE, "", List.of(), 0, 0, 0, Consent.NONE, 0, 0, Optional.empty(), "");

    public CovenView {
        anchorName = clip(anchorName, CovenWire.MAX_NAME);
        circleNames = circleNames.stream().limit(CovenWire.MAX_CIRCLE).map(n -> clip(n, CovenWire.MAX_NAME)).toList();
    }

    public static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** The same view with only the shunning set (a player who is not in a coven can still be shunned from one). */
    public CovenView shunned(int days) {
        return new CovenView(tier, size, circle, anchorName, circleNames, meter, meterCap, regenSeconds, consent, sharePower, days, vote, callToken);
    }
}
