package net.ofatech.supernaturalbloodlines.core.rite;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tensura races of other addons that count as one of our kinds for the rites (spec section 9, plan 19d D2). Ascension's five Bloodfiends are vampires;
 * Mysticism's direwolf and every one of its spirit wolves are werewolves. Nothing else is linked: Mysticism's fang, frost, scorch, star, tempest and guitar
 * lines and its divine wolf are not named by the decision, nor is Ascension's kitsune branch. Pure.
 */
public final class RaceLinks {
    public static final Set<String> VAMPIRE_RACES = Set.of("ascension:fledgling_bloodfiend", "ascension:kindred_bloodfiend", "ascension:blood_noble",
            "ascension:elder_bloodfiend", "ascension:progenitor_bloodfiend");
    static final String DIREWOLF = "mysticism:direwolf";
    static final String SPIRIT_WOLF_PREFIX = "mysticism:";
    static final String SPIRIT_WOLF_SUFFIX = "_spirit_wolf";

    private RaceLinks() {
    }

    public static boolean isVampire(String raceId) {
        return raceId != null && VAMPIRE_RACES.contains(raceId);
    }

    public static boolean isWerewolf(String raceId) {
        if (raceId == null) {
            return false;
        }
        return raceId.equals(DIREWOLF) || raceId.startsWith(SPIRIT_WOLF_PREFIX) && raceId.endsWith(SPIRIT_WOLF_SUFFIX)
                && raceId.length() > SPIRIT_WOLF_PREFIX.length() + SPIRIT_WOLF_SUFFIX.length() && raceId.charAt(SPIRIT_WOLF_PREFIX.length()) != '_';
    }

    /** The kinds a Tensura race adds; empty for every race that is not linked. */
    public static Set<RiteKind> kindsOfRace(String raceId) {
        Set<RiteKind> out = EnumSet.noneOf(RiteKind.class);
        if (isVampire(raceId)) {
            out.add(RiteKind.VAMPIRE);
        }
        if (isWerewolf(raceId)) {
            out.add(RiteKind.WEREWOLF);
        }
        return out;
    }
}
