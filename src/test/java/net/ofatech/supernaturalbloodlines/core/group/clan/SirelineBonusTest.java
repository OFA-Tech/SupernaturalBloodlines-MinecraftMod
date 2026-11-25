package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SirelineBonusTest {
    private static UUID id(int n) {
        return UUID.fromString("00000000-0000-0000-0000-0000000000" + String.format("%02d", n));
    }

    private final Map<UUID, UUID> sires = new HashMap<>();
    private final Function<UUID, Optional<UUID>> sireOf = c -> Optional.ofNullable(sires.get(c));

    @Test
    void threePercentEach() {
        assertEquals(0.03, SirelineBonus.of(1, 3, 15), 1e-9);
        assertEquals(0.09, SirelineBonus.of(3, 3, 15), 1e-9);
    }

    @Test
    void capsAtFifteen() {
        assertEquals(0.15, SirelineBonus.of(5, 3, 15), 1e-9);
        assertEquals(0.15, SirelineBonus.of(6, 3, 15), 1e-9);
        assertEquals(0.15, SirelineBonus.of(60, 3, 15), 1e-9);
    }

    @Test
    void zeroMatesIsNoBonus() {
        assertEquals(0.0, SirelineBonus.of(0, 3, 15), 1e-9);
        assertEquals(0.0, SirelineBonus.of(-2, 3, 15), 1e-9);
    }

    @Test
    void percentZeroIsOff() {
        assertEquals(0.0, SirelineBonus.of(9, 0, 15), 1e-9);
    }

    @Test
    void theRootWalkFindsTheTop() {
        sires.put(id(2), id(1));
        sires.put(id(3), id(2));
        assertEquals(id(1), SirelineBonus.rootOf(id(3), sireOf, 64));
        assertEquals(id(1), SirelineBonus.rootOf(id(1), sireOf, 64));
    }

    @Test
    void aCycleStops() {
        sires.put(id(1), id(2));
        sires.put(id(2), id(1));
        int[] calls = {0};
        UUID root = SirelineBonus.rootOf(id(1), c -> {
            calls[0]++;
            return sireOf.apply(c);
        }, 64);
        assertTrue(root.equals(id(1)) || root.equals(id(2)));
        assertTrue(calls[0] <= 3, "the walk went round the cycle " + calls[0] + " times");
    }

    @Test
    void aDeepLineStopsAtTheDepthCap() {
        for (int i = 2; i <= 30; i++) {
            sires.put(id(i), id(i - 1));
        }
        assertEquals(id(30 - 5), SirelineBonus.rootOf(id(30), sireOf, 5));
        assertEquals(id(1), SirelineBonus.rootOf(id(30), sireOf, 64));
    }

    @Test
    void unsiredPlayersShareNothing() {
        assertEquals(id(4), SirelineBonus.rootOf(id(4), sireOf, 64));
        assertFalse(SirelineBonus.sameLine(id(4), id(5), sireOf, 64));
    }

    @Test
    void sameRootIsTheSameLine() {
        sires.put(id(2), id(1));
        sires.put(id(3), id(1));
        sires.put(id(4), id(2));
        assertTrue(SirelineBonus.sameLine(id(3), id(4), sireOf, 64), "an aunt and a niece share the root");
        assertTrue(SirelineBonus.sameLine(id(2), id(3), sireOf, 64), "siblings");
        assertTrue(SirelineBonus.sameLine(id(1), id(4), sireOf, 64), "a root and a grandchild");
    }

    @Test
    void differentRootsAreNot() {
        sires.put(id(2), id(1));
        sires.put(id(6), id(5));
        assertFalse(SirelineBonus.sameLine(id(2), id(6), sireOf, 64));
    }

    @Test
    void aCreatureRootIsAnId() {
        UUID creature = id(77);
        sires.put(id(2), creature);
        sires.put(id(3), creature);
        assertTrue(SirelineBonus.sameLine(id(2), id(3), sireOf, 64));
        assertEquals(creature, SirelineBonus.rootOf(id(2), sireOf, 64));
    }
}
