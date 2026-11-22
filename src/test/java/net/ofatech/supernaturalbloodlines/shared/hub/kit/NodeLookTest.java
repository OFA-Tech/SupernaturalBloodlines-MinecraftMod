package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.NodeLook.Kind;
import org.junit.jupiter.api.Test;

class NodeLookTest {
    @Test
    void eachStateSaysOneThingWhateverTheSizeOfTheNode() {
        NodeLook.Look owned = NodeLook.of(Kind.OWNED, false);
        assertTrue(owned.filled());
        assertFalse(owned.goldRing() || owned.tickRing() || owned.dimIcon());
        NodeLook.Look open = NodeLook.of(Kind.AVAILABLE, false);
        assertTrue(open.goldRing());
        assertFalse(open.filled() || open.tickRing());
        NodeLook.Look locked = NodeLook.of(Kind.LOCKED, false);
        assertTrue(locked.tickRing() && locked.dimIcon());
        assertFalse(locked.filled() || locked.goldRing());
    }

    @Test
    void theChosenNodeHasAHaloWhateverItsState() {
        for (Kind k : Kind.values()) {
            assertTrue(NodeLook.of(k, true).halo(), k.toString());
            assertFalse(NodeLook.of(k, false).halo(), k.toString());
            assertEquals(NodeLook.of(k, false).filled(), NodeLook.of(k, true).filled(), "the halo does not change what it says");
        }
    }
}