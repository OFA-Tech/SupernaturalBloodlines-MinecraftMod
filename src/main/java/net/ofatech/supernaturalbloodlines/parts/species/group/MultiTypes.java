package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.EnumSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;

/** Pure multi-type computation from leader kinds (design §1.2) and per-type fit rules. */
public final class MultiTypes {
    /** The facts about a player a fit check reads. */
    public record Fit(boolean hunter, Species species, boolean vampire, boolean werewolf, boolean humanLike) {
    }

    private MultiTypes() {
    }

    /** Derives the active type set from the leader's kinds (design §1.2). */
    public static Set<GroupType> of(Leader l) {
        EnumSet<GroupType> types = EnumSet.noneOf(GroupType.class);
        boolean isWerewolfOrHybrid = l.werewolf() || l.species().hybridType();
        boolean isVampireLike = isVampireLikeForType(l.species(), l.vampire());
        boolean isWitchLike = isWitchLike(l.species());
        boolean isHunter = l.hunter();
        boolean isPlainHuman = l.species() == Species.NONE && !l.vampire() && !l.werewolf() && l.humanLike();

        if (l.species() == Species.TRIBRID) {
            types.add(GroupType.PACK);
            types.add(GroupType.CLAN);
            types.add(GroupType.COVEN);
        } else if (isWerewolfOrHybrid && isVampireLike) {
            types.add(GroupType.PACK);
            types.add(GroupType.CLAN);
        } else if (isVampireLike && isWitchLike) {
            types.add(GroupType.CLAN);
            types.add(GroupType.COVEN);
        } else if (isWerewolfOrHybrid && isWitchLike) {
            types.add(GroupType.PACK);
            types.add(GroupType.COVEN);
        } else if (isHunter && isWitchLike) {
            types.add(GroupType.COVEN);
            types.add(GroupType.BROTHERHOOD);
        } else if (isHunter || isPlainHuman) {
            types.add(GroupType.BROTHERHOOD);
        } else if (isVampireLike) {
            types.add(GroupType.CLAN);
        } else if (isWitchLike) {
            types.add(GroupType.COVEN);
        } else if (l.werewolf()) {
            types.add(GroupType.PACK);
        } else {
            types.add(GroupType.PACK);
        }

        return Set.copyOf(types);
    }

    /** Returns the main type: first in canonical order PACK > CLAN > COVEN > BROTHERHOOD that is active. */
    public static GroupType mainType(Set<GroupType> types) {
        for (GroupType t : new GroupType[] {GroupType.PACK, GroupType.CLAN, GroupType.COVEN, GroupType.BROTHERHOOD}) {
            if (types.contains(t)) {
                return t;
            }
        }
        return GroupType.PACK;
    }

    /** Whether a player fits a specific type (design §1.3). */
    public static boolean fits(GroupType type, Fit f) {
        return switch (type) {
            case PACK -> true;
            case CLAN -> isVampireLikeForFit(f.species(), f.vampire());
            case COVEN -> isWitchLike(f.species());
            case BROTHERHOOD -> f.hunter() || (f.species() == Species.NONE && !f.vampire() && !f.werewolf() && f.humanLike());
        };
    }

    private static boolean isWitchLike(Species s) {
        return s == Species.WITCH || s == Species.SIPHONER || s == Species.HERETIC;
    }

    private static boolean isVampireLikeForType(Species s, boolean vampireFlag) {
        if (vampireFlag) {
            return true;
        }
        return switch (s) {
            case ORIGINAL, AUGUSTINE -> true;
            default -> false;
        };
    }

    private static boolean isVampireLikeForFit(Species s, boolean vampireFlag) {
        if (vampireFlag) {
            return true;
        }
        return switch (s) {
            case NONE, ORIGINAL, AUGUSTINE, SIPHONER -> true;
            default -> false;
        };
    }
}