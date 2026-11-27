package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** The mark is a Hunter of the Five's kills (R80-R83), capped; everybody else has none, whatever a leftover count says. */
public final class Marks {
    private Marks() {
    }

    public static int of(Species species, int markKills, int cap) {
        return species == Species.HUNTER_OF_THE_FIVE ? Math.max(0, Math.min(markKills, cap)) : 0;
    }

    public static boolean hasSense(Species species, int markKills, int senseKills) {
        return species == Species.HUNTER_OF_THE_FIVE && markKills >= senseKills;
    }

    /** A percent of the kills, at least 1 when there are any, never more than they are; nothing for 0 kills or 0 percent. */
    public static int loss(int kills, int percent) {
        if (kills <= 0 || percent <= 0) {
            return 0;
        }
        return Math.min(kills, Math.max(1, kills * percent / 100));
    }

    public static int after(int kills, int percent) {
        return kills - loss(kills, percent);
    }
}