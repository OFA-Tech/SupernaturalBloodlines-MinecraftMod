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
            case SPARK_INHERITED -> isWerewolf(w) && !isAlpha(w) ? ok(w.toBuilder().rank(Rank.ALPHA).packAlpha(null).build()) : ok(w);
            case SPARK_GIVEN_UP -> isAlpha(w)
                    ? ok(w.toBuilder().rank(Rank.BETA).packAlpha(c.otherAlpha()).betaSince(c.gameTime()).build())
                    : ok(w);
            // the spark is gone (stolen, or passed on with the faction): an Alpha with nothing left is an Omega. A True Alpha never loses it.
            case SPARK_LOST -> w.rank() == Rank.TRUE_ALPHA ? refuse(w, "true_alpha_keeps_spark")
                    : isAlpha(w) ? ok(w.toBuilder().rank(Rank.OMEGA).packAlpha(null).betaSince(0).build()) : ok(w);
            case TRUE_ALPHA_EARNED -> trueAlpha(w, c);
            // Only a werewolf's eyes turn blue; the caller sends GENE_AWAKENED first when a kill awakens a human.
            case KILLED_INNOCENT -> isWerewolf(w) ? ok(w.toBuilder().killedInnocent(true).build()) : ok(w);
            case DEFENDER_KILL -> ok(w.toBuilder().defenderKills(w.defenderKills() + 1).build());
        };
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
        WolfState.Builder b = w.toBuilder().stoleSpark(true);
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
        if (w.stoleSpark()) {
            return refuse(w, "true_alpha_stole_spark");
        }
        // an Alpha leads: they leave the pack they were a Beta in
        return ok(w.toBuilder().rank(Rank.TRUE_ALPHA).packAlpha(null).betaSince(0).build());
    }
}
