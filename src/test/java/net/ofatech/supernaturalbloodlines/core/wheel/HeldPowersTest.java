package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class HeldPowersTest {
    private static final PowerRef A = new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:gravity_flight"));
    private static final PowerRef B = new PowerRef(PowerSource.VAMPIRISM, ResourceLocation.parse("vampirism:rage"));

    @Test
    void releaseIsSentOnceOnButtonUp() {
        HeldPowers h = new HeldPowers();
        h.press(A, 2);
        assertTrue(h.any() && h.holds(A));
        assertEquals(new HeldPowers.Held(A, 2), h.release(A));
        assertNull(h.release(A), "a second button-up sends nothing");
        assertFalse(h.any());
    }

    @Test
    void releaseIsSentWhenTheScreenClosesWhileHeld() {
        HeldPowers h = new HeldPowers();
        h.press(A, 0);
        h.press(B, 1);
        assertEquals(List.of(new HeldPowers.Held(A, 0), new HeldPowers.Held(B, 1)), h.releaseAll(), "every held power gets its release, in the order they were pressed");
        assertTrue(h.releaseAll().isEmpty(), "and only once");
    }

    @Test
    void aSecondPressOfTheSamePowerIsOneHold() {
        HeldPowers h = new HeldPowers();
        h.press(A, 0);
        h.press(A, 1);
        assertEquals(List.of(new HeldPowers.Held(A, 1)), h.releaseAll());
    }
}
