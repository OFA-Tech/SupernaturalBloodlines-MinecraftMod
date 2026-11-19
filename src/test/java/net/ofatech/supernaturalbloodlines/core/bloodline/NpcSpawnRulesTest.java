package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules.Plan;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules.Tuning;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night;
import org.junit.jupiter.api.Test;

/** R143, row by row: when and how an NPC werewolf pack spawns. */
class NpcSpawnRulesTest {
    private static final Tuning T = new Tuning(0.35, 0.5, 0.7, 3, 5, 3);

    private static java.util.function.DoubleSupplier dice(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    @Test
    void aFullMoonAndAnEclipseRollAtTheFullChance() {
        assertEquals(1.0, NpcSpawnRules.nightFactor(Night.FULL_MOON, 0.5));
        assertEquals(1.0, NpcSpawnRules.nightFactor(Night.ECLIPSE, 0.5));
    }

    @Test
    void aBloodMoonRollsAtItsFactor() {
        assertEquals(0.5, NpcSpawnRules.nightFactor(Night.BLOOD_MOON, 0.5));
        assertEquals(0.0, NpcSpawnRules.nightFactor(Night.BLOOD_MOON, 0.0));
        assertEquals(1.0, NpcSpawnRules.nightFactor(Night.BLOOD_MOON, 7), "a factor above 1 is no more than a full moon");
        assertEquals(0.0, NpcSpawnRules.nightFactor(Night.BLOOD_MOON, -2));
    }

    @Test
    void anOrdinaryNightNeverSpawns() {
        assertEquals(0.0, NpcSpawnRules.nightFactor(Night.NONE, 0.5));
        assertEquals(Plan.NOTHING, NpcSpawnRules.plan(Night.NONE, 1, new Tuning(1, 1, 1, 3, 5, 3), 5, dice(0)));
    }

    @Test
    void aLowDiceRollMakesAPackWithAnAlpha() {
        Plan p = NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.1, 0.1, 0.0, 0.99));
        assertTrue(p.pack() && p.alpha() && !p.lone());
        assertEquals(3, p.wolves(), "the smallest size at the lowest dice");
    }

    @Test
    void theChanceIsTheLimit() {
        assertTrue(NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.3499, 0, 0, 1)).pack());
        assertFalse(NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.35, 0, 0, 1)).pack());
    }

    @Test
    void aBloodMoonHalvesTheChance() {
        assertTrue(NpcSpawnRules.plan(Night.BLOOD_MOON, 0.5, T, 5, dice(0.1, 0, 0, 1)).pack(), "0.1 < 0.175");
        assertFalse(NpcSpawnRules.plan(Night.BLOOD_MOON, 0.5, T, 5, dice(0.2, 0, 0, 1)).pack(), "0.2 < 0.35 but not < 0.175");
        assertTrue(NpcSpawnRules.plan(Night.FULL_MOON, 0.5, T, 5, dice(0.2, 0, 0, 1)).pack(), "the same roll on a full moon");
    }

    @Test
    void theLoneWolfHasItsOwnRoll() {
        Plan p = NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.99, 0, 0, 0.1));
        assertFalse(p.pack());
        assertTrue(p.lone());
        assertEquals(0, p.wolves());
        assertTrue(p.any());
    }

    @Test
    void theLoneWolfAlsoFollowsTheBloodMoonFactor() {
        assertTrue(NpcSpawnRules.plan(Night.BLOOD_MOON, 0.5, T, 5, dice(0.99, 0, 0, 0.2)).lone(), "0.2 < 0.25");
        assertFalse(NpcSpawnRules.plan(Night.BLOOD_MOON, 0.5, T, 5, dice(0.99, 0, 0, 0.3)).lone(), "0.3 is not");
    }

    @Test
    void theAlphaIsALaterRoll() {
        assertFalse(NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.1, 0.7, 0, 1)).alpha(), "0.7 is not below 0.7");
        assertTrue(NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.1, 0.69, 0, 1)).alpha());
        assertFalse(NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, dice(0.99, 0.0, 0, 1)).alpha(), "no pack, no Alpha");
    }

    @Test
    void theSizeSpansTheRange() {
        assertEquals(3, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 99, dice(0, 0, 0.0, 1)).wolves());
        assertEquals(4, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 99, dice(0, 0, 0.5, 1)).wolves());
        assertEquals(5, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 99, dice(0, 0, 0.999, 1)).wolves());
    }

    @Test
    void anAlphaLeadsOnlyAsManyAsItCan() {
        assertEquals(2, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 2, dice(0, 0, 0.999, 1)).wolves());
        assertEquals(1, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 0, dice(0, 0, 0.999, 1)).wolves(), "at least one");
        assertEquals(5, NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 2, dice(0, 0.99, 0.999, 1)).wolves(), "leaderless wolves have no such limit");
    }

    @Test
    void aSillyConfigIsRepaired() {
        Tuning odd = new Tuning(5, -1, 2, 0, -3, 3);
        Plan p = NpcSpawnRules.plan(Night.FULL_MOON, 1, odd, 9, dice(0.99, 0.99, 0.99, 0.0));
        assertTrue(p.pack(), "a chance above 1 is a sure thing");
        assertFalse(p.lone(), "a negative chance is none");
        assertEquals(1, p.wolves(), "min 1 and max at least min");
    }

    @Test
    void thePlanAlwaysDrawsFourNumbers() {
        int[] n = {0};
        NpcSpawnRules.plan(Night.NONE, 1, T, 5, () -> n[0]++);
        assertEquals(4, n[0]);
        n[0] = 0;
        NpcSpawnRules.plan(Night.FULL_MOON, 1, T, 5, () -> n[0]++ * 0.0 + 0.99);
        assertEquals(4, n[0]);
    }

    @Test
    void aPlayerIsRolledOncePerNight() {
        assertTrue(NpcSpawnRules.mayRoll(-1, 5, 0, 3));
        assertTrue(NpcSpawnRules.mayRoll(4, 5, 0, 3));
        assertFalse(NpcSpawnRules.mayRoll(5, 5, 0, 3));
    }

    @Test
    void aLevelHasAMostPerNight() {
        assertTrue(NpcSpawnRules.mayRoll(-1, 5, 2, 3));
        assertFalse(NpcSpawnRules.mayRoll(-1, 5, 3, 3));
        assertFalse(NpcSpawnRules.mayRoll(-1, 5, 0, 0), "0 packs a night is off");
    }

    @Test
    void theNightIsTheDayNumber() {
        assertEquals(0, NpcSpawnRules.nightOf(0));
        assertEquals(0, NpcSpawnRules.nightOf(23999));
        assertEquals(1, NpcSpawnRules.nightOf(24000));
        assertEquals(24, NpcSpawnRules.nightOf(24000L * 24 + 13000));
    }

    @Test
    void feralNeedsAFullMoonNightAndTheDark() {
        assertTrue(NpcSpawnRules.feral(true, 0));
        assertTrue(NpcSpawnRules.feral(true, 7));
        assertFalse(NpcSpawnRules.feral(true, 8), "a lit spot");
        assertFalse(NpcSpawnRules.feral(false, 0), "no full moon");
    }
}