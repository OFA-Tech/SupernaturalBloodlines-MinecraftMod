package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Teen Wolf layer of a player: lineage, rank, pack and spark.
 *
 * @param heldSpark   the player has held a normal spark at some time (stolen, inherited, won): they can never be a True Alpha. An Alpha, and anyone
 *                    who stole one, always has (set on load, so a world from before this field migrates by itself)
 * @param protectPath the player's own choice, on by default: while the path to True Alpha is open, no spark is forced on them (see {@link PackRules#pathOpen})
 * @param preApex     the Tensura race the player had before {@code alpha_primeval} ("" for none): where they go back to while they hold no spark
 * @param weak        the weaker sparks of NPC Alphas (plan 15b, R145): they count for nothing until they have grown
 */
public record WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent,
                        @Nullable UUID packAlpha, long betaSince, int defenderKills, boolean stoleSpark,
                        Set<String> grantedSkills, int sparks, boolean heldSpark, boolean protectPath, String preApex, Weak weak) {

    /**
     * The weaker sparks an NPC Alpha gave (R145): how many are held, how many online ticks the oldest has grown, and the game day the last one was
     * given plus one (the cooldown; 0 is never). They count for nothing (not toward the cap, not toward the bonus) and cannot be stolen: they are not in {@code sparks}.
     */
    public record Weak(int held, int progress, long day) {
        public static final Weak NONE = new Weak(0, 0, 0);
        public static final Codec<Weak> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.lenientOptionalFieldOf("held", 0).forGetter(Weak::held),
                Codec.INT.lenientOptionalFieldOf("progress", 0).forGetter(Weak::progress),
                Codec.LONG.lenientOptionalFieldOf("day", 0L).forGetter(Weak::day)).apply(i, Weak::new));

        public Weak {
            held = Math.max(0, held);
            progress = held == 0 ? 0 : Math.max(0, progress);
        }
    }

    /** Without the weak sparks: none. */
    public WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent,
                     @Nullable UUID packAlpha, long betaSince, int defenderKills, boolean stoleSpark, Set<String> grantedSkills,
                     int sparks, boolean heldSpark, boolean protectPath, String preApex) {
        this(lineage, rank, geneRolled, latentGene, killedInnocent, packAlpha, betaSince, defenderKills, stoleSpark, grantedSkills, sparks, heldSpark,
                protectPath, preApex, Weak.NONE);
    }

    public int weakSparks() {
        return weak.held();
    }


    public enum Lineage { NONE, BORN, BITTEN }

    public enum Rank { NONE, OMEGA, BETA, ALPHA, TRUE_ALPHA }

    public static final WolfState NONE =
            new WolfState(Lineage.NONE, Rank.NONE, false, false, false, null, 0, 0, false, Set.of(), 1, false, true, "", Weak.NONE);

    /** Without a spark count: one spark, the Alpha's own. */
    public WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent,
                     @Nullable UUID packAlpha, long betaSince, int defenderKills, boolean stoleSpark, Set<String> grantedSkills) {
        this(lineage, rank, geneRolled, latentGene, killedInnocent, packAlpha, betaSince, defenderKills, stoleSpark, grantedSkills, 1);
    }

    /** Without the spark history, the protection choice and the pre-apex race: the defaults. */
    public WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent,
                     @Nullable UUID packAlpha, long betaSince, int defenderKills, boolean stoleSpark, Set<String> grantedSkills, int sparks) {
        this(lineage, rank, geneRolled, latentGene, killedInnocent, packAlpha, betaSince, defenderKills, stoleSpark, grantedSkills, sparks, false, true, "", Weak.NONE);
    }

    public static final Codec<WolfState> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.enumByName(Lineage.class, Lineage.NONE).lenientOptionalFieldOf("lineage", Lineage.NONE).forGetter(WolfState::lineage),
            BloodlineCodecs.enumByName(Rank.class, Rank.NONE).lenientOptionalFieldOf("rank", Rank.NONE).forGetter(WolfState::rank),
            Codec.BOOL.lenientOptionalFieldOf("gene_rolled", false).forGetter(WolfState::geneRolled),
            Codec.BOOL.lenientOptionalFieldOf("latent_gene", false).forGetter(WolfState::latentGene),
            Codec.BOOL.lenientOptionalFieldOf("killed_innocent", false).forGetter(WolfState::killedInnocent),
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("pack_alpha").forGetter(w -> Optional.ofNullable(w.packAlpha())),
            Codec.LONG.lenientOptionalFieldOf("beta_since", 0L).forGetter(WolfState::betaSince),
            Codec.INT.lenientOptionalFieldOf("defender_kills", 0).forGetter(WolfState::defenderKills),
            Codec.BOOL.lenientOptionalFieldOf("stole_spark", false).forGetter(WolfState::stoleSpark),
            Codec.STRING.listOf().lenientOptionalFieldOf("granted_skills", java.util.List.of())
                    .forGetter(w -> {
                        var sorted = new ArrayList<>(w.grantedSkills());
                        Collections.sort(sorted);
                        return sorted;
                    }),
            // stacked Alpha sparks (plan 14): an old save has none, which is one
            Codec.INT.lenientOptionalFieldOf("sparks", 1).forGetter(WolfState::sparks),
            Codec.BOOL.lenientOptionalFieldOf("held_spark", false).forGetter(WolfState::heldSpark),
            Codec.BOOL.lenientOptionalFieldOf("protect_path", true).forGetter(WolfState::protectPath),
            Codec.STRING.lenientOptionalFieldOf("pre_apex", "").forGetter(WolfState::preApex),
            Weak.CODEC.lenientOptionalFieldOf("weak", Weak.NONE).forGetter(WolfState::weak)
    ).apply(i, (lineage, rank, rolled, latent, innocent, alpha, since, kills, stole, skills, sparks, held, protect, apex, weak) ->
            new WolfState(lineage, rank, rolled, latent, innocent, alpha.orElse(null), since, kills, stole, new HashSet<>(skills), sparks, held, protect, apex, weak)));

    public WolfState {
        lineage = lineage == null ? Lineage.NONE : lineage;
        rank = rank == null ? Rank.NONE : rank;
        grantedSkills = grantedSkills == null ? Set.of() : Set.copyOf(grantedSkills);
        sparks = Math.max(1, sparks);
        preApex = preApex == null ? "" : preApex;
        weak = weak == null ? Weak.NONE : weak;
        // an Alpha holds a spark, and whoever stole one held it: the history of a save from before the field is read off the rank
        heldSpark = heldSpark || stoleSpark || rank == Rank.ALPHA;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Lineage lineage;
        private Rank rank;
        private boolean geneRolled;
        private boolean latentGene;
        private boolean killedInnocent;
        private UUID packAlpha;
        private long betaSince;
        private int defenderKills;
        private boolean stoleSpark;
        private Set<String> grantedSkills;
        private int sparks;
        private boolean heldSpark;
        private boolean protectPath;
        private String preApex;
        private Weak weak;

        private Builder(WolfState w) {
            lineage = w.lineage;
            rank = w.rank;
            geneRolled = w.geneRolled;
            latentGene = w.latentGene;
            killedInnocent = w.killedInnocent;
            packAlpha = w.packAlpha;
            betaSince = w.betaSince;
            defenderKills = w.defenderKills;
            stoleSpark = w.stoleSpark;
            grantedSkills = w.grantedSkills;
            sparks = w.sparks;
            heldSpark = w.heldSpark;
            protectPath = w.protectPath;
            preApex = w.preApex;
            weak = w.weak;
        }

        public Builder lineage(Lineage v) { lineage = v; return this; }
        public Builder rank(Rank v) { rank = v; return this; }
        public Builder geneRolled(boolean v) { geneRolled = v; return this; }
        public Builder latentGene(boolean v) { latentGene = v; return this; }
        public Builder killedInnocent(boolean v) { killedInnocent = v; return this; }
        public Builder packAlpha(@Nullable UUID v) { packAlpha = v; return this; }
        public Builder betaSince(long v) { betaSince = v; return this; }
        public Builder defenderKills(int v) { defenderKills = v; return this; }
        public Builder stoleSpark(boolean v) { stoleSpark = v; return this; }
        public Builder grantedSkills(Set<String> v) { grantedSkills = v; return this; }
        public Builder sparks(int v) { sparks = v; return this; }
        public Builder heldSpark(boolean v) { heldSpark = v; return this; }
        public Builder protectPath(boolean v) { protectPath = v; return this; }
        public Builder preApex(String v) { preApex = v; return this; }
        public Builder weak(Weak v) { weak = v; return this; }

        public WolfState build() {
            return new WolfState(lineage, rank, geneRolled, latentGene, killedInnocent, packAlpha, betaSince,
                    defenderKills, stoleSpark, grantedSkills, sparks, heldSpark, protectPath, preApex, weak);
        }
    }
}
