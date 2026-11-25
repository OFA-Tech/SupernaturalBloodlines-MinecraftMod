package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.List;

/** R293: what a brother who senses prey shares with the brothers who lack the sense. */
public final class SightRules {
    private SightRules() {
    }

    public static boolean gives(boolean fits, boolean breaker, boolean hasSense) {
        return fits && !breaker && hasSense;
    }

    public static boolean receives(boolean fits, boolean breaker, boolean hasSense, boolean online, boolean sameDimension, double distSq, int radius) {
        return fits && !breaker && !hasSense && online && sameDimension && distSq <= (double) radius * radius;
    }

    /** The ids to send to one recipient: never their own. */
    public static List<Integer> idsFor(List<Integer> ids, int recipientId) {
        return ids.stream().filter(id -> id != recipientId).toList();
    }
}