package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CompulsionTest {
    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 EAST = new Vec3(3, 0, 0);

    @Test
    void lookWithinTheConeCounts() {
        assertTrue(Compulsion.looksAt(new Vec3(1, 0, 0), ORIGIN, EAST, 15));
        // 10 degrees off
        assertTrue(Compulsion.looksAt(new Vec3(Math.cos(Math.toRadians(10)), 0, Math.sin(Math.toRadians(10))), ORIGIN, EAST, 15));
    }

    @Test
    void lookOutsideTheConeDoesNot() {
        assertFalse(Compulsion.looksAt(new Vec3(Math.cos(Math.toRadians(20)), 0, Math.sin(Math.toRadians(20))), ORIGIN, EAST, 15));
        assertTrue(Compulsion.looksAt(new Vec3(Math.cos(Math.toRadians(20)), 0, Math.sin(Math.toRadians(20))), ORIGIN, EAST, 30));
        assertFalse(Compulsion.looksAt(new Vec3(-1, 0, 0), ORIGIN, EAST, 30));
        assertFalse(Compulsion.looksAt(new Vec3(0, 1, 0), ORIGIN, EAST, 30));
    }

    @Test
    void standingInsideEachOtherNeverCounts() {
        assertFalse(Compulsion.looksAt(new Vec3(1, 0, 0), ORIGIN, ORIGIN, 30));
    }
}
