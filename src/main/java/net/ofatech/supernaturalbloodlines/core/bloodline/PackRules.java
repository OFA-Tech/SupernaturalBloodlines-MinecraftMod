package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;

/** Pure rules for the Teen Wolf layer (R90 to R94). Refusals leave the state unchanged. */
public final class PackRules {
    private PackRules() {
    }

    public static PackOutcome apply(WolfState w, PackEvent e, PackContext c) {
        return switch (e) {
            case GENE_AWAKENED -> geneAwakened(w);
            case RITE_OF_LINEAGE_AWAKEN -> lineageRite(w);
            case BITTEN_BY_ALPHA -> bittenByAlpha(w, c);
            case BITTEN_BY_WILD -> isWerewolf(w) ? ok(w) : ok(w.toBuilder().lineage(Lineage.BITTEN).rank(Rank.OMEGA).build());
            case JOINED_PACK -> joinedPack(w, c);
            case LEFT_PACK -> leftPack(w);
            case KILLED_ALPHA -> killedAlpha(w);
            case SPARK_INHERITED -> isWerewolf(w) && !isAlpha(w) ? ok(w.toBuilder().rank(Rank.ALPHA).packAlpha(null).sparks(1).heldSpark(true).build()) : ok(w);
            case SPARK_GIVEN_UP -> w.rank() == Rank.TRUE_ALPHA ? refuse(w, "true_alpha_keeps_spark")
                    : isAlpha(w)
                    ? ok(w.toBuilder().rank(Rank.BETA).packAlpha(c.otherAlpha()).betaSince(c.gameTime()).sparks(1).build())
                    : ok(w);
            // the spark is gone (stolen, or passed on with the faction): an Alpha with nothing left is an Omega. A True Alpha never loses it.
            case SPARK_LOST -> w.rank() == Rank.TRUE_ALPHA ? refuse(w, "true_alpha_keeps_spark")
                    : isAlpha(w) ? ok(w.toBuilder().rank(Rank.OMEGA).packAlpha(null).betaSince(0).sparks(1).weak(WeakSparkRules.lost(w.weak())).build()) : ok(w);
            case TRUE_ALPHA_EARNED -> trueAlpha(w, c);
            // Only a werewolf's eyes turn blue; the caller sends GENE_AWAKENED first when a kill awakens a human.
            case KILLED_INNOCENT -> isWerewolf(w) ? ok(w.toBuilder().killedInnocent(true).build()) : ok(w);
            case DEFENDER_KILL -> ok(w.toBuilder().defenderKills(w.defenderKills() + 1).build());
        };
    }

    // ---- stacked Alpha sparks (plan 14, the Deucalion factor) ----

    /** Each spark beyond the first adds this much to the Alpha rank's attack and health bonuses. */
    public static final double SPARK_BONUS = 0.10;

    /** How many of a victim's sparks a killer takes: all of them. A True Alpha's spark is theirs by nature and is never taken (0: just a death). */
    public static int transferableSparks(int victimSparks, boolean victimTrueAlpha) {
        return victimTrueAlpha ? 0 : Math.max(1, victimSparks);
    }

    /** What a kill of an Alpha means for the killer: they take the spark, or it is destroyed to protect their path, or a True Alpha takes nothing. */
    public enum Steal { TAKES, PROTECTED, NEVER }

    /**
     * Whether the path to True Alpha is still open: a werewolf Beta or Omega who never held a spark and never killed an innocent (blue eyes).
     * Taking a spark closes it for good, so it is what every warning and the protection look at.
     */
    public static boolean pathOpen(WolfState w) {
        return w.lineage() != Lineage.NONE && (w.rank() == Rank.BETA || w.rank() == Rank.OMEGA) && !w.heldSpark() && !w.killedInnocent();
    }

    /** What {@code killer} does with the spark of an Alpha they killed: a True Alpha never steals; one with an open, protected path destroys it instead. */
    public static Steal stealBy(WolfState killer) {
        if (killer.rank() == Rank.TRUE_ALPHA) {
            return Steal.NEVER;
        }
        return pathOpen(killer) && killer.protectPath() ? Steal.PROTECTED : Steal.TAKES;
    }

    /**
     * The sparks that count for the bonus and the Roar: a True Alpha who is active (a werewolf now) always holds the {@code cap}, whatever
     * was stacked on them before; anyone else their own count. Dormant, the True Alpha's count is the plain one.
     */
    public static int effectiveSparks(Rank rank, int sparks, int cap, boolean active) {
        return rank == Rank.TRUE_ALPHA && active ? Math.max(1, cap) : Math.max(1, sparks);
    }

    /** The killer's sparks after a steal: their own (0 for a non-Alpha, who becomes an Alpha by the first one) plus what they took, up to {@code cap}. */
    public static int sparksAfterSteal(int killerSparks, boolean killerIsAlpha, int taken, int cap) {
        int own = killerIsAlpha ? Math.max(1, killerSparks) : 0;
        return Math.max(1, Math.min(Math.max(1, cap), own + Math.max(0, taken)));
    }

    /** What the Alpha rank's attribute bonuses are multiplied by: +10% for every spark beyond the first (up to {@code cap}). */
    public static double sparkScale(int sparks, int cap) {
        int extra = Math.max(0, Math.min(Math.max(1, sparks), Math.max(1, cap)) - 1);
        return 1 + SPARK_BONUS * extra;
    }

    /** The Alpha Roar's Strength amplifier: Strength I with one spark, one tier higher for each extra one. */
    public static int roarTier(int sparks, int cap) {
        return Math.max(0, Math.min(Math.max(1, sparks), Math.max(1, cap)) - 1);
    }

    private static boolean isWerewolf(WolfState w) {
        return w.lineage() != Lineage.NONE;
    }

    private static boolean isAlpha(WolfState w) {
        return w.rank() == Rank.ALPHA || w.rank() == Rank.TRUE_ALPHA;
    }

    private static PackOutcome ok(WolfState w) {
        return new PackOutcome(w, null);
    }

    private static PackOutcome refuse(WolfState unchanged, String reason) {
        return new PackOutcome(unchanged, Outcome.REFUSAL_PREFIX + reason);
    }

    private static PackOutcome geneAwakened(WolfState w) {
        if (!w.latentGene()) {
            return refuse(w, "no_gene");
        }
        return isWerewolf(w) ? ok(w) : ok(w.toBuilder().lineage(Lineage.BORN).rank(Rank.OMEGA).build());
    }

    // Gives the gene even to someone who already is a werewolf; it only awakens a target with no lineage.
    private static PackOutcome lineageRite(WolfState w) {
        WolfState.Builder b = w.toBuilder().geneRolled(true).latentGene(true);
        if (!isWerewolf(w)) {
            b.lineage(Lineage.BORN).rank(Rank.OMEGA);
        }
        return ok(b.build());
    }

    private static PackOutcome bittenByAlpha(WolfState w, PackContext c) {
        if (c.otherAlpha() == null) {
            return refuse(w, "no_alpha");
        }
        if (isWerewolf(w)) {
            return ok(w);
        }
        return ok(w.toBuilder().lineage(Lineage.BITTEN).rank(Rank.BETA).packAlpha(c.otherAlpha()).betaSince(c.gameTime()).build());
    }

    private static PackOutcome joinedPack(WolfState w, PackContext c) {
        if (!isWerewolf(w)) {
            return refuse(w, "not_werewolf");
        }
        if (c.otherAlpha() == null) {
            return refuse(w, "no_alpha");
        }
        if (!c.otherAlphaHasPack()) {
            return refuse(w, "no_pack");
        }
        if (isAlpha(w)) {
            return ok(w);
        }
        return ok(w.toBuilder().rank(Rank.BETA).packAlpha(c.otherAlpha()).betaSince(c.gameTime()).build());
    }

    private static PackOutcome leftPack(WolfState w) {
        if (!isWerewolf(w) || isAlpha(w)) {
            return ok(w);
        }
        return ok(w.toBuilder().rank(Rank.OMEGA).packAlpha(null).betaSince(0).build());
    }

    private static PackOutcome killedAlpha(WolfState w) {
        if (!isWerewolf(w)) {
            return refuse(w, "not_werewolf");
        }
        if (w.rank() == Rank.TRUE_ALPHA) {
            return refuse(w, "true_alpha_never_steals");
        }
        WolfState.Builder b = w.toBuilder().stoleSpark(true).heldSpark(true);
        if (!isAlpha(w)) {
            b.rank(Rank.ALPHA).packAlpha(null).betaSince(0);
        }
        return ok(b.build());
    }

    // R91: checked in the order blue eyes, level, beta days, defender kills, spark.
    private static PackOutcome trueAlpha(WolfState w, PackContext c) {
        if (!isWerewolf(w)) {
            return refuse(w, "not_werewolf");
        }
        if (w.rank() == Rank.TRUE_ALPHA) {
            return ok(w);
        }
        if (w.killedInnocent()) {
            return refuse(w, "true_alpha_blue_eyes");
        }
        // A maxLevel of 0 means the caller did not supply the cap: never grant on an empty check.
        if (c.maxLevel() <= 0 || c.werewolfLevel() < c.maxLevel()) {
            return refuse(w, "true_alpha_level");
        }
        if (w.betaSince() <= 0 || c.gameTime() - w.betaSince() < c.betaDaysTicks()) {
            return refuse(w, "true_alpha_beta_days");
        }
        if (w.defenderKills() < c.defenderKillsNeeded()) {
            return refuse(w, "true_alpha_defender_kills");
        }
        if (w.heldSpark() || w.stoleSpark()) {
            return refuse(w, "true_alpha_held_spark");
        }
        // an Alpha leads: they leave the pack they were a Beta in
        // a True Alpha holds no weaker spark (R145)
        return ok(w.toBuilder().rank(Rank.TRUE_ALPHA).packAlpha(null).betaSince(0).sparks(1).weak(WeakSparkRules.lost(w.weak())).build());
    }
}
