package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** Who a brotherhood's perks reach: a hunter, or a plain human (no species, no vampire, no werewolf, human-like). Everyone else is a member with no perks (R290, R298). */
public final class BrotherhoodFit {
    /** The facts about a player the question reads. */
    public record Facts(boolean hunter, Species species, boolean vampire, boolean werewolf, boolean humanLike) {
    }

    private BrotherhoodFit() {
    }

    public static boolean fits(Facts f) {
        return f.hunter() || (f.species() == Species.NONE && !f.vampire() && !f.werewolf() && f.humanLike());
    }
}