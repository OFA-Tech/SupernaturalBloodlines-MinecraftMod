package net.ofatech.supernaturalbloodlines.core;

import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodlineListenerTest {
    private static final BloodlineState NONE = BloodlineState.NONE;
    private static final BloodlineState WITCH = NONE.toBuilder().species(Species.WITCH).build();
    private static final BloodlineState SIPHONER = NONE.toBuilder().species(Species.SIPHONER).build();

    @Test
    void listenerNotifiedOnSpeciesLineageOrRankChangeOnly() {
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = List.of((p, c) -> seen.add(c));
        UUID id = UUID.randomUUID();
        BloodlineState witchThirsty = WITCH.toBuilder().thirst(3).build();

        BloodlineAccess.notifyListeners(listeners, null, id, NONE, WITCH);
        assertEquals(1, seen.size());
        assertEquals(NONE, seen.get(0).before());
        assertEquals(WITCH, seen.get(0).after());

        BloodlineAccess.notifyListeners(listeners, null, id, WITCH, witchThirsty);
        BloodlineAccess.notifyListeners(listeners, null, id, WITCH, WITCH);
        assertEquals(1, seen.size());

        WolfState bornBeta = WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BORN).rank(WolfState.Rank.BETA).build();
        BloodlineState wolfA = NONE.toBuilder().wolf(bornBeta).build();
        BloodlineState wolfLineage = NONE.toBuilder().wolf(bornBeta.toBuilder().lineage(WolfState.Lineage.BITTEN).build()).build();
        BloodlineState wolfRank = NONE.toBuilder().wolf(bornBeta.toBuilder().rank(WolfState.Rank.ALPHA).build()).build();
        BloodlineState wolfSkills = NONE.toBuilder().wolf(bornBeta.toBuilder().grantedSkills(Set.of("x")).build()).build();

        BloodlineAccess.notifyListeners(listeners, null, id, NONE, wolfA);
        BloodlineAccess.notifyListeners(listeners, null, id, wolfA, wolfLineage);
        BloodlineAccess.notifyListeners(listeners, null, id, wolfA, wolfRank);
        assertEquals(4, seen.size());
        BloodlineAccess.notifyListeners(listeners, null, id, wolfA, wolfSkills);
        assertEquals(4, seen.size());

        // the hunter's mark scales with markKills, so that is a change too
        BloodlineState marked = NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(3).build();
        BloodlineAccess.notifyListeners(listeners, null, id, marked, marked.toBuilder().markKills(4).build());
        assertEquals(5, seen.size());

        // the sireline index follows the sire, so a sire gained or lost is a change too
        BloodlineState sired = WITCH.toBuilder().sire(UUID.randomUUID()).build();
        BloodlineAccess.notifyListeners(listeners, null, id, WITCH, sired);
        BloodlineAccess.notifyListeners(listeners, null, id, sired, WITCH);
        assertEquals(7, seen.size());
        BloodlineAccess.notifyListeners(listeners, null, id, sired, sired.toBuilder().thirst(1).build());
        assertEquals(7, seen.size());
    }

    @Test
    void killedInnocentAndPackAlphaChangesAreNotified() {
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = List.of((p, c) -> seen.add(c));
        UUID id = UUID.randomUUID();
        WolfState beta = WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BITTEN).rank(WolfState.Rank.BETA).build();
        BloodlineState base = NONE.toBuilder().wolf(beta).build();

        // the eye colour turns blue for good after an innocent kill
        BloodlineState guilty = NONE.toBuilder().wolf(beta.toBuilder().killedInnocent(true).build()).build();
        BloodlineAccess.notifyListeners(listeners, null, id, base, guilty);
        assertEquals(1, seen.size());
        assertEquals(base, seen.get(0).before());
        assertEquals(guilty, seen.get(0).after());
        BloodlineAccess.notifyListeners(listeners, null, id, guilty, guilty.toBuilder().wolf(guilty.wolf().toBuilder().killedInnocent(false).build()).build());
        assertEquals(2, seen.size());

        // the pack index follows the pack Alpha, joined, switched or left
        UUID alpha = UUID.randomUUID();
        BloodlineState inPack = NONE.toBuilder().wolf(beta.toBuilder().packAlpha(alpha).build()).build();
        BloodlineAccess.notifyListeners(listeners, null, id, base, inPack);
        BloodlineAccess.notifyListeners(listeners, null, id, inPack, NONE.toBuilder().wolf(beta.toBuilder().packAlpha(UUID.randomUUID()).build()).build());
        BloodlineAccess.notifyListeners(listeners, null, id, inPack, base);
        assertEquals(5, seen.size());

        // the same values again are not a change
        BloodlineAccess.notifyListeners(listeners, null, id, inPack, NONE.toBuilder().wolf(beta.toBuilder().packAlpha(alpha).build()).build());
        BloodlineAccess.notifyListeners(listeners, null, id, guilty, guilty);
        assertEquals(5, seen.size());
    }

    @Test
    void clientViewHidesTheGeneAndOthersSeeNoSire() {
        UUID sire = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        WolfState wolf = WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BORN).rank(WolfState.Rank.BETA)
                .latentGene(true).geneRolled(true).build();
        BloodlineState full = WITCH.toBuilder().sire(sire).pendingSire(pending).wolf(wolf).build();

        BloodlineState own = BloodlineAccess.clientView(full, true);
        assertFalse(own.wolf().latentGene());
        assertFalse(own.wolf().geneRolled());
        assertNull(own.pendingSire());
        assertEquals(sire, own.sire());
        assertEquals(WolfState.Lineage.BORN, own.wolf().lineage());
        assertEquals(WolfState.Rank.BETA, own.wolf().rank());
        assertEquals(Species.WITCH, own.species());

        BloodlineState other = BloodlineAccess.clientView(full, false);
        assertNull(other.sire());
        assertNull(other.pendingSire());
        assertFalse(other.wolf().latentGene());
        // the stored state is untouched
        assertTrue(full.wolf().latentGene());
        assertEquals(pending, full.pendingSire());
    }

    @Test
    void nestedChangeIsQueuedAndEveryListenerSeesTheFinalState() {
        UUID id = UUID.randomUUID();
        List<BloodlineAccess.BloodlineChange> l1 = new ArrayList<>();
        List<BloodlineAccess.BloodlineChange> l2 = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = new ArrayList<>();
        // L1 stands in for a part calling set() from its listener: A -> B, then B -> A
        listeners.add((p, c) -> {
            l1.add(c);
            if (c.after().species() == Species.WITCH) {
                BloodlineAccess.notifyListeners(listeners, p, id, c.after(), NONE);
            }
        });
        listeners.add((p, c) -> l2.add(c));

        BloodlineAccess.notifyListeners(listeners, null, id, NONE, WITCH);

        List<BloodlineAccess.BloodlineChange> expected = List.of(
                new BloodlineAccess.BloodlineChange(NONE, WITCH), new BloodlineAccess.BloodlineChange(WITCH, NONE));
        assertEquals(expected, l1);
        assertEquals(expected, l2);
        assertEquals(NONE, l2.get(l2.size() - 1).after());

        // the guard is released afterwards
        BloodlineAccess.notifyListeners(listeners, null, id, NONE, SIPHONER);
        assertEquals(3, l2.size());
    }

    @Test
    void permanentlyFlippingListenerStopsAtTheCap() {
        UUID id = UUID.randomUUID();
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = new ArrayList<>();
        listeners.add((p, c) -> {
            seen.add(c);
            BloodlineState flipped = c.after().species() == Species.WITCH ? NONE : WITCH;
            BloodlineAccess.notifyListeners(listeners, p, id, c.after(), flipped);
        });

        BloodlineAccess.notifyListeners(listeners, null, id, NONE, WITCH);

        assertEquals(1 + BloodlineAccess.MAX_DRAINED, seen.size());
        // and a later, unrelated change still notifies (queue and guard were cleaned up)
        int before = seen.size();
        BloodlineAccess.notifyListeners(List.of((p, c) -> seen.add(c)), null, id, NONE, SIPHONER);
        assertEquals(before + 1, seen.size());
    }

    @Test
    void throwingListenerDoesNotStopOthers() {
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = List.of(
                (p, c) -> { throw new IllegalStateException("expected by test"); },
                (p, c) -> seen.add(c));
        BloodlineAccess.notifyListeners(listeners, null, UUID.randomUUID(), NONE, WITCH);
        assertEquals(1, seen.size());
    }
}
