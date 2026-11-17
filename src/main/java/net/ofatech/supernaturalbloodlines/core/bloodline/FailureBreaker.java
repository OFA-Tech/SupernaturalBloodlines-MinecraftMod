package net.ofatech.supernaturalbloodlines.core.bloodline;

/**
 * Turns a flaky outside call off only when it keeps failing: {@code limit} failures in a row trip it for good, and any success
 * before that starts the count again. Pure, so the rule is tested without the outside mod.
 */
public final class FailureBreaker {
    private final int limit;
    private int run;
    private boolean tripped;

    public FailureBreaker(int limit) {
        this.limit = Math.max(1, limit);
    }

    /** @return true when this very call tripped the breaker (so the caller logs once) */
    public boolean record(boolean ok) {
        if (tripped) {
            return false;
        }
        if (ok) {
            run = 0;
            return false;
        }
        if (++run >= limit) {
            tripped = true;
            return true;
        }
        return false;
    }

    public boolean tripped() {
        return tripped;
    }
}
