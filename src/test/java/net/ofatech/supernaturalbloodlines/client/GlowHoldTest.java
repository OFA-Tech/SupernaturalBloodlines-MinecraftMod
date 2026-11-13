package net.ofatech.supernaturalbloodlines.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GlowHoldTest {
    @Test
    void holdsTheGlowThenRestoresAnEntityThatDidNotGlow() {
        GlowHold h = new GlowHold();
        h.hold(List.of(7), 100);
        assertTrue(h.frame(7, false, 60));
        assertTrue(h.frame(7, true, 99)); // our own write read back
        assertFalse(h.frame(7, true, 100));
        assertTrue(h.isEmpty());
    }

    @Test
    void anEntityThatReallyGlowsStillGlowsAfterwards() {
        GlowHold h = new GlowHold();
        h.hold(List.of(7), 100);
        assertTrue(h.frame(7, true, 60));
        assertTrue(h.frame(7, true, 99));
        assertTrue(h.frame(7, true, 100));
        assertTrue(h.isEmpty());
    }

    @Test
    void aGlowTheServerClearedDuringTheHoldIsNotBroughtBack() {
        GlowHold h = new GlowHold();
        h.hold(List.of(7), 100);
        assertTrue(h.frame(7, true, 60));
        assertTrue(h.frame(7, false, 70)); // the server wrote false over ours: the entity does not glow
        assertFalse(h.frame(7, true, 100));
    }

    @Test
    void aRefreshKeepsTheBaseAndExtendsTheHold() {
        GlowHold h = new GlowHold();
        h.hold(List.of(7), 100);
        assertTrue(h.frame(7, false, 60));
        h.hold(List.of(7), 150);
        assertTrue(h.frame(7, true, 100)); // not expired any more, and our own write is not read again as the base
        assertTrue(h.frame(7, true, 149));
        assertFalse(h.frame(7, true, 150));
    }

    @Test
    void anEntityThatIsNotHeldIsLeftAlone() {
        GlowHold h = new GlowHold();
        assertTrue(h.frame(3, true, 0));
        assertFalse(h.frame(3, false, 0));
    }

    @Test
    void anAbsentEntityIsForgottenOnlyWhenItExpires() {
        GlowHold h = new GlowHold();
        h.hold(List.of(7, 8), 100);
        h.missing(7, 99);
        assertEquals(2, h.ids().size());
        h.missing(7, 100);
        assertEquals(List.of(8), h.ids());
        h.clear();
        assertTrue(h.isEmpty());
    }
}
