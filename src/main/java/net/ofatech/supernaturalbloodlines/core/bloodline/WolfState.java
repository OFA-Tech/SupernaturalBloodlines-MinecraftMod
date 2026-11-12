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

/** The Teen Wolf layer of a player: lineage, rank, pack and spark. */
public record WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent,
                        @Nullable UUID packAlpha, long betaSince, int defenderKills, boolean stoleSpark,
                        Set<String> grantedSkills) {

    public enum Lineage { NONE, BORN, BITTEN }

    public enum Rank { NONE, OMEGA, BETA, ALPHA, TRUE_ALPHA }

    public static final WolfState NONE =
            new WolfState(Lineage.NONE, Rank.NONE, false, false, false, null, 0, 0, false, Set.of());

    public static final Codec<WolfState> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.enumByName(Lineage.class, Lineage.NONE).optionalFieldOf("lineage", Lineage.NONE).forGetter(WolfState::lineage),
            BloodlineCodecs.enumByName(Rank.class, Rank.NONE).optionalFieldOf("rank", Rank.NONE).forGetter(WolfState::rank),
            Codec.BOOL.optionalFieldOf("gene_rolled", false).forGetter(WolfState::geneRolled),
            Codec.BOOL.optionalFieldOf("latent_gene", false).forGetter(WolfState::latentGene),
            Codec.BOOL.optionalFieldOf("killed_innocent", false).forGetter(WolfState::killedInnocent),
            BloodlineCodecs.UUID_CODEC.optionalFieldOf("pack_alpha").forGetter(w -> Optional.ofNullable(w.packAlpha())),
            Codec.LONG.optionalFieldOf("beta_since", 0L).forGetter(WolfState::betaSince),
            Codec.INT.optionalFieldOf("defender_kills", 0).forGetter(WolfState::defenderKills),
            Codec.BOOL.optionalFieldOf("stole_spark", false).forGetter(WolfState::stoleSpark),
            Codec.STRING.listOf().optionalFieldOf("granted_skills", java.util.List.of())
                    .forGetter(w -> {
                        var sorted = new ArrayList<>(w.grantedSkills());
                        Collections.sort(sorted);
                        return sorted;
                    })
    ).apply(i, (lineage, rank, rolled, latent, innocent, alpha, since, kills, stole, skills) ->
            new WolfState(lineage, rank, rolled, latent, innocent, alpha.orElse(null), since, kills, stole, new HashSet<>(skills))));

    public WolfState {
        lineage = lineage == null ? Lineage.NONE : lineage;
        rank = rank == null ? Rank.NONE : rank;
        grantedSkills = grantedSkills == null ? Set.of() : Set.copyOf(grantedSkills);
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

        public WolfState build() {
            return new WolfState(lineage, rank, geneRolled, latentGene, killedInnocent, packAlpha, betaSince,
                    defenderKills, stoleSpark, grantedSkills);
        }
    }
}
