package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlphaRoarRulesTest {
    @Test
    void onlyAnAliveAwakeNonSpectatorRoars() {
        assertTrue(AlphaRoar.mayAct(true, false, false));
        assertFalse(AlphaRoar.mayAct(false, false, false), "dead");
        assertFalse(AlphaRoar.mayAct(true, true, false), "spectating");
        assertFalse(AlphaRoar.mayAct(true, false, true), "asleep: the key's payload must not roar a sleeper");
    }
}
