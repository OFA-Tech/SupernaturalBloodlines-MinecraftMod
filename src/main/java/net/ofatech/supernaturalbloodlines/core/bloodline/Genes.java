package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The two hidden genes (R10a, R10b) and what a client may know of them. Pure rules, no game classes.
 * <p>
 * The server keeps the raw values ({@code wolf.latentGene}, {@code doppelgangerGene}) and the discovered set
 * ({@code revealedGenes}). A client never gets an undiscovered value: {@link #forClient} fills the value fields and the set only
 * for the genes the client may know (discovered, or all of the subject's own under {@code gene_visibility=always}), and
 * {@link #ofClient} reads that back as {@link Knowledge}. Other players' genes are never sent.
 */
public final class Genes {
    public static final String WEREWOLF = "werewolf";
    public static final String DOPPELGANGER = "doppelganger";
    public static final List<String> ALL = List.of(WEREWOLF, DOPPELGANGER);

    /** What a client knows of one gene. */
    public enum Knowledge { UNKNOWN, TRUE, FALSE }

    private Genes() {
    }

    /** The raw value on a server state. */
    public static boolean has(BloodlineState s, String gene) {
        return switch (gene) {
            case WEREWOLF -> s.wolf().latentGene();
            case DOPPELGANGER -> s.doppelgangerGene();
            default -> false;
        };
    }

    /** {@code s} with these genes discovered; ids that are not genes are ignored, and the same state comes back when nothing is new. */
    public static BloodlineState reveal(BloodlineState s, String... genes) {
        Set<String> set = new HashSet<>(s.revealedGenes());
        for (String g : genes) {
            if (ALL.contains(g)) {
                set.add(g);
            }
        }
        return set.equals(s.revealedGenes()) ? s : s.toBuilder().revealedGenes(set).build();
    }

    /**
     * Migration for a player from before the genes were discovered: one who already awakened the werewolf gene (a born werewolf,
     * or a hybrid still bound by it) knows they carry it.
     */
    public static BloodlineState migrateDiscovered(BloodlineState s) {
        boolean awakened = s.wolf().latentGene() && (s.wolf().lineage() == WolfState.Lineage.BORN || s.hybridCurseBound());
        return awakened ? reveal(s, WEREWOLF) : s;
    }

    /**
     * The genes this player has been rolled for. The werewolf gene is never rolled without Werewolves, and a gene that was never
     * rolled is not "no", it is not there: the Rite of Revelation neither reveals nor reports it.
     */
    public static List<String> rolled(BloodlineState s) {
        List<String> out = new java.util.ArrayList<>();
        if (s.wolf().geneRolled()) {
            out.add(WEREWOLF);
        }
        if (s.doppelgangerRolled()) {
            out.add(DOPPELGANGER);
        }
        return out;
    }

    /** The Rite of Revelation: every gene, carried or not, becomes known. */
    public static BloodlineState revealAll(BloodlineState s) {
        return reveal(s, ALL.toArray(String[]::new));
    }

    /** What the subject's client is told about {@code gene} given the stored state. */
    public static Knowledge knowledge(BloodlineState s, String gene, boolean always) {
        if (!ALL.contains(gene) || !always && !s.revealedGenes().contains(gene)) {
            return Knowledge.UNKNOWN;
        }
        return has(s, gene) ? Knowledge.TRUE : Knowledge.FALSE;
    }

    /**
     * {@code s} as the client may see it: the gene fields are cleared, then filled again for the genes the subject's own client may
     * know. A viewer who is not the subject gets none.
     */
    public static BloodlineState forClient(BloodlineState s, boolean viewerIsSubject, boolean always) {
        Knowledge wolf = viewerIsSubject ? knowledge(s, WEREWOLF, always) : Knowledge.UNKNOWN;
        Knowledge doppel = viewerIsSubject ? knowledge(s, DOPPELGANGER, always) : Knowledge.UNKNOWN;
        Set<String> known = new HashSet<>();
        if (wolf != Knowledge.UNKNOWN) {
            known.add(WEREWOLF);
        }
        if (doppel != Knowledge.UNKNOWN) {
            known.add(DOPPELGANGER);
        }
        return s.toBuilder()
                .wolf(s.wolf().toBuilder().latentGene(wolf == Knowledge.TRUE).geneRolled(false).build())
                .doppelgangerGene(doppel == Knowledge.TRUE).doppelgangerRolled(false)
                .revealedGenes(known).build();
    }

    /** Client side: reads a state that went through {@link #forClient}. */
    public static Knowledge ofClient(BloodlineState view, String gene) {
        if (!view.revealedGenes().contains(gene)) {
            return Knowledge.UNKNOWN;
        }
        return has(view, gene) ? Knowledge.TRUE : Knowledge.FALSE;
    }
}
