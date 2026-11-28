package net.ofatech.supernaturalbloodlines.core.group;

import java.util.EnumSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;

/** Plan 20e §1.2: multi-type computation from leader facts, and per-type fit checks for members. */
public final class MultiTypes {
    private static final GroupType[] CANONICAL = {GroupType.PACK, GroupType.CLAN, GroupType.COVEN, GroupType.BROTHERHOOD};

    private MultiTypes() {
    }

    /** The active type set for a leader's kinds (owner decisions #1248–#1253). */
    public static Set<GroupType> of(Leader l) {
        EnumSet<GroupType> types = EnumSet.noneOf(GroupType.class);
        boolean hybrid = l.species().hybridType();
        boolean magicUser = l.species().witchType();
        boolean vampireLike = l.vampire() || isVampireSpecies(l.species());

        if (l.werewolf() || hybrid) {
            types.add(GroupType.PACK);
        }
        if (vampireLike && (hybrid || isVampireSpecies(l.species()))) {
            types.add(GroupType.CLAN);
        }
        if (magicUser || l.species() == Species.TRIBRID) {
            types.add(GroupType.COVEN);
        }
        if (l.hunter() || (l.species() == Species.NONE && l.humanLike() && !l.vampire() && !l.werewolf())) {
            types.add(GroupType.BROTHERHOOD);
        }

        // tribrid special case: pack + clan + coven
        if (l.species() == Species.TRIBRID) {
            types.add(GroupType.PACK);
            types.add(GroupType.CLAN);
            types.add(GroupType.COVEN);
        }

        // werewolf + witch = pack + coven (no clan)
        if (l.werewolf() && magicUser && !l.vampire()) {
            types.remove(GroupType.CLAN);
        }

        // hunter + witch = coven + brotherhood (no pack unless werewolf)
        if (l.hunter() && magicUser && !l.werewolf()) {
            types.remove(GroupType.PACK);
            types.add(GroupType.COVEN);
            types.add(GroupType.BROTHERHOOD);
        }

        if (types.isEmpty()) {
            types.add(GroupType.PACK);
        }
        return Set.copyOf(types);
    }

    /** First in canonical order PACK > CLAN > COVEN > BROTHERHOOD that is active; PACK when empty. */
    public static GroupType mainType(Set<GroupType> types) {
        for (GroupType t : CANONICAL) {
            if (types.contains(t)) {
                return t;
            }
        }
        return GroupType.PACK;
    }

    /** Whether a player with these facts fits the given type (for join/remain checks). */
    public static boolean fits(GroupType type, Leader l) {
        return switch (type) {
            case PACK -> true;
            case CLAN -> l.vampire() || isVampireSpecies(l.species());
            case COVEN -> l.species().witchType();
            case BROTHERHOOD -> l.hunter() || (l.species() == Species.NONE && l.humanLike() && !l.vampire() && !l.werewolf());
        };
    }

    private static boolean isVampireSpecies(Species s) {
        return s == Species.NONE || s == Species.ORIGINAL || s == Species.AUGUSTINE || s == Species.SIPHONER;
    }
}