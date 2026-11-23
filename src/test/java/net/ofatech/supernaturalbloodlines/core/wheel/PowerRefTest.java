package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PowerRefTest {
    @Test
    void parseRoundTripsEveryKey() {
        for (PowerSource s : PowerSource.values()) {
            PowerRef ref = new PowerRef(s, ResourceLocation.parse("some_mod:power/deep"));
            assertEquals(Optional.of(ref), PowerRef.parse(ref.key()), s.name());
        }
    }

    @Test
    void keyHasTheSourceThenTheId() {
        assertEquals("tensura/tensura:coercion", new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:coercion")).key());
    }

    @Test
    void rejectsUnknownSourceEmptyAndOverlongKeys() {
        for (String bad : new String[] {"", "x/y", "tensura/", "tensura", "/tensura:a", "tensura/Not Valid", "tensura/" + "a".repeat(300)}) {
            assertTrue(PowerRef.parse(bad).isEmpty(), "[" + bad.length() + "] " + bad);
        }
        assertTrue(PowerRef.parse(null).isEmpty());
    }

    @Test
    void displayOrderIsTheSpecOrder() {
        assertEquals(java.util.List.of(PowerSource.BLOODLINE, PowerSource.VAMPIRISM, PowerSource.WEREWOLVES, PowerSource.TENSURA, PowerSource.INFINITY,
                PowerSource.GREAT_SAGE), PowerSource.displayOrder());
    }

    @Test
    void lockReasonOrdinalsAreTheWireValues() {
        assertEquals(0, LockReason.NONE.ordinal());
        assertEquals(1, LockReason.NOT_UNLOCKED.ordinal());
        assertEquals(2, LockReason.CANNOT_ACT.ordinal());
        assertEquals(3, LockReason.NEEDS_SLOT.ordinal());
        assertEquals(4, LockReason.REFUSED_NOW.ordinal());
    }
}
