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
    void listenerThatFlipsSpeciesDoesNotRecurse() {
        UUID id = UUID.randomUUID();
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = new ArrayList<>();
        // stands in for a part calling set() from its listener: A -> B -> A
        listeners.add((p, c) -> {
            seen.add(c);
            BloodlineState flipped = c.after().species() == Species.WITCH ? NONE : WITCH;
            BloodlineAccess.notifyListeners(listeners, p, id, c.after(), flipped);
        });
        BloodlineAccess.notifyListeners(listeners, null, id, NONE, WITCH);
        assertEquals(1, seen.size());

        // the guard is released afterwards
        BloodlineAccess.notifyListeners(listeners, null, id, WITCH, SIPHONER);
        assertEquals(2, seen.size());
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
