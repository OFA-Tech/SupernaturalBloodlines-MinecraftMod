package net.ofatech.supernaturalbloodlines.core.group.clan;

/** The master switch of every clan perk (R280): groups on and clans on. Off means nothing new happens and nothing is deleted. */
public final class ClanSwitches {
    private ClanSwitches() {
    }

    public static boolean perksOn(boolean groupsOn, boolean clansOn) {
        return groupsOn && clansOn;
    }
}
