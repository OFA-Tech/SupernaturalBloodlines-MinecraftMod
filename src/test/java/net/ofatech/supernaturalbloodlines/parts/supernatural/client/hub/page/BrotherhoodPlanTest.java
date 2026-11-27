package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.MapRegions;
import org.junit.jupiter.api.Test;

class BrotherhoodPlanTest {
    private static BrotherhoodView view(boolean fits, boolean on, int mark, boolean complete, int strikes, int seconds) {
        return view(fits, on, mark, complete, strikes, seconds, 25);
    }

    private static BrotherhoodView view(boolean fits, boolean on, int mark, boolean complete, int strikes, int seconds, int penalty) {
        return new BrotherhoodView(true, fits, on, mark, 19, complete, 4, 12, true, true, strikes, 3, seconds, true, penalty, 7);
    }

    private static BrotherhoodPlan.Line line(BrotherhoodView v, String key) {
        return BrotherhoodPlan.of(v, true).lines().stream().filter(l -> l.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void loadedButNotInABrotherhoodSaysSoInsteadOfWaiting() {
        assertEquals(List.of("not_in"), BrotherhoodPlan.of(BrotherhoodView.NONE, true).lines().stream().map(BrotherhoodPlan.Line::key).toList());
    }

    @Test
    void theMarkLineGivesTheMarkOutOfTwenty() {
        assertEquals(new BrotherhoodPlan.Line("mark", List.of(8, 20)), BrotherhoodPlan.markLine(8));
    }

    @Test
    void theMapTextCountsTheFragments() {
        assertEquals(List.of(3, 6), line(view(true, true, 8, false, 0, 0), "map.own").args());
        assertEquals(List.of(6, 6), line(view(true, true, 0, true, 0, 0), "map.complete").args());
    }

    @Test
    void theStrikesLineSaysTheWindow() {
        assertEquals(List.of(2, 3, 7), line(view(true, true, 8, false, 2, 0), "strikes").args());
    }

    @Test
    void theBreakerClockNeverBreaksBetweenNumberAndUnit() {
        String clock = (String) line(view(true, true, 8, false, 1, 43_200), "breaker").args().get(0);
        assertTrue(clock.contains("\u00A0") && !clock.contains(" "), clock);
    }

    @Test
    void theCostSaysHowMuchOfTheMark() {
        assertEquals(List.of(), line(view(true, true, 8, false, 0, 0, 25), "code.cost.quarter").args());
        assertEquals(List.of(40), line(view(true, true, 8, false, 0, 0, 40), "code.cost").args());
        assertTrue(BrotherhoodPlan.of(view(true, true, 8, false, 0, 0, 0), true).lines().stream().anyMatch(l -> l.key().equals("code.cost.none")));
    }

    @Test
    void theMapShowsOnlyTheOwnRegionsUntilAnyHasTwenty() {
        for (int mark = 0; mark <= 20; mark++) {
            assertEquals(MapRegions.revealed(mark), BrotherhoodPlan.of(view(true, true, mark, false, 0, 0), true).regions());
        }
    }

    @Test
    void theMapIsCompleteForEveryoneWhenAnyHasTwenty() {
        assertEquals(MapRegions.revealed(20), BrotherhoodPlan.of(view(true, true, 0, true, 0, 0), true).regions());
    }

    @Test
    void theCodeCardShowsStrikesOnlyAboveZero() {
        assertFalse(keys(true, true, 0, 0).contains("strikes"));
        assertTrue(keys(true, true, 1, 0).contains("strikes"));
        assertTrue(keys(true, true, 2, 0).contains("strikes"));
    }

    @Test
    void theBreakerTimerShowsOnlyWhileABreaker() {
        assertFalse(keys(true, true, 1, 0).contains("breaker"));
        assertTrue(keys(true, true, 1, 1).contains("breaker"));
    }

    @Test
    void aViewerWhoDoesNotFitSeesOneLineAndNoNumbers() {
        assertEquals(List.of("not_fit"), keys(false, true, 2, 60));
        assertEquals(Set.of(), BrotherhoodPlan.of(view(false, true, 20, true, 2, 60), true).regions());
    }

    @Test
    void perksOffSaysSoWithoutNumbers() {
        assertEquals(List.of("off"), keys(true, false, 2, 60));
    }

    @Test
    void theArmsLineShowsTheCountAndThePercent() {
        var line = BrotherhoodPlan.of(view(true, true, 8, false, 2, 60), true).lines().stream()
                .filter(l -> l.key().equals("arms")).findFirst().orElseThrow();
        assertEquals(List.of(4, 12), line.args());
    }

    @Test
    void noLineCarriesAnotherBrothersMark() {
        assertFalse(BrotherhoodPlan.of(view(true, true, 8, false, 2, 60), true).lines().stream()
                .anyMatch(l -> l.args().contains(19)));
    }

    @Test
    void thePipRowIsTheViewersAndTheLeadersOnly() {
        var v = view(true, true, 8, false, 0, 0);
        assertEquals(8, BrotherhoodPlan.mark(v, true, true, true));
        assertEquals(19, BrotherhoodPlan.mark(v, true, false, true));
        assertEquals(-1, BrotherhoodPlan.mark(v, true, false, false));
        assertEquals(-1, BrotherhoodPlan.mark(v, false, true, true));
    }

    @Test
    void itWaitsForThePayload() {
        assertEquals(List.of("loading"), BrotherhoodPlan.of(BrotherhoodView.NONE, false).lines().stream().map(BrotherhoodPlan.Line::key).toList());
    }

    private static List<String> keys(boolean fits, boolean on, int strikes, int seconds) {
        return BrotherhoodPlan.of(view(fits, on, 8, false, strikes, seconds), true).lines().stream().map(BrotherhoodPlan.Line::key).toList();
    }
}
