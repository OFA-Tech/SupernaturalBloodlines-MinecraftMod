package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapTableTest.ref;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import org.junit.jupiter.api.Test;

class DedupTest {
    private record E(PowerRef ref, int modeCount) implements Dedup.Item {
        E(String key) {
            this(OverlapTableTest.ref(key), 1);
        }

        @Override
        public String toString() {
            return ref.key();
        }
    }

    private static final OverlapTable SEED = OverlapTableTest.seed();
    private static final String V_INVIS = "vampirism/vampirism:vampire_invisibility";
    private static final String T_INVIS = "tensura/tensura:invisible";
    private static final String I_INVIS = "infinity/infinity:reality.invisibility";
    private static final String V_RAGE = "vampirism/vampirism:vampire_rage";
    private static final String W_RAGE = "werewolves/werewolves:rage";
    private static final String RACE = "tensura/tensura:race_ability";
    private static final String BAT = "vampirism/vampirism:bat";
    private static final String BITE = "bloodline/supernaturalbloodlines:bite";
    private static final String OTHER = "tensura/tensura:coercion";

    private static List<E> list(String... keys) {
        List<E> out = new ArrayList<>();
        for (String k : keys) {
            out.add(new E(k));
        }
        return out;
    }

    private static List<String> shown(Dedup.Result<E> r) {
        return r.shown().stream().map(e -> e.ref().key()).toList();
    }

    @Test
    void m01OneEntryWhenBothSidesPresent() {
        Dedup.Result<E> r = Dedup.apply(list(T_INVIS, V_INVIS, OTHER), SEED);
        assertEquals(List.of(V_INVIS, OTHER), shown(r), "the main side stays, the order of the input is kept");
        assertEquals("invisibility", r.hidden().get(r.hidden().keySet().iterator().next()).id());
        assertEquals("invisibility", r.groupOf().get(r.shown().get(0)).id());
    }

    @Test
    void m01SpellIsAThirdMemberAndIsHidden() {
        // for a vampire: the Vampirism action, the Tensura spell and an Infinity stone power are all there: one entry stays
        assertEquals(List.of(V_INVIS), shown(Dedup.apply(list(V_INVIS, T_INVIS, I_INVIS), SEED)));
        // without Infinity (the stone power is not deduplicated in production, but the seed lists it): the spell is hidden
        assertEquals(List.of(V_INVIS), shown(Dedup.apply(list(V_INVIS, T_INVIS), SEED)));
    }

    @Test
    void m01OnlyOneSidePresentChangesNothing() {
        assertEquals(List.of(T_INVIS, OTHER), shown(Dedup.apply(list(T_INVIS, OTHER), SEED)));
        assertEquals(List.of(V_INVIS), shown(Dedup.apply(list(V_INVIS), SEED)));
    }

    @Test
    void m01MainMissingFallsBackToTheOtherSide() {
        // no Vampirism: Infinity (the next member in file order) stays and the spell goes
        assertEquals(List.of(I_INVIS), shown(Dedup.apply(list(T_INVIS, I_INVIS), SEED)));
    }

    @Test
    void m06FakeSidesDedupLikeM01() {
        assertEquals(List.of(V_RAGE), shown(Dedup.apply(list(W_RAGE, V_RAGE), SEED)));
        assertEquals(List.of(W_RAGE), shown(Dedup.apply(list(W_RAGE), SEED)));
    }

    @Test
    void noStackingOfTheGroupAcrossThreeMembers() {
        Dedup.Result<E> r = Dedup.apply(list(I_INVIS, T_INVIS, V_INVIS), SEED);
        assertEquals(1, r.shown().size());
        assertEquals(2, r.hidden().size());
    }

    @Test
    void theSideThatIsOnStaysSoThePlayerCanTurnItOff() {
        // the Tensura spell is the one that is on: it is shown, the main is hidden (until it is off)
        Dedup.Result<E> r = Dedup.apply(list(V_INVIS, T_INVIS), SEED, id -> id.equals("invisibility") ? 2 : -1);
        assertEquals(List.of(T_INVIS), shown(r));
        // an active side that is not among the entries changes nothing
        assertEquals(List.of(V_INVIS), shown(Dedup.apply(list(V_INVIS, T_INVIS), SEED, id -> 1)));
    }

    @Test
    void onlyTheHighestChainTierIsShown() {
        String one = "tensura/tensura:self_regeneration";
        String two = "tensura/tensura:ultraspeed_regeneration";
        String three = "tensura/tensura:infinite_regeneration";
        Dedup.Result<E> r = Dedup.apply(list(one, OTHER, three, two), SEED);
        assertEquals(List.of(OTHER, three), shown(r));
        assertEquals(2, r.collapsed().size());
        assertEquals(List.of(one, OTHER), shown(Dedup.apply(list(one, OTHER), SEED)), "a lone tier stays");
        assertEquals(List.of(two), shown(Dedup.apply(list(one, two), SEED)));
    }

    @Test
    void chainMemberMatchesAGroupMemberOfItsChain() {
        // the group names self_regeneration; the player has the top tier: it is the same power, so the vampire's regen wins
        String three = "tensura/tensura:infinite_regeneration";
        String regen = "vampirism/vampirism:regen";
        assertEquals(List.of(regen), shown(Dedup.apply(list(three, regen), SEED)));
        assertEquals(List.of(regen), shown(Dedup.apply(list(three, "tensura/tensura:self_regeneration", regen), SEED)));
    }

    @Test
    void skillWithModesIsNeverHiddenByAModeMatch() {
        // a Tensura skill with several modes that is named in a group (the seed names invisible): it is neither hidden nor does it hide the vampire's action
        List<E> in = List.of(new E(ref(V_INVIS), 1), new E(ref(T_INVIS), 3));
        Dedup.Result<E> r = Dedup.apply(in, SEED);
        assertEquals(in, r.shown());
        assertTrue(r.hidden().isEmpty());
        // and as a main it hides nothing either
        List<E> multiMain = List.of(new E(ref(V_INVIS), 3), new E(ref(T_INVIS), 1));
        assertEquals(multiMain, Dedup.apply(multiMain, SEED).shown());
    }

    @Test
    void aRefInTwoGroupsIsHiddenOnceAndStaysHidden() {
        // the race ability is in bat_form (main: the bat) and in bite (main: our bite): a vampire with all three sees the bat and the bite, and the race ability once hidden
        Dedup.Result<E> r = Dedup.apply(list(BITE, BAT, RACE), SEED);
        assertEquals(List.of(BITE, BAT), shown(r));
        assertEquals(1, r.hidden().size());
        assertEquals("bat_form", r.hidden().values().iterator().next().id(), "hidden by the first group in file order");
    }

    @Test
    void aHiddenEntryTakesNoPartInALaterGroup() {
        // without the bat the race ability meets the bite: the bite stays, the race ability goes
        assertEquals(List.of(BITE), shown(Dedup.apply(list(RACE, BITE), SEED)));
        // with the bat but no bite: the race ability is hidden by the bat, so it cannot hide anything else
        assertEquals(List.of(BAT), shown(Dedup.apply(list(RACE, BAT), SEED)));
    }

    @Test
    void emptyTableChangesNothing() {
        List<E> in = list(V_INVIS, T_INVIS, I_INVIS);
        Dedup.Result<E> r = Dedup.apply(in, OverlapTable.EMPTY);
        assertEquals(in, r.shown());
        assertTrue(r.hidden().isEmpty() && r.collapsed().isEmpty());
        assertTrue(Dedup.apply(List.<E>of(), SEED).shown().isEmpty());
    }

    @Test
    void dedupIsStableAcrossRuns() {
        List<E> in = list(OTHER, T_INVIS, V_RAGE, V_INVIS, W_RAGE, I_INVIS);
        List<String> first = shown(Dedup.apply(in, SEED));
        for (int i = 0; i < 5; i++) {
            assertEquals(first, shown(Dedup.apply(new ArrayList<>(in), SEED)));
        }
        assertEquals(List.of(OTHER, V_RAGE, V_INVIS), first);
    }

    @Test
    void looseMatchesStaySeparate() {
        OverlapTable prod = OverlapTableTest.production();
        // research section 3: L1 threat warning, L4 leaps, L5 disguise, L12 forms: none of them is in the production table
        List<E> in = list("vampirism/vampirism:awareness_hunter", "tensura/tensura:danger_sense", "werewolves/werewolves:sixth_sense", "vampirism/vampirism:jump_boost",
                "werewolves/werewolves:leap", "vampirism/vampirism:disguise_vampire", "tensura/tensura:falsifier", "werewolves/werewolves:beast_form", "tensura/tensura:beast_transformation",
                "tensura/tensura:hero_haki", "werewolves/werewolves:fear", "tensura/tensura:traveler", "vampirism/vampirism:teleport");
        Dedup.Result<E> r = Dedup.apply(in, prod);
        assertEquals(in.size(), r.shown().size());
        // the haki and traveler stay beside the fear and the teleport they resemble
        assertTrue(shown(r).contains("tensura/tensura:hero_haki") && shown(r).contains("tensura/tensura:traveler"));
    }

    @Test
    void productionGroupsDedupTheAcceptedPairs() {
        OverlapTable prod = OverlapTableTest.production();
        assertEquals(List.of("vampirism/vampirism:regen"), shown(Dedup.apply(list("tensura/tensura:infinite_regeneration", "vampirism/vampirism:regen"), prod)));
        assertEquals(List.of("werewolves/werewolves:fear"), shown(Dedup.apply(list("tensura/tensura:coercion", "werewolves/werewolves:fear"), prod)));
        assertEquals(List.of("tensura/tensura:coercion"), shown(Dedup.apply(list("tensura/tensura:coercion", "tensura/ascension:intimidating_roar"), prod)));
        assertEquals(List.of("tensura/tensura:analytical_appraisal"), shown(Dedup.apply(list("tensura/tensura:analytical_appraisal", "great_sage/greatsage:scan"), prod)));
        assertEquals(List.of(BITE), shown(Dedup.apply(list(RACE, BITE), prod)));
        // a stone power is never part of a group: both stay
        assertEquals(2, Dedup.apply(list(V_INVIS, I_INVIS), prod).shown().size());
    }
}
