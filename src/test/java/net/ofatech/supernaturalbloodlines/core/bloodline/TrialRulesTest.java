package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialRules.Fail;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialRules.Refusal;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

/** R147, row by row: who may start the Alpha Trial and what its fight counts. */
class TrialRulesTest {
    private static WolfState wolf(Rank rank) {
        return WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).build();
    }

    /** Everything right: a Beta, level 12 of 10, a full-moon night, transformed, nothing running, the warning seen. */
    private static Refusal ok(WolfState w) {
        return TrialRules.gate(true, w, true, 12, 10, true, true, false, false, false);
    }

    @Test
    void aBetaAndAnOmegaMayStart() {
        assertEquals(Refusal.NONE, ok(wolf(Rank.BETA)));
        assertEquals(Refusal.NONE, ok(wolf(Rank.OMEGA)));
    }

    @Test
    void theSwitchComesFirst() {
        assertEquals(Refusal.OFF, TrialRules.gate(false, wolf(Rank.BETA), true, 12, 10, true, true, false, false, false));
    }

    @Test
    void aNonWerewolfIsRefused() {
        assertEquals(Refusal.NOT_WEREWOLF, TrialRules.gate(true, wolf(Rank.BETA), false, 12, 10, true, true, false, false, false), "not in the faction");
        assertEquals(Refusal.NOT_WEREWOLF, TrialRules.gate(true, WolfState.NONE, true, 12, 10, true, true, false, false, false), "no lineage");
    }

    @Test
    void anAlphaAndATrueAlphaAreRefused() {
        assertEquals(Refusal.RANK, ok(wolf(Rank.ALPHA)));
        assertEquals(Refusal.RANK, ok(wolf(Rank.TRUE_ALPHA)));
        assertEquals(Refusal.RANK, ok(wolf(Rank.NONE)));
    }

    @Test
    void theLevelIsAMinimum() {
        assertEquals(Refusal.LEVEL, TrialRules.gate(true, wolf(Rank.BETA), true, 9, 10, true, true, false, false, false));
        assertEquals(Refusal.NONE, TrialRules.gate(true, wolf(Rank.BETA), true, 10, 10, true, true, false, false, false));
    }

    @Test
    void itNeedsAFullMoonNightAndTheBeastForm() {
        assertEquals(Refusal.NOT_NIGHT, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, false, true, false, false, false));
        assertEquals(Refusal.NOT_TRANSFORMED, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, false, false, false, false));
    }

    @Test
    void oneTrialAtATimePerPlayer() {
        assertEquals(Refusal.ALREADY, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, true, true, false, false));
    }

    @Test
    void anOpenPathIsRefusedUntilTheWarningWasAnswered() {
        assertEquals(Refusal.NEEDS_WARNING, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, true, false, true, false));
        assertEquals(Refusal.NONE, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, true, false, true, true));
        assertEquals(Refusal.NONE, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, true, false, false, false), "a closed path needs no warning");
    }

    @Test
    void theOrderOfTheRefusalsPutsTheWarningLast() {
        // everything wrong at once: the first reason wins, and the warning is only asked of someone who could otherwise start
        assertEquals(Refusal.LEVEL, TrialRules.gate(true, wolf(Rank.BETA), true, 1, 10, false, false, true, true, false));
        assertEquals(Refusal.NOT_NIGHT, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, false, false, true, true, false));
        assertEquals(Refusal.NOT_TRANSFORMED, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, false, true, true, false));
        assertEquals(Refusal.ALREADY, TrialRules.gate(true, wolf(Rank.BETA), true, 12, 10, true, true, true, true, false));
    }

    @Test
    void thePhasesAreThirds() {
        assertEquals(0, TrialRules.phase(100, 100));
        assertEquals(0, TrialRules.phase(67, 100));
        assertEquals(1, TrialRules.phase(66, 100));
        assertEquals(1, TrialRules.phase(34, 100));
        assertEquals(2, TrialRules.phase(33, 100));
        assertEquals(2, TrialRules.phase(1, 100));
        assertEquals(2, TrialRules.phase(5, 0), "no maximum: the last phase");
    }

    @Test
    void twoBeastsComeForEachPhasePassed() {
        assertEquals(0, TrialRules.addsFor(0, 100, 100));
        assertEquals(2, TrialRules.addsFor(0, 60, 100));
        assertEquals(0, TrialRules.addsFor(1, 60, 100), "the same phase again");
        assertEquals(2, TrialRules.addsFor(1, 30, 100));
        assertEquals(4, TrialRules.addsFor(0, 30, 100), "a big hit that skips a phase brings both");
        assertEquals(0, TrialRules.addsFor(2, 90, 100), "health that went back up calls nobody");
    }

    @Test
    void theLeashCountsAwayTimeAndStartsAgainOnReturn() {
        assertEquals(10, TrialRules.away(0, false, 10));
        assertEquals(30, TrialRules.away(20, false, 10));
        assertEquals(0, TrialRules.away(500, true, 10));
    }

    @Test
    void theTrialIsLostForEachReasonInItsOrder() {
        assertEquals(Fail.NONE, TrialRules.failure(true, true, 0, false));
        assertEquals(Fail.CHALLENGER_DEAD, TrialRules.failure(false, true, 0, false));
        assertEquals(Fail.OTHER_DIMENSION, TrialRules.failure(true, false, 0, false));
        assertEquals(Fail.NIGHT_OVER, TrialRules.failure(true, true, 0, true));
        assertEquals(Fail.NONE, TrialRules.failure(true, true, TrialRules.LEASH_TICKS - 1, false));
        assertEquals(Fail.TOO_FAR, TrialRules.failure(true, true, TrialRules.LEASH_TICKS, false));
        assertEquals(Fail.CHALLENGER_DEAD, TrialRules.failure(false, false, 9999, true), "dead beats everything");
    }

    @Test
    void theWarningCannotBeShownToAClientWithoutTheChannel() {
        assertTrue(TrialRules.canShowWarning(true, true));
        assertFalse(TrialRules.canShowWarning(true, false), "a connected player whose game does not know the packet");
        assertTrue(TrialRules.canShowWarning(false, false), "no connection at all is a test player, let through");
    }

    @Test
    void onlyTheChallengerAndTheirSideMayHurtTheBoss() {
        UUID me = UUID.randomUUID();
        assertTrue(TrialRules.mayHurt(me, me, false));
        assertTrue(TrialRules.mayHurt(UUID.randomUUID(), me, true), "a pack mate or an ally");
        assertFalse(TrialRules.mayHurt(UUID.randomUUID(), me, false), "a stranger");
        assertTrue(TrialRules.mayHurt(null, me, false), "the world is no stranger");
    }

    @Test
    void homeSaysRunningPossibleOrNothing() {
        assertEquals(TrialRules.Notice.RUNNING, TrialRules.notice(() -> true, Refusal.ALREADY), "a running trial");
        assertEquals(TrialRules.Notice.RUNNING, TrialRules.notice(() -> true, Refusal.NONE));
        assertEquals(TrialRules.Notice.POSSIBLE, TrialRules.notice(() -> false, Refusal.NONE));
        assertEquals(TrialRules.Notice.POSSIBLE, TrialRules.notice(() -> false, Refusal.NEEDS_WARNING), "the warning is a step, not a reason to say nothing");
        for (Refusal no : new Refusal[] {Refusal.OFF, Refusal.NOT_WEREWOLF, Refusal.RANK, Refusal.LEVEL, Refusal.NOT_NIGHT}) {
            assertEquals(TrialRules.Notice.NONE, TrialRules.notice(() -> false, no), no.name());
        }
    }

    @Test
    void aNonWerewolfIsAnsweredBeforeTheRunningLookup() {
        assertEquals(TrialRules.Notice.NONE, TrialRules.notice(() -> {
            throw new AssertionError("asked whether a trial runs for someone who is not a werewolf");
        }, Refusal.NOT_WEREWOLF));
    }

    /** The Home notice goes over the wire as the ordinal of the enum: the two lists of names must stay in step. */
    @Test
    void theNoticeOrdinalsAreTheFlagsConstants() {
        assertEquals(net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.TRIAL_NONE, TrialRules.Notice.NONE.ordinal());
        assertEquals(net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.TRIAL_POSSIBLE, TrialRules.Notice.POSSIBLE.ordinal());
        assertEquals(net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.TRIAL_RUNNING, TrialRules.Notice.RUNNING.ordinal());
        assertEquals(3, TrialRules.Notice.values().length);
    }
}
