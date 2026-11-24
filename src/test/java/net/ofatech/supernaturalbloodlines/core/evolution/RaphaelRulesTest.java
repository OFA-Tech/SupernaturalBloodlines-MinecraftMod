package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.evolution.RaphaelRules.Facts;
import net.ofatech.supernaturalbloodlines.core.evolution.RaphaelRules.Sweep;
import net.ofatech.supernaturalbloodlines.core.evolution.RaphaelRules.Verdict;
import org.junit.jupiter.api.Test;

class RaphaelRulesTest {
    @Test
    void withoutTheOtherAddonEverythingPassesThrough() {
        for (boolean held : new boolean[] {false, true}) {
            for (boolean owned : new boolean[] {false, true}) {
                assertEquals(Verdict.PASS, RaphaelRules.ofLearn(new Facts(false, held, owned)), "held " + held + " owned " + owned);
            }
        }
    }

    @Test
    void holdingEitherRaphaelIsAlreadyHeldWhateverTheOwnerSays() {
        assertEquals(Verdict.ALREADY_HELD, RaphaelRules.ofLearn(new Facts(true, true, false)));
        assertEquals(Verdict.ALREADY_HELD, RaphaelRules.ofLearn(new Facts(true, true, true)));
    }

    @Test
    void anOwnerElsewhereRefuses() {
        assertEquals(Verdict.REFUSE, RaphaelRules.ofLearn(new Facts(true, false, true)));
    }

    @Test
    void aFreeRaphaelIsRedirected() {
        assertEquals(Verdict.REDIRECT, RaphaelRules.ofLearn(new Facts(true, false, false)));
    }

    @Test
    void nothingToConvertWhenTheSourceIsNotHeld() {
        for (boolean target : new boolean[] {false, true}) {
            for (boolean owned : new boolean[] {false, true}) {
                assertEquals(Sweep.NOTHING, RaphaelRules.onLogin(true, false, target, owned));
            }
        }
    }

    @Test
    void withoutTheOtherAddonTheLoginLeavesEverythingAlone() {
        assertEquals(Sweep.NOTHING, RaphaelRules.onLogin(false, true, false, false));
        assertEquals(Sweep.NOTHING, RaphaelRules.onLogin(false, true, true, false));
    }

    @Test
    void bothHeldDropsTheSourceWhoeverOwnsIt() {
        assertEquals(Sweep.DROP_SOURCE, RaphaelRules.onLogin(true, true, true, false));
        assertEquals(Sweep.DROP_SOURCE, RaphaelRules.onLogin(true, true, true, true));
    }

    @Test
    void aSourceWhoseTargetIsOwnedElsewhereIsLeftAlone() {
        assertEquals(Sweep.LEAVE, RaphaelRules.onLogin(true, true, false, true));
    }

    @Test
    void aFreeTargetIsConverted() {
        assertEquals(Sweep.CONVERT, RaphaelRules.onLogin(true, true, false, false));
    }
}
