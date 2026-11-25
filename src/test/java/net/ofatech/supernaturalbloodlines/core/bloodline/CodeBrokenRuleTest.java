package net.ofatech.supernaturalbloodlines.core.bloodline;

import static net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent.CODE_BROKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeBrokenRuleTest {
    private static BloodlineState hof(int kills) {
        return BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(kills).lastVampireKill(77).build();
    }

    private static Outcome run(BloodlineState s, int percent) {
        return BloodlineRules.apply(s, CODE_BROKEN, BloodlineContext.defaults().withCodePenaltyPercent(percent));
    }

    @Test
    void codeBrokenCostsAHunterOfTheFiveAQuarter() {
        int[][] rows = {{20, 15}, {12, 9}, {4, 3}, {3, 2}, {1, 0}};
        for (int[] r : rows) {
            assertEquals(r[1], run(hof(r[0]), 25).state().markKills(), r[0] + " kills");
        }
    }

    @Test
    void aHunterOfTheFiveAtZeroStaysAtZero() {
        Outcome o = run(hof(0), 25);
        assertEquals(0, o.state().markKills());
        assertFalse(o.refused());
    }

    @Test
    void codeBrokenChangesNothingForAnyoneElse() {
        for (Species s : Species.values()) {
            if (s != Species.HUNTER_OF_THE_FIVE) {
                BloodlineState st = BloodlineState.NONE.toBuilder().species(s).markKills(9).build();
                assertEquals(st, run(st, 25).state(), s.name());
            }
        }
        BloodlineState vampire = BloodlineState.NONE.toBuilder().species(Species.NONE).build();
        assertEquals(vampire, run(vampire, 25).state());
    }

    @Test
    void theSpeciesAndTheInstinctClockAreUntouched() {
        BloodlineState after = run(hof(8), 25).state();
        assertEquals(Species.HUNTER_OF_THE_FIVE, after.species());
        assertEquals(77, after.lastVampireKill());
        assertEquals(hof(8).toBuilder().markKills(after.markKills()).build(), after);
    }

    @Test
    void percentZeroLosesNothing() {
        assertEquals(20, run(hof(20), 0).state().markKills());
    }

    @Test
    void aHundredPercentGoesToZeroNotBelow() {
        assertEquals(0, run(hof(7), 100).state().markKills());
        assertEquals(0, run(hof(7), 1000).state().markKills());
        assertTrue(run(hof(0), 100).state().markKills() >= 0);
    }

    @Test
    void theContextDefaultIsAQuarter() {
        assertEquals(25, BloodlineContext.defaults().codePenaltyPercent());
        assertEquals(40, BloodlineContext.defaults().withCodePenaltyPercent(40).codePenaltyPercent());
    }

    @Test
    void everyCopyCarriesThePenalty() {
        BloodlineContext c = BloodlineContext.defaults().withCodePenaltyPercent(33);
        assertEquals(33, c.withFaction(BaseFaction.HUNTER).codePenaltyPercent());
        assertEquals(33, c.withHumanLike(false).codePenaltyPercent());
        assertEquals(33, c.withOriginalsInWorld(2).codePenaltyPercent());
        assertEquals(33, c.withMaxOriginals(2).codePenaltyPercent());
        assertEquals(33, c.withBloodSource(null).codePenaltyPercent());
        assertEquals(33, c.withTransitionTicks(5).codePenaltyPercent());
        assertEquals(33, c.withGameTime(5).codePenaltyPercent());
        assertEquals(33, c.withFiveCount(1).codePenaltyPercent());
        assertEquals(33, c.withMaxFive(1).codePenaltyPercent());
        assertEquals(33, c.withWerewolvesLoaded(false).codePenaltyPercent());
        assertEquals(33, c.withVampireBloodTicks(1).codePenaltyPercent());
    }

    @Test
    void theNewEventIsLastSoNoOrdinalMoved() {
        BloodlineEvent[] all = BloodlineEvent.values();
        assertEquals(CODE_BROKEN, all[all.length - 1]);
        assertEquals(BloodlineEvent.AUGUSTINE_SYRINGE, all[all.length - 2]);
    }
}