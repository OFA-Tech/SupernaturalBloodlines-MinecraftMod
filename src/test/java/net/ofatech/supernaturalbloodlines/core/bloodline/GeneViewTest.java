package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.Genes.Knowledge;
import org.junit.jupiter.api.Test;

import static net.ofatech.supernaturalbloodlines.core.bloodline.Genes.DOPPELGANGER;
import static net.ofatech.supernaturalbloodlines.core.bloodline.Genes.WEREWOLF;
import static org.junit.jupiter.api.Assertions.*;

class GeneViewTest {
    private static BloodlineState carrier() {
        return BloodlineState.NONE.toBuilder()
                .wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build())
                .doppelgangerGene(true).doppelgangerRolled(true).build();
    }

    @Test
    void knownLineNeverReachesAClient() {
        BloodlineState s = BloodlineState.NONE.toBuilder().knownLine(java.util.UUID.randomUUID()).build();
        assertNull(net.ofatech.supernaturalbloodlines.core.BloodlineAccess.clientView(s, true).knownLine());
        assertNull(net.ofatech.supernaturalbloodlines.core.BloodlineAccess.clientView(s, false).knownLine());
    }

    @Test
    void undiscoveredGenesAreUnknownAndCarryNoValue() {
        BloodlineState view = Genes.forClient(carrier(), true, false);
        assertEquals(Knowledge.UNKNOWN, Genes.ofClient(view, WEREWOLF));
        assertEquals(Knowledge.UNKNOWN, Genes.ofClient(view, DOPPELGANGER));
        assertFalse(view.wolf().latentGene());
        assertFalse(view.doppelgangerGene());
        assertTrue(view.revealedGenes().isEmpty());
    }

    @Test
    void aRevealedGeneShowsItsValueAndTheOtherStaysUnknown() {
        BloodlineState view = Genes.forClient(Genes.reveal(carrier(), DOPPELGANGER), true, false);
        assertEquals(Knowledge.TRUE, Genes.ofClient(view, DOPPELGANGER));
        assertEquals(Knowledge.UNKNOWN, Genes.ofClient(view, WEREWOLF));
        assertFalse(view.wolf().latentGene(), "the werewolf value leaked");
    }

    @Test
    void aRevealedGeneTheSubjectLacksIsFalseNotUnknown() {
        BloodlineState view = Genes.forClient(Genes.revealAll(BloodlineState.NONE), true, false);
        assertEquals(Knowledge.FALSE, Genes.ofClient(view, WEREWOLF));
        assertEquals(Knowledge.FALSE, Genes.ofClient(view, DOPPELGANGER));
    }

    @Test
    void alwaysVisibilityShowsEveryGeneOfTheSubject() {
        BloodlineState view = Genes.forClient(carrier(), true, true);
        assertEquals(Knowledge.TRUE, Genes.ofClient(view, WEREWOLF));
        assertEquals(Knowledge.TRUE, Genes.ofClient(view, DOPPELGANGER));
        assertEquals(Knowledge.FALSE, Genes.ofClient(Genes.forClient(BloodlineState.NONE, true, true), WEREWOLF));
    }

    @Test
    void otherPlayersNeverSeeGenesNotEvenWhenRevealedOrAlways() {
        BloodlineState revealed = Genes.revealAll(carrier());
        for (boolean always : new boolean[]{false, true}) {
            BloodlineState view = Genes.forClient(revealed, false, always);
            assertEquals(Knowledge.UNKNOWN, Genes.ofClient(view, WEREWOLF));
            assertEquals(Knowledge.UNKNOWN, Genes.ofClient(view, DOPPELGANGER));
            assertFalse(view.wolf().latentGene());
            assertFalse(view.doppelgangerGene());
        }
    }

    @Test
    void theRolledFlagsNeverReachTheClient() {
        BloodlineState view = Genes.forClient(Genes.revealAll(carrier()), true, true);
        assertFalse(view.doppelgangerRolled());
        assertFalse(view.wolf().geneRolled());
    }

    @Test
    void revealAddsOnlyRealGenesAndKeepsTheSameStateWhenNothingIsNew() {
        BloodlineState once = Genes.reveal(BloodlineState.NONE, WEREWOLF, "dragon");
        assertEquals(java.util.Set.of(WEREWOLF), once.revealedGenes());
        assertSame(once, Genes.reveal(once, WEREWOLF));
        assertEquals(java.util.Set.of(WEREWOLF, DOPPELGANGER), Genes.revealAll(once).revealedGenes());
    }

    @Test
    void serverKnowledgeMatchesWhatTheClientReadsBack() {
        for (boolean wolf : new boolean[]{false, true}) {
            for (boolean doppel : new boolean[]{false, true}) {
                for (boolean always : new boolean[]{false, true}) {
                    BloodlineState s = BloodlineState.NONE.toBuilder()
                            .wolf(WolfState.NONE.toBuilder().latentGene(wolf).build()).doppelgangerGene(doppel).build();
                    for (BloodlineState base : new BloodlineState[]{s, Genes.reveal(s, WEREWOLF), Genes.revealAll(s)}) {
                        BloodlineState view = Genes.forClient(base, true, always);
                        for (String g : Genes.ALL) {
                            assertEquals(Genes.knowledge(base, g, always), Genes.ofClient(view, g), g);
                        }
                    }
                }
            }
        }
    }

    @Test
    void migrationDiscoversAnAlreadyAwakenedWerewolfGeneOnly() {
        WolfState gene = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();
        BloodlineState born = BloodlineState.NONE.toBuilder().wolf(gene.toBuilder().lineage(WolfState.Lineage.BORN).build()).build();
        BloodlineState bound = BloodlineState.NONE.toBuilder().wolf(gene).hybridCurseBound(true).build();
        BloodlineState latent = BloodlineState.NONE.toBuilder().wolf(gene).build();
        BloodlineState bitten = BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BITTEN).build()).build();
        assertEquals(java.util.Set.of(WEREWOLF), Genes.migrateDiscovered(born).revealedGenes());
        assertEquals(java.util.Set.of(WEREWOLF), Genes.migrateDiscovered(bound).revealedGenes());
        assertSame(latent, Genes.migrateDiscovered(latent), "a gene that never awakened stays secret");
        assertSame(bitten, Genes.migrateDiscovered(bitten), "a bitten werewolf has no gene to discover");
    }

    @Test
    void onlyRolledGenesCount() {
        assertTrue(Genes.rolled(BloodlineState.NONE).isEmpty());
        assertEquals(java.util.List.of(WEREWOLF), Genes.rolled(BloodlineState.NONE.toBuilder()
                .wolf(WolfState.NONE.toBuilder().geneRolled(true).build()).build()));
        assertEquals(java.util.List.of(DOPPELGANGER), Genes.rolled(BloodlineState.NONE.toBuilder().doppelgangerRolled(true).build()));
        assertEquals(java.util.List.of(WEREWOLF, DOPPELGANGER), Genes.rolled(carrier()));
    }

    @Test
    void anUnknownGeneIdIsUnknown() {
        assertEquals(Knowledge.UNKNOWN, Genes.knowledge(carrier(), "dragon", true));
    }
}
