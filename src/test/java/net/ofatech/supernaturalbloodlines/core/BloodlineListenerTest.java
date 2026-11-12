package net.ofatech.supernaturalbloodlines.core;

import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodlineListenerTest {
    private static final BloodlineState NONE = BloodlineState.NONE;
    private static final BloodlineState WITCH = NONE.toBuilder().species(Species.WITCH).build();
    private static final BloodlineState SIPHONER = NONE.toBuilder().species(Species.SIPHONER).build();

    @Test
    void listenerNotifiedOnSpeciesChangeOnly() {
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
