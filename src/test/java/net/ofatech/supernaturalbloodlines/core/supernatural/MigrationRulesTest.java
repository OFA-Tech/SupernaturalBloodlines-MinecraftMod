package net.ofatech.supernaturalbloodlines.core.supernatural;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules.Facts;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules.Result;
import org.junit.jupiter.api.Test;

class MigrationRulesTest {
    private static final LadderTable T = ShippedLadders.all();
    private static final long DAY = StatePredicates.DAY_TICKS;
    private static final long NOW = 100 * DAY;
    private static final String VAMPIRE_FACTION = "vampirism:vampire";
    private static final String WOLF_FACTION = "werewolves:werewolf";

    private static BloodlineState bl(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    private static BloodlineState rank(BloodlineState b, WolfState.Rank r) {
        return b.toBuilder().wolf(b.wolf().toBuilder().rank(r).build()).build();
    }

    private static Facts facts(SupernaturalRace creature, SupernaturalRace magic, BloodlineState b, String faction, int level, String apex) {
        return new Facts(creature, Optional.ofNullable(magic), b, NOW, faction, level, false, false, 0, apex);
    }

    private static Result migrate(Facts f) {
        return MigrationRules.migrate(T, SupernaturalState.NONE, f);
    }

    private static int spOfRungs(SupernaturalRace race, int from, int to) {
        int sp = 0;
        for (int i = from; i <= to; i++) {
            sp += T.get(race).rungs().get(i).sp();
        }
        return sp;
    }

    @Test
    void anOriginalKeepsItsLayer() {
        Result r = migrate(facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 4, null));
        assertEquals(1, r.state().schema());
        assertEquals(LayerSlices.layerRung(ORIGINAL), r.state().rung(ORIGINAL), "the layer rung");
        assertTrue(LayerSlices.slice(ORIGINAL, r.state().rung(ORIGINAL)).isEmpty(), "the whole layer applies");
        assertEquals(List.of("L-ORI-1", "L-ORI-2", "L-ORI-3"), r.rungsGiven());
        assertEquals(spOfRungs(ORIGINAL, 1, 3), r.state().sp());
        assertEquals(r.state().sp(), r.state().spEarned());
        assertEquals(r.state().sp(), r.spGiven());
        assertEquals(List.of("L-ORI-1", "L-ORI-2", "L-ORI-3"), r.state().backfill());
    }

    @Test
    void aTaskTheStateProvesIsDoneAndACountIsNot() {
        Result r = migrate(facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 4, null));
        // level 4 proves L-ORI-1a, but "drink 20 times" has no history to prove it: the rung came from the floor
        assertEquals(java.util.Set.of(), r.state().counters().keySet());
        assertFalse(r.state().done().contains("L-ORI-1b"));
        Result noFloor = migrate(facts(VAMPIRE, null, BloodlineState.NONE, VAMPIRE_FACTION, 2, null));
        assertTrue(noFloor.state().done().contains("L-VAM-1a"), "level 2 is proven");
        assertFalse(noFloor.state().done().contains("L-VAM-1b"), "ten feeds are not");
        assertEquals(0, noFloor.state().rung(VAMPIRE));
        assertEquals(0, noFloor.state().sp());
        assertEquals(List.of(), noFloor.rungsGiven());
    }

    @Test
    void aRungWhoseEveryTaskIsProvenIsReachedAndItsTasksClaimed() {
        // L-WOL-5 asks only for the rank
        SupernaturalState from = SupernaturalState.NONE.toBuilder().rung(WEREWOLF, 4).build();
        Facts f = facts(WEREWOLF, null, rank(BloodlineState.NONE, WolfState.Rank.ALPHA), WOLF_FACTION, 14, null);
        Result r = MigrationRules.migrate(T, from, f);
        assertEquals(5, r.state().rung(WEREWOLF));
        assertTrue(r.state().claimed().contains("L-WOL-5a"));
        assertEquals(6, r.state().sp());
    }

    @Test
    void anAlphaIsAtLeastAtLWol5() {
        Result r = migrate(facts(WEREWOLF, null, rank(BloodlineState.NONE, WolfState.Rank.ALPHA), WOLF_FACTION, 1, null));
        assertEquals(5, r.state().rung(WEREWOLF));
        assertEquals(spOfRungs(WEREWOLF, 1, 5), r.state().sp());
        assertEquals(List.of("L-WOL-1", "L-WOL-2", "L-WOL-3", "L-WOL-4", "L-WOL-5"), r.rungsGiven());
    }

    @Test
    void aTrueAlphaWhoIsNotAWerewolfNowKeepsTheRungDormant() {
        Result r = migrate(facts(HUMAN, null, rank(BloodlineState.NONE, WolfState.Rank.TRUE_ALPHA), null, 0, null));
        assertEquals(5, r.state().rung(WEREWOLF), "the werewolf ladder keeps what the rank proves");
        assertEquals(0, r.state().rung(HUMAN));
        assertEquals(spOfRungs(WEREWOLF, 1, 5), r.state().sp());
        assertTrue(r.state().raceSince().containsKey("human"));
        assertFalse(r.state().raceSince().containsKey("werewolf"), "they are not a werewolf now");
    }

    @Test
    void aWitchWerewolfIsMigratedOnBothLadders() {
        Result r = migrate(facts(WEREWOLF, WITCH, bl(Species.WITCH), WOLF_FACTION, 5, null));
        assertEquals(3, r.state().rung(WITCH), "the witch layer rung");
        assertTrue(r.state().done().contains("L-WOL-1a"), "level 5 proves the werewolf level step");
        assertEquals(0, r.state().rung(WEREWOLF), "a forced shift survived has no history");
        assertTrue(r.state().raceSince().keySet().containsAll(java.util.Set.of("werewolf", "witch")));
        assertEquals(spOfRungs(WITCH, 1, 3), r.state().sp());
    }

    @Test
    void anApexRaceIsAtTheTopRung() {
        Result r = migrate(facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 14, "primordial_vampire"));
        assertEquals(5, r.state().rung(ORIGINAL));
        assertEquals(spOfRungs(ORIGINAL, 1, 5), r.state().sp());
        Result siphoner = migrate(facts(HUMAN, SIPHONER, bl(Species.SIPHONER), null, 0, "void_siphon"));
        assertEquals(5, siphoner.state().rung(SIPHONER));
        Result heretic = migrate(facts(HERETIC, null, bl(Species.HERETIC), VAMPIRE_FACTION, 4, "void_siphon"));
        assertEquals(5, heretic.state().rung(HERETIC));
        assertEquals(3, migrate(facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 4, "elf")).state().rung(ORIGINAL), "a Tensura race that is not ours changes nothing");
    }

    @Test
    void anApexRaceOfAnotherLineDoesNotRaiseThisOne() {
        Result r = migrate(facts(HERETIC, null, bl(Species.HERETIC), VAMPIRE_FACTION, 4, "primordial_hybrid"));
        assertEquals(3, r.state().rung(HERETIC), "only the layer rung");
    }

    @Test
    void theApexFloorIsClippedToTheLadder() {
        // without Tensura the ladder ends before the apex rung; the floor stops at its last rung
        LadderTable noTensura = ShippedLadders.load(mod -> !mod.equals("tensura"));
        Result r = MigrationRules.migrate(noTensura, SupernaturalState.NONE,
                facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 14, "primordial_vampire"));
        assertEquals(4, r.state().rung(ORIGINAL));
    }

    @Test
    void theFloorsCombine() {
        // an Original hybrid who is an Alpha: the layer rung (3) and the pack floor of the werewolf ladder do not interfere
        BloodlineState b = rank(bl(Species.ORIGINAL_HYBRID), WolfState.Rank.ALPHA);
        Result r = migrate(facts(ORIGINAL_HYBRID, null, b, WOLF_FACTION, 14, null));
        assertEquals(3, r.state().rung(ORIGINAL_HYBRID), "the proven rung 1 and the layer rung 3");
        assertTrue(r.state().claimed().contains("L-OHY-1b"), "the proven rung 1 has its tasks claimed");
        assertEquals(0, r.state().rung(WEREWOLF), "they are an Original hybrid, not a werewolf creature, and the rank floor is for the werewolf ladder");
    }

    @Test
    void aSecondMigrationChangesNothing() {
        Result first = migrate(facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 4, null));
        Result second = MigrationRules.migrate(T, first.state(), facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 14, "primordial_vampire"));
        assertEquals(first.state(), second.state());
        assertEquals(List.of(), second.rungsGiven());
        assertEquals(0, second.spGiven());
    }

    @Test
    void thePoolOnlyGrows() {
        SupernaturalState had = SupernaturalState.NONE.toBuilder().sp(7).spEarned(9).build();
        Result r = MigrationRules.migrate(T, had, facts(ORIGINAL, null, bl(Species.ORIGINAL), VAMPIRE_FACTION, 4, null));
        assertEquals(7 + spOfRungs(ORIGINAL, 1, 3), r.state().sp());
        assertEquals(9 + spOfRungs(ORIGINAL, 1, 3), r.state().spEarned());
    }

    @Test
    void theRaceStartsWhenTheSpeciesDid() {
        long turned = 40 * DAY;
        BloodlineState b = BloodlineState.NONE.toBuilder().turnedAt(turned).build();
        Result r = migrate(facts(VAMPIRE, null, b, VAMPIRE_FACTION, 1, null));
        assertEquals(turned, r.state().raceSince().get("vampire"), "a long-time vampire keeps their days");
        // and the days count from it: 60 days at a vampire's rung 4 prove "30 days as a vampire" together with level 14
        SupernaturalState from = SupernaturalState.NONE.toBuilder().rung(VAMPIRE, 4).build();
        Result days = MigrationRules.migrate(T, from, facts(VAMPIRE, null, b, VAMPIRE_FACTION, 14, null));
        assertEquals(5, days.state().rung(VAMPIRE), "L-VAM-5 asks level 14 and 30 days as a vampire");
        Result fresh = MigrationRules.migrate(T, from, facts(VAMPIRE, null, BloodlineState.NONE, VAMPIRE_FACTION, 14, null));
        assertEquals(4, fresh.state().rung(VAMPIRE), "no history: a new vampire has no days");
    }

    @Test
    void aGhoulIsLeftAlone() {
        Result r = migrate(facts(GHOUL, null, BloodlineState.NONE.toBuilder().ghoul(true).build(), VAMPIRE_FACTION, 1, null));
        assertEquals(1, r.state().schema());
        assertEquals(0, r.state().sp());
        assertTrue(r.state().rungs().isEmpty() && r.state().raceSince().isEmpty());
    }

    @Test
    void noReadingIsEverACount() {
        Facts f = new Facts(WEREWOLF, Optional.of(WITCH), rank(bl(Species.WITCH), WolfState.Rank.ALPHA), NOW, WOLF_FACTION, 14, true, true, 3, null);
        List<TaskEvent> readings = MigrationRules.readings(f, java.util.Map.of("werewolf", 0L, "witch", 0L));
        assertFalse(readings.isEmpty());
        for (TaskEvent e : readings) {
            assertFalse(e.detector().counting(), e.detector() + " is a count and can never be proven");
        }
    }

    @Test
    void theFloorIsClippedWhenTheSpeciesPartIsOff() {
        // the Hunter ladder without the species part ends at L-HUN-2 (rungs 3 to 5 are the Five's)
        LadderTable noSpecies = LadderTable.parse(ShippedLadders.files(), mod -> true, part -> !part.equals("species")).table();
        Result r = MigrationRules.migrate(noSpecies, SupernaturalState.NONE, facts(HUNTER, null, bl(Species.HUNTER_OF_THE_FIVE), "vampirism:hunter", 6, null));
        assertEquals(2, r.state().rung(HUNTER), "the ladder ends at L-HUN-2 here");
    }
}
