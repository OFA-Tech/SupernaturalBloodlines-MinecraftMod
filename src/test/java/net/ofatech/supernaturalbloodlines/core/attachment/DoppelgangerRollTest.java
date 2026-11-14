package net.ofatech.supernaturalbloodlines.core.attachment;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoppelgangerRollTest {
    @Test
    void rollIsBelowTheChanceNotEqual() {
        assertTrue(DoppelgangerRoll.of(0.0, 0.005).doppelganger());
        assertTrue(DoppelgangerRoll.of(0.0049, 0.005).doppelganger());
        assertFalse(DoppelgangerRoll.of(0.005, 0.005).doppelganger());
        assertFalse(DoppelgangerRoll.of(0.0, 0.0).doppelganger());
        assertTrue(DoppelgangerRoll.of(0.9999, 1.0).doppelganger());
    }

    @Test
    void anyRollCountsAsRolled() {
        assertTrue(DoppelgangerRoll.of(0.9, 0.0).rolled());
        assertTrue(DoppelgangerRoll.of(0.0, 1.0).rolled());
        assertFalse(DoppelgangerRoll.NONE.rolled());
    }

    @Test
    void roundTripsAndASavedRollIsNeverTheDefault() {
        for (DoppelgangerRoll r : new DoppelgangerRoll[]{DoppelgangerRoll.of(0, 1), DoppelgangerRoll.of(1, 0)}) {
            JsonElement json = DoppelgangerRoll.CODEC.encodeStart(JsonOps.INSTANCE, r).getOrThrow();
            assertEquals(r, DoppelgangerRoll.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            assertNotEquals(DoppelgangerRoll.NONE, r);
        }
    }
}
