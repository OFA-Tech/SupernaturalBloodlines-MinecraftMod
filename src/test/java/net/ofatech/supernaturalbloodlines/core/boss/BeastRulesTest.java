package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.BeastRules.Kind;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules.Target;
import org.junit.jupiter.api.Test;

class BeastRulesTest {
    @Test
    void theHostIsCalledByAnAlphaOrAHunterOfTheFiveInTheOverworldWhoIsNoSpectator() {
        assertTrue(BeastRules.hostTarget(new Target(true, false, true, false)));
        assertTrue(BeastRules.hostTarget(new Target(false, true, true, false)));
        assertFalse(BeastRules.hostTarget(new Target(false, false, true, false)), "a plain player calls nobody");
        assertFalse(BeastRules.hostTarget(new Target(true, true, false, false)), "not in the overworld");
        assertFalse(BeastRules.hostTarget(new Target(true, true, true, true)), "a spectator");
    }

    @Test
    void theHostAppearsOnlyOnAFullMoonNightWithTheRollUnderTheChanceAndNothingRolledTonight() {
        BeastPhase none = BeastPhase.NONE;
        assertTrue(BeastRules.hostAppears(none, 0, true, false, true, 0.25, 0.249));
        assertFalse(BeastRules.hostAppears(none, 0, true, false, true, 0.25, 0.25), "the roll must be under the chance");
        assertFalse(BeastRules.hostAppears(none, 0, false, false, true, 1.0, 0.0), "no full moon night");
        assertFalse(BeastRules.hostAppears(none, 0, true, true, true, 1.0, 0.0), "one try a night");
        assertFalse(BeastRules.hostAppears(none, 0, true, false, false, 1.0, 0.0), "nobody to call it");
        assertFalse(BeastRules.hostAppears(BeastPhase.HOST, 1, true, false, true, 1.0, 0.0), "there is a Host already");
        assertFalse(BeastRules.hostAppears(BeastPhase.KILLED, 1, true, false, true, 1.0, 0.0), "a finished story");
        assertTrue(BeastRules.hostAppears(BeastPhase.FAILED, 1, true, false, true, 1.0, 0.99), "a lost Host is made again once");
        assertFalse(BeastRules.hostAppears(BeastPhase.FAILED, 2, true, false, true, 1.0, 0.0));
        assertTrue(BeastRules.hostAppears(none, 0, true, false, true, 7.0, 0.999), "a chance above 1 is 1");
        assertFalse(BeastRules.hostAppears(none, 0, true, false, true, -1.0, 0.0), "a chance below 0 is 0");
        assertFalse(BeastRules.hostAppears(none, 0, true, false, true, 0.0, 0.0), "a chance of 0 never");
    }

    @Test
    void theRoarFrightensEveryoneButAlphasAndTheExempt() {
        assertTrue(BeastRules.roarAffects(Kind.PLAYER, false, false), "an Omega, a Beta or a plain player");
        assertFalse(BeastRules.roarAffects(Kind.PLAYER, true, false), "an Alpha or a True Alpha");
        assertTrue(BeastRules.roarAffects(Kind.PACK_MEMBER, false, false), "an NPC Beta panics");
        assertFalse(BeastRules.roarAffects(Kind.PACK_MEMBER, true, false), "an NPC Alpha does not");
        assertTrue(BeastRules.roarAffects(Kind.MOB, false, false), "a hostile mob that belongs to no pack flees");
        assertTrue(BeastRules.roarAffects(Kind.MOB, true, false), "rank is a wolf's thing");
        for (Kind k : Kind.values()) {
            assertFalse(BeastRules.roarAffects(k, false, true), k + " exempt");
        }
    }

    @Test
    void theRoarReachesTheRangeEdgeAndWaitsForItsCooldown() {
        assertTrue(BeastRules.roarInRange(16.0 * 16.0, 16.0));
        assertFalse(BeastRules.roarInRange(16.0 * 16.0 + 0.01, 16.0));
        assertTrue(BeastRules.roarInRange(0, 16.0));
        assertTrue(BeastRules.roarReady(100, 100));
        assertFalse(BeastRules.roarReady(99, 100));
        assertEquals(120, BeastRules.fearTicks(6));
        assertEquals(20, BeastRules.fearTicks(0), "at least a second");
        assertEquals(20 + 400, BeastRules.nextRoarAt(20, 20));
        assertEquals(1 + 20, BeastRules.nextRoarAt(1, 0), "a cooldown of 0 is a second");
        assertEquals(40, BeastRules.ROAR_WINDUP_TICKS);
    }

    @Test
    void shotsAreReducedAndNothingElseIs() {
        assertEquals(6.0F, BeastRules.damageTaken(true, 10F), 1e-6);
        assertEquals(10.0F, BeastRules.damageTaken(false, 10F), 1e-6);
        assertEquals(0.6F, BeastRules.RANGED_FACTOR);
    }

    @Test
    void itHealsTwoAtATimeUnlessAWolfsbaneStrainIsOnItAndNeverAboveFullOrFromTheDead() {
        assertEquals(2.0F, BeastRules.regen(false, 500F, 1350F));
        assertEquals(0F, BeastRules.regen(true, 500F, 1350F), "under any wolfsbane strain");
        assertEquals(1.0F, BeastRules.regen(false, 1349F, 1350F), "never above full");
        assertEquals(0F, BeastRules.regen(false, 1350F, 1350F));
        assertEquals(0F, BeastRules.regen(false, 0F, 1350F), "the dead do not heal");
    }

    @Test
    void aHearingDeviceStunsItForASecondAtMostAndAShorterStunStaysShort() {
        assertEquals(20, BeastRules.stunTicks(100));
        assertEquals(20, BeastRules.stunTicks(20));
        assertEquals(5, BeastRules.stunTicks(5));
    }

    @Test
    void aWolfsbanePulseTakesAHeartAtMostSoNorthernBlueNeverKillsIt() {
        assertEquals(1.0F, BeastRules.wolfsbaneDamage(Float.MAX_VALUE));
        assertEquals(1.0F, BeastRules.wolfsbaneDamage(1.0F));
        assertEquals(0.5F, BeastRules.wolfsbaneDamage(0.5F));
    }
}
