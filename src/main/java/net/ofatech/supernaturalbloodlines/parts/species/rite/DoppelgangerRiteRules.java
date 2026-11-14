package net.ofatech.supernaturalbloodlines.parts.species.rite;

import org.jetbrains.annotations.Nullable;

/** Who a Rite of Doppelgänger Blood accepts a taglock of (R14), as pure rules. */
public final class DoppelgangerRiteRules {
    public static final String NO_OWNER = "supernaturalbloodlines.gene.refusal.no_owner";
    public static final String OWNER_AWAY = "supernaturalbloodlines.gene.refusal.owner_away";
    public static final String NOT_DOPPELGANGER = "supernaturalbloodlines.gene.refusal.not_doppelganger";
    public static final String NOT_HUMAN_LIKE = "supernaturalbloodlines.gene.refusal.not_human_like";

    private DoppelgangerRiteRules() {
    }

    /**
     * The refusal key, null when the taglock is accepted.
     *
     * @param acceptsHumans {@code doppelganger_rite_taglock = human}
     * @param doppelganger  the owner is a doppelgänger villager or a player with the gene
     * @param humanLike     the owner is a human-like villager or player
     */
    @Nullable
    public static String refusal(boolean acceptsHumans, boolean doppelganger, boolean humanLike) {
        if (acceptsHumans) {
            return humanLike ? null : NOT_HUMAN_LIKE;
        }
        return doppelganger ? null : NOT_DOPPELGANGER;
    }
}
