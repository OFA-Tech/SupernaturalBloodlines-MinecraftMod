package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

/** The master switch of every brotherhood perk (R298): groups on and brotherhoods on. Off means nothing new happens and nothing is deleted. */
public final class BrotherhoodSwitches {
    private BrotherhoodSwitches() {
    }

    public static boolean perksOn(boolean groupsOn, boolean brotherhoodOn) {
        return groupsOn && brotherhoodOn;
    }
}
