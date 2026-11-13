package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class FiveRosterTest {
    @Test
    void countsEachMemberOnce() {
        FiveRoster r = new FiveRoster();
        UUID a = UUID.randomUUID();
        assertTrue(r.add(a));
        assertFalse(r.add(a));
        r.add(UUID.randomUUID());
        assertEquals(2, r.size());
        assertTrue(r.contains(a));
        r.remove(a);
        r.remove(a);
        assertEquals(1, r.size());
        assertFalse(r.contains(a));
    }

    @Test
    void survivesSaveAndLoad() {
        FiveRoster r = new FiveRoster();
        UUID member = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        UUID lifted = UUID.randomUUID();
        r.add(member);
        r.setPendingPotentialChoice(pending, true);
        r.setLiftedCurse(lifted, true);
        FiveRoster back = FiveRoster.load(r.save(new CompoundTag(), null), null);
        assertTrue(back.contains(member));
        assertEquals(1, back.size());
        assertTrue(back.pendingPotentialChoice().contains(pending));
        assertTrue(back.liftedCurses().contains(lifted));
        back.setPendingPotentialChoice(pending, false);
        assertTrue(back.pendingPotentialChoice().isEmpty());
    }
}
