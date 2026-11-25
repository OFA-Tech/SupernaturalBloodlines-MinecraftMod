package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

/**
 * The viewer's own brotherhood as the hub draws it (R297): numbers and switches, nothing else. No string, no id, no list, no kind, and never another
 * brother's kills: the roster's names and ranks ride the group view. A viewer who does not fit, or perks that are off, carry no perk numbers.
 *
 * @param ownMark the viewer's own mark, 0 to 20
 * @param leaderMark the leader's recorded mark, 0 to 20
 * @param anyHasMax some brother has the full mark (the map is whole for everybody then)
 * @param armsBrothers the other brothers who count for the viewer's bonus now
 * @param armsPercent the bonus the viewer has now, in percent
 * @param strikes the breaks that still count towards an expulsion
 * @param breakerSecondsLeft seconds of the Code-breaker day left, 0 when none
 */
public record BrotherhoodView(boolean inBrotherhood, boolean fits, boolean perksOn, int ownMark, int leaderMark, boolean anyHasMax, int armsBrothers, int armsPercent,
                              boolean sightOn, boolean codeOn, int strikes, int strikeLimit, int breakerSecondsLeft, boolean leadByMark) {
    public static final BrotherhoodView NONE = new BrotherhoodView(false, false, false, 0, 0, false, 0, 0, false, false, 0, 0, 0, false);

    public BrotherhoodView {
        ownMark = BrotherhoodWire.clamp(ownMark, BrotherhoodWire.MAX_MARK);
        leaderMark = BrotherhoodWire.clamp(leaderMark, BrotherhoodWire.MAX_MARK);
        armsBrothers = BrotherhoodWire.clamp(armsBrothers, BrotherhoodWire.MAX_COUNT);
        armsPercent = BrotherhoodWire.clamp(armsPercent, BrotherhoodWire.MAX_PERCENT);
        strikes = BrotherhoodWire.clamp(strikes, BrotherhoodWire.MAX_STRIKES);
        strikeLimit = BrotherhoodWire.clamp(strikeLimit, BrotherhoodWire.MAX_STRIKES);
        breakerSecondsLeft = BrotherhoodWire.clamp(breakerSecondsLeft, BrotherhoodWire.MAX_SECONDS);
    }

    /** True when the viewer is a breaker now. */
    public boolean breaker() {
        return breakerSecondsLeft > 0;
    }
}