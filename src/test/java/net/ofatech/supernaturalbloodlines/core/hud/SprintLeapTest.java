package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SprintLeapTest {
    /** setting, jumpPressed, sprinting, canJump, werewolf, transformed, unlocked, onCooldown, active, screenOpen, hybridLeapInFlight */
    private static boolean leap(boolean... f) {
        return SprintLeap.shouldLeap(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10]);
    }

    private static final boolean[] OK = {true, true, true, true, true, true, true, false, false, false, false};

    @Test
    void aTransformedWerewolfJumpingWhileSprintingLeaps() {
        assertTrue(leap(OK));
    }

    @Test
    void eachConditionAloneStopsIt() {
        String[] names = {"setting off", "no jump press", "not sprinting", "cannot jump", "not a werewolf", "not transformed", "not unlocked", "on cooldown", "already active", "screen open", "a Hybrid Leap is in flight"};
        for (int i = 0; i < OK.length; i++) {
            boolean[] f = OK.clone();
            f[i] = !f[i];
            assertFalse(leap(f), names[i]);
        }
    }

    @Test
    void aHybridLeapKeepsTheSprintLeapOffForTwentyTicksOnly() {
        long until = SprintLeap.hybridGuardUntil(1000);
        assertEquals(1020, until);
        assertTrue(SprintLeap.hybridInFlight(1000, until));
        assertTrue(SprintLeap.hybridInFlight(1019, until));
        assertFalse(SprintLeap.hybridInFlight(1020, until));
        assertFalse(SprintLeap.hybridInFlight(5, 0), "no Hybrid Leap was ever thrown");
        boolean[] f = OK.clone();
        f[10] = true;
        assertFalse(leap(f), "the one guard: a jump never arms the sprint-leap during a Hybrid Leap");
        assertTrue(leap(OK), "and every other combination is unchanged");
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
        assertEquals(3 + 3 + 1 + 9 + 1 + 1 + 1, ids.size(), "3 Tensura + 3 Tensura Slots + suck + 9 actions + bite + our Roar key + leap");
        assertTrue(ids.contains("key.supernaturalbloodlines.alpha_roar"), "the Roar key is hidden while the HUD gives the Roar a slot");
    }

    @Test
    void theLeapKeyIsCoveredOnlyWhileTheSprintLeapReplacesIt() {
        assertTrue(CoveredKeyIds.all(true).contains(CoveredKeyIds.WEREWOLVES_LEAP));
        assertFalse(CoveredKeyIds.all(false).contains(CoveredKeyIds.WEREWOLVES_LEAP), "sprint_leap off: the key must work again");
        assertTrue(CoveredKeyIds.all(false).contains(CoveredKeyIds.WEREWOLVES_BITE), "the other keys stay covered");
        // and the plan that runs when the list changes gives a recorded leap bind back, even while covering
        var plan = CoveredKeys.plan(java.util.Map.of(CoveredKeyIds.WEREWOLVES_LEAP, CoveredKeys.UNBOUND), java.util.Map.of(CoveredKeyIds.WEREWOLVES_LEAP, "key.keyboard.left.control"),
                CoveredKeyIds.all(false), java.util.Map.of(CoveredKeyIds.WEREWOLVES_LEAP, "key.keyboard.left.control"), true);
        assertEquals("key.keyboard.left.control", plan.set().get(CoveredKeyIds.WEREWOLVES_LEAP));
    }

    @Test
    void aJumpThatNeverStartsDisarmsTheLeapButOneThatDoesDoesNot() {
        assertFalse(SprintLeap.jumpBlocked(0, false));
        assertFalse(SprintLeap.jumpBlocked(SprintLeap.JUMP_WINDOW - 1, false), "still inside the window");
        assertTrue(SprintLeap.jumpBlocked(SprintLeap.JUMP_WINDOW, false), "never left the ground: a ceiling, a trapdoor, water");
        assertFalse(SprintLeap.jumpBlocked(SprintLeap.JUMP_WINDOW + 5, true), "left the ground: the jump happened");
    }
}
