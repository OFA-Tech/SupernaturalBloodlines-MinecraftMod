package net.ofatech.supernaturalbloodlines.core.group;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** Which type a leader's kind leads, and who may found one. */
public final class GroupTypes {
    private GroupTypes() {
    }

    /** The facts about a leader the type reads. */
    public record Leader(Species species, boolean vampire, boolean werewolf, boolean hunter, boolean humanLike) {
    }

    /** What a would-be founder is, for the founding gate. */
    public record Fit(boolean activeAlpha, boolean vampire, boolean magicUser, boolean hunter) {
    }

    /** First match wins: werewolf or hybrid type, hunter, vampire, a plain human (no species, human-like), everything else. */
    public static GroupType of(Leader l) {
        if (l.werewolf() || l.species().hybridType()) {
            return GroupType.PACK;
        }
        if (l.hunter()) {
            return GroupType.BROTHERHOOD;
        }
        if (l.vampire() && switch (l.species()) {
            case NONE, ORIGINAL, AUGUSTINE, SIPHONER -> true;
            default -> false;
        }) {
            return GroupType.CLAN;
        }
        if (l.species() == Species.NONE && l.humanLike()) {
            return GroupType.BROTHERHOOD;
        }
        return GroupType.COVEN;
    }

    public static boolean canFound(GroupType type, Fit fit, boolean openFounding) {
        if (openFounding) {
            return true;
        }
        return switch (type) {
            case PACK -> fit.activeAlpha();
            case CLAN -> fit.vampire();
            case COVEN -> fit.magicUser();
            case BROTHERHOOD -> fit.hunter();
        };
    }
}
