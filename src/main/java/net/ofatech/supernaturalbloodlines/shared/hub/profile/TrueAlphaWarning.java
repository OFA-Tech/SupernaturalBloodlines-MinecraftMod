package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;

/**
 * The warning before an Alpha Trial or a steal that would give a Beta or an Omega a spark: taking one ends their path to True Alpha. Owner ruling:
 * it shows for every Beta or Omega whose path is still open (never one who stole a spark already, never an Alpha). Only the rule and the dialog
 * are here; the Alpha Trial and the steal call {@link #applies} when they are built (a later PR changes the spark rules).
 */
public final class TrueAlphaWarning {
    private TrueAlphaWarning() {}

    public static boolean applies(WolfState.Rank rank, boolean pathStillOpen) {
        return pathStillOpen && (rank == WolfState.Rank.BETA || rank == WolfState.Rank.OMEGA);
    }
}

