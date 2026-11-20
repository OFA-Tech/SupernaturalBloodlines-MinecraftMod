package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SprintLeapTest {
    /** setting, jumpPressed, sprinting, canJump, werewolf, transformed, unlocked, onCooldown, active, screenOpen */
    private static boolean leap(boolean... f) {
        return SprintLeap.shouldLeap(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9]);
    }

    private static final boolean[] OK = {true, true, true, true, true, true, true, false, false, false};

    @Test
    void aTransformedWerewolfJumpingWhileSprintingLeaps() {
        assertTrue(leap(OK));
    }

    @Test
    void eachConditionAloneStopsIt() {
        String[] names = {"setting off", "no jump press", "not sprinting", "cannot jump", "not a werewolf", "not transformed", "not unlocked", "on cooldown", "already active", "screen open"};
        for (int i = 0; i < OK.length; i++) {
            boolean[] f = OK.clone();
            f[i] = !f[i];
            assertFalse(leap(f), names[i]);
        }
    }

    @Test
    void anActiveLeapIsNeverToggledOffByASecondJump() {
        boolean[] f = OK.clone();
        f[8] = true;
        assertFalse(leap(f));
    }

    @Test
    void werewolvesLeapKeyIsCoveredAndTheBiteKeyStillIs() {
        List<String> ids = CoveredKeyIds.all();
        assertTrue(ids.contains("keys.werewolves.leap"), "Left Control is vanilla sprint: the leap key must go");
        assertTrue(ids.contains("keys.werewolves.bite"));
        assertEquals(3 + 3 + 1 + 9 + 1 + 1, ids.size(), "3 Tensura + 3 Tensura Slots + suck + 9 actions + bite + leap");
    }
}
