package net.ofatech.supernaturalbloodlines.core.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DamageProbeTest {
    @Test
    void itIsActiveOnlyInsideTheProbe() {
        assertFalse(DamageProbe.active());
        assertTrue(DamageProbe.run(DamageProbe::active));
        assertFalse(DamageProbe.active());
    }

    @Test
    void probesNest() {
        assertTrue(DamageProbe.run(() -> {
            DamageProbe.run(() -> null);
            return DamageProbe.active();
        }));
        assertFalse(DamageProbe.active());
    }

    @Test
    void anExceptionEndsTheProbe() {
        assertThrows(IllegalStateException.class, () -> DamageProbe.run(() -> {
            throw new IllegalStateException();
        }));
        assertFalse(DamageProbe.active());
    }

    @Test
    void itIsPerThread() {
        boolean[] other = new boolean[1];
        DamageProbe.run(() -> {
            Thread t = new Thread(() -> other[0] = DamageProbe.active());
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertEquals(false, other[0]);
    }
}
