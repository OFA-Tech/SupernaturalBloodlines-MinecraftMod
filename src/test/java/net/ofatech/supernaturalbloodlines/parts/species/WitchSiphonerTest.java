package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.species.witch.SiphonCooldown;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import org.junit.jupiter.api.Test;

class WitchSiphonerTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void cooldownBlocksUntilItRunsOut() {
        SiphonCooldown<String> c = new SiphonCooldown<>();
        assertTrue(c.tryUse(A, "pos", 100, 600));
        assertFalse(c.tryUse(A, "pos", 699, 600));
        assertTrue(c.tryUse(A, "pos", 700, 600));
    }

    @Test
    void cooldownIsPerPlayerAndPerKey() {
        SiphonCooldown<String> c = new SiphonCooldown<>();
        assertTrue(c.tryUse(A, "one", 0, 600));
        assertTrue(c.tryUse(A, "two", 0, 600));
        assertTrue(c.tryUse(B, "one", 0, 600));
    }

    @Test
    void cooldownForgetsExpiredKeysAndLeavingPlayers() {
        SiphonCooldown<String> c = new SiphonCooldown<>();
        c.tryUse(A, "one", 0, 600);
        c.tryUse(A, "two", 0, 600);
        c.tryUse(A, "three", 1000, 600); // prunes the two that ran out
        assertEquals(1, c.size(A));
        c.forget(A);
        assertEquals(0, c.size(A));
    }

    @Test
    void witchBonusMath() {
        assertEquals(48, WitchAltarBonus.scaledRange(32));
        assertEquals(750, WitchAltarBonus.scaledCost(1000));
        assertEquals(1, WitchAltarBonus.scaledCost(1)); // never free
        assertEquals(0, WitchAltarBonus.scaledCost(0));
    }

    @Test
    void siphonedPowerPaysFirst() {
        RiteCost.Split s = RiteCost.split(1000, false, 300);
        assertEquals(700, s.altarCost());
        assertEquals(300, s.siphoned());
    }

    @Test
    void siphonedPowerCannotPayMoreThanTheCost() {
        RiteCost.Split s = RiteCost.split(1000, false, 5000);
        assertEquals(0, s.altarCost());
        assertEquals(1000, s.siphoned());
    }

    @Test
    void witchDiscountComesBeforeSiphoning() {
        RiteCost.Split s = RiteCost.split(1000, true, 100);
        assertEquals(650, s.altarCost());
        assertEquals(100, s.siphoned());
    }

    @Test
    void nothingToPayChangesNothing() {
        RiteCost.Split s = RiteCost.split(0, true, 100);
        assertEquals(0, s.altarCost());
        assertEquals(0, s.siphoned());
    }
}
