package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.junit.jupiter.api.Test;

class StatePredicatesTest {
    private static BloodlineState wolf(WolfState.Rank rank, long betaSince) {
        return BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().rank(rank).betaSince(betaSince).build()).build();
    }

    private static long v(String predicate, BloodlineState s) {
        return StatePredicates.value(predicate, s, 100 * StatePredicates.DAY_TICKS, false, false).orElseThrow();
    }

    @Test
    void everyIdTheLaddersUseIsKnown() {
        for (String id : StatePredicates.IDS) {
            assertTrue(StatePredicates.value(id, BloodlineState.NONE, 0, false, false).isPresent(), id);
        }
        assertTrue(StatePredicates.value("nonsense", BloodlineState.NONE, 0, false, false).isEmpty());
    }

    @Test
    void ranksAreOrdered() {
        assertEquals(0, v("rank_beta", wolf(WolfState.Rank.OMEGA, 0)));
        assertEquals(1, v("rank_beta", wolf(WolfState.Rank.BETA, 5)));
        assertEquals(1, v("rank_beta", wolf(WolfState.Rank.ALPHA, 0)));
        assertEquals(0, v("rank_alpha", wolf(WolfState.Rank.BETA, 5)));
        assertEquals(1, v("rank_alpha", wolf(WolfState.Rank.ALPHA, 0)));
        assertEquals(1, v("rank_alpha", wolf(WolfState.Rank.TRUE_ALPHA, 0)));
    }

    @Test
    void betaDaysOnlyCountForABetaWithAStamp() {
        assertEquals(95, v("beta_days", wolf(WolfState.Rank.BETA, 5 * StatePredicates.DAY_TICKS)));
        assertEquals(0, v("beta_days", wolf(WolfState.Rank.BETA, 0)));
        // an Alpha's stamp is cleared; Beta-or-Alpha time is the pack_days counter
        assertEquals(0, v("beta_days", wolf(WolfState.Rank.ALPHA, 5 * StatePredicates.DAY_TICKS)));
        assertTrue(StatePredicates.inPack(wolf(WolfState.Rank.BETA, 1)));
        assertTrue(StatePredicates.inPack(wolf(WolfState.Rank.ALPHA, 0)));
        assertTrue(StatePredicates.inPack(wolf(WolfState.Rank.TRUE_ALPHA, 0)));
        assertTrue(!StatePredicates.inPack(wolf(WolfState.Rank.OMEGA, 0)));
        assertTrue(!StatePredicates.inPack(BloodlineState.NONE));
    }

    @Test
    void moonUnboundIsAHybridWhoseCurseIsLifted() {
        BloodlineState bound = BloodlineState.NONE.toBuilder().species(Species.HYBRID).hybridCurseBound(true).build();
        assertEquals(0, v("moon_unbound", bound));
        assertEquals(1, v("moon_unbound", bound.toBuilder().hybridCurseBound(false).build()));
        assertEquals(1, v("moon_unbound", bound.toBuilder().species(Species.ORIGINAL_HYBRID).hybridCurseBound(false).build()));
        assertEquals(1, v("moon_unbound", bound.toBuilder().species(Species.TRIBRID).hybridCurseBound(false).build()), "a tribrid is a hybrid type like the other two");
        assertEquals(0, v("moon_unbound", BloodlineState.NONE));
    }

    @Test
    void theNumbersComeStraightFromTheState() {
        BloodlineState s = BloodlineState.NONE.toBuilder().markKills(7).siphonedPower(640)
                .wolf(WolfState.NONE.toBuilder().defenderKills(33).build()).species(Species.HUNTER_OF_THE_FIVE).build();
        assertEquals(7, v("mark_kills", s));
        assertEquals(640, v("siphoned_power", s));
        assertEquals(33, v("defender_kills", s));
        assertEquals(1, v("hunter_of_the_five", s));
        assertEquals(0, v("hunter_of_the_five", BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build()));
    }

    @Test
    void emissaryAndMoonControlAreWhatTheCallerSays() {
        assertEquals(1, StatePredicates.value("emissary", BloodlineState.NONE, 0, true, false).orElseThrow());
        assertEquals(0, StatePredicates.value("emissary", BloodlineState.NONE, 0, false, true).orElseThrow());
        assertEquals(1, StatePredicates.value("moon_control", BloodlineState.NONE, 0, false, true).orElseThrow());
    }
}
