package net.ofatech.supernaturalbloodlines.core.group.clan;

/**
 * What the hub may know about the viewer's own clan, and nothing about anybody outside it (R281): counts, percents, seconds, and the names of members of
 * that clan. No id of any person, no species, no sireline root, nothing of another clan.
 *
 * @param inClan   false for a viewer in no clan; every other field is then its empty value
 * @param autoPromoteDaysLeft the days until a Night Walker is promoted by the clock; -1 when they are not one or the clock is off
 */
public record ClanView(boolean inClan, int daysInClan, int autoPromoteDaysLeft, Charter charter, Contest contest, boolean feedOn, int feedPercent, int bonusPercent) {
    public static final byte CHARTER_NONE = 0;
    public static final byte CHARTER_HELD = 1;
    public static final byte CHARTER_STORED = 2;
    public static final byte CHARTER_DROPPED = 3;
    public static final byte CHARTER_LOST = 4;
    /** There is a Charter row but clans or Charters are off: inert, nothing happens with it. */
    public static final byte CHARTER_INERT = 5;

    public static final byte CONTEST_NONE = 0;
    public static final byte CONTEST_PENDING = 1;
    public static final byte CONTEST_RUNNING = 2;

    /** {@code holderName} is a member of this clan or empty. {@code mine} says the viewer holds it. */
    public record Charter(byte state, String holderName, boolean mine, boolean forgeable, int reforgeSecondsLeft) {
        public static final Charter NONE = new Charter(CHARTER_NONE, "", false, false, 0);

        public Charter {
            holderName = ClanWire.clip(holderName);
        }
    }

    /**
     * @param waitDaysLeft the days until what stops the viewer is over: the contest cooldown, or (for a viewer not long enough in the clan) the days still to wait
     * @param challengeWhy 0 when the viewer may challenge, else why not: the {@code ContestRules.Refusal} ordinal plus one
     */
    public record Contest(byte phase, String challengerName, int secondsLeft, int waitDaysLeft, boolean iMayChallenge, int challengeWhy, boolean iMustAccept) {
        public static final Contest NONE = new Contest(CONTEST_NONE, "", 0, 0, false, 0, false);

        public Contest {
            challengerName = ClanWire.clip(challengerName);
        }
    }

    public static final ClanView NONE = new ClanView(false, 0, -1, Charter.NONE, Contest.NONE, false, 0, 0);
}
