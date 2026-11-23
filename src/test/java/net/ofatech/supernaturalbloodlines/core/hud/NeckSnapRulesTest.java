package net.ofatech.supernaturalbloodlines.core.hud;

import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.AUGUSTINE;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.BOSS;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.HYBRID;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.MORTAL;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.ORIGINAL;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Kind.VAMPIRE;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Result.KILL;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Result.NOTHING;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Result.SNAP;
import static net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules.Result.STAGGER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class NeckSnapRulesTest {
    @Test
    void theFourVampiricKindsLieDownAtAnyHealth() {
        for (NeckSnapRules.Kind k : new NeckSnapRules.Kind[] {VAMPIRE, AUGUSTINE, HYBRID, ORIGINAL}) {
            assertEquals(SNAP, NeckSnapRules.decide(k, 1f, 10f), k + " low");
            assertEquals(SNAP, NeckSnapRules.decide(k, 20f, 10f), k + " full");
            assertEquals(SNAP, NeckSnapRules.decide(k, 100f, 10f), k + " very healthy");
        }
    }

    @Test
    void aMortalDiesWhenWeakAndStaggersWhenHealthy() {
        assertEquals(KILL, NeckSnapRules.decide(MORTAL, 10.0f, 10.0f));
        assertEquals(STAGGER, NeckSnapRules.decide(MORTAL, 10.01f, 10.0f));
        assertEquals(KILL, NeckSnapRules.decide(MORTAL, 1f, 10f));
        assertEquals(STAGGER, NeckSnapRules.decide(MORTAL, 20f, 10f));
    }

    @Test
    void aBossIsNeverMoved() {
        assertEquals(NOTHING, NeckSnapRules.decide(BOSS, 1f, 10f));
        assertEquals(NOTHING, NeckSnapRules.decide(BOSS, 400f, 10f));
    }

    @Test
    void kindsFollowTheShow() {
        assertEquals(BOSS, NeckSnapRules.kindOf(Species.ORIGINAL, true, false, false, true), "a boss is a boss first");
        assertEquals(ORIGINAL, NeckSnapRules.kindOf(Species.ORIGINAL, true, false, false, false));
        assertEquals(ORIGINAL, NeckSnapRules.kindOf(Species.ORIGINAL_HYBRID, false, false, false, false));
        assertEquals(HYBRID, NeckSnapRules.kindOf(Species.HYBRID, true, false, false, false), "a hybrid in the werewolf faction is still a hybrid");
        assertEquals(HYBRID, NeckSnapRules.kindOf(Species.TRIBRID, false, false, false, false));
        assertEquals(AUGUSTINE, NeckSnapRules.kindOf(Species.AUGUSTINE, true, false, false, false));
        assertEquals(VAMPIRE, NeckSnapRules.kindOf(Species.HERETIC, false, false, false, false));
        assertEquals(VAMPIRE, NeckSnapRules.kindOf(Species.NONE, true, false, false, false), "a plain vampire");
        assertEquals(VAMPIRE, NeckSnapRules.kindOf(Species.NONE, false, true, false, false), "a Ghoul");
        assertEquals(VAMPIRE, NeckSnapRules.kindOf(Species.NONE, false, false, true, false), "a player in the turning or the transition");
    }

    @Test
    void everyoneElseIsMortal() {
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.NONE, false, false, false, false), "a human");
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.WITCH, false, false, false, false));
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.SIPHONER, false, false, false, false));
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.HUNTER_OF_THE_FIVE, false, false, false, false));
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.NONE, false, false, false, false), "a werewolf: the wiki says they stay dead");
        assertEquals(MORTAL, NeckSnapRules.kindOf(Species.WITCH, false, false, false, false), "a witch-werewolf with no vampire layer dies at once");
    }

    @Test
    void theCooldownIsNeverShorterThanTheLongestSnap() {
        assertEquals(45, NeckSnapRules.cooldownSeconds(45, 10, 20, 30, 40));
        assertEquals(60, NeckSnapRules.cooldownSeconds(45, 60, 20, 30, 40));
        assertEquals(300, NeckSnapRules.cooldownSeconds(45, 10, 20, 300, 40));
        assertEquals(1800, NeckSnapRules.cooldownSeconds(1, 5, 5, 5, 1800));
        assertEquals(100, NeckSnapRules.cooldownSeconds(100, 100, 100, 100, 100));
    }

    @Test
    void theWakeTimeIsThePerKindConfigAndZeroForTheRest() {
        assertEquals(60 * 20, NeckSnapRules.ticks(VAMPIRE, 60, 61, 62, 63));
        assertEquals(61 * 20, NeckSnapRules.ticks(AUGUSTINE, 60, 61, 62, 63));
        assertEquals(62 * 20, NeckSnapRules.ticks(HYBRID, 60, 61, 62, 63));
        assertEquals(63 * 20, NeckSnapRules.ticks(ORIGINAL, 60, 61, 62, 63));
        assertEquals(0, NeckSnapRules.ticks(MORTAL, 60, 61, 62, 63));
        assertEquals(0, NeckSnapRules.ticks(BOSS, 60, 61, 62, 63));
    }
}
