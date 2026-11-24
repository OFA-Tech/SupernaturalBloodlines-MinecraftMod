package net.ofatech.supernaturalbloodlines.core.group.coven;

/** The count of a coven vote. Non-voters count as no once the time is up; a tie fails. */
public final class VoteTally {
    public enum Result { PENDING, PASSED, FAILED }

    /** Below this many electors (the Regent and the Elders) the Regent decides alone. */
    public static final int MIN_ELECTORS = 3;

    private VoteTally() {
    }

    /** An electorate of fewer than three is the Regent alone: appointing, removing and shunning need no vote. */
    public static boolean needsVote(int electors) {
        return electors >= MIN_ELECTORS;
    }

    /** No election for a new Regent can be called with an electorate under three. */
    public static boolean mayCallElection(int electors) {
        return electors >= MIN_ELECTORS;
    }

    /** A strict majority of the whole electorate, counted once everyone voted or the time is up. */
    public static Result result(int electors, int yes, int no, boolean everyoneVoted, boolean expired) {
        if (!everyoneVoted && !expired) {
            return Result.PENDING;
        }
        return yes * 2 > electors ? Result.PASSED : Result.FAILED;
    }
}