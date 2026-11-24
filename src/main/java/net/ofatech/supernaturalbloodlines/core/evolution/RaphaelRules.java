package net.ofatech.supernaturalbloodlines.core.evolution;

/**
 * The merge of the two Raphaels (spec D14, E9): what to do when something learns Great Sage's Raphael while FoxAblaze's is the real one, and what to do
 * at login for a world that already holds Great Sage's. Pure: the facts are read by the listener.
 */
public final class RaphaelRules {
    /** What the learn of the source skill becomes. */
    public enum Verdict { PASS, ALREADY_HELD, REFUSE, REDIRECT }

    /** What the login does for a player holding the source skill. */
    public enum Sweep { NOTHING, CONVERT, DROP_SOURCE, LEAVE }

    /**
     * @param merged      the other addon is loaded and the learner is a player
     * @param holdsEither the learner holds either Raphael
     * @param ownedByOther FoxAblaze's single-owner rule says another player owns the Raphael or the Ciel
     */
    public record Facts(boolean merged, boolean holdsEither, boolean ownedByOther) {
    }

    private RaphaelRules() {
    }

    /** Holding either Raphael wins over everything: nothing more is given and nothing is taken, so the owner's answer is never asked. */
    public static Verdict ofLearn(Facts f) {
        if (!f.merged()) {
            return Verdict.PASS;
        }
        if (f.holdsEither()) {
            return Verdict.ALREADY_HELD;
        }
        return f.ownedByOther() ? Verdict.REFUSE : Verdict.REDIRECT;
    }

    public static Sweep onLogin(boolean merged, boolean holdsSource, boolean holdsTarget, boolean ownedByOther) {
        if (!merged || !holdsSource) {
            return Sweep.NOTHING;
        }
        if (holdsTarget) {
            return Sweep.DROP_SOURCE;
        }
        return ownedByOther ? Sweep.LEAVE : Sweep.CONVERT;
    }
}
