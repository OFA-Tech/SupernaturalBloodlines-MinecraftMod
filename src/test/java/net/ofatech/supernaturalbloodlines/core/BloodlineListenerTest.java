package net.ofatech.supernaturalbloodlines.core;

import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodlineListenerTest {
    @Test
    void listenerNotifiedOnSpeciesChangeOnly() {
        List<BloodlineAccess.BloodlineChange> seen = new ArrayList<>();
        List<BiConsumer<Object, BloodlineAccess.BloodlineChange>> listeners = List.of((p, c) -> seen.add(c));

        BloodlineState none = BloodlineState.NONE;
        BloodlineState witch = none.toBuilder().species(Species.WITCH).build();
        BloodlineState witchThirsty = witch.toBuilder().thirst(3).build();

        BloodlineAccess.notifyListeners(listeners, null, none, witch);
        assertEquals(1, seen.size());
        assertEquals(none, seen.get(0).before());
        assertEquals(witch, seen.get(0).after());

        BloodlineAccess.notifyListeners(listeners, null, witch, witchThirsty);
        BloodlineAccess.notifyListeners(listeners, null, witch, witch);
        assertEquals(1, seen.size());
    }
}
