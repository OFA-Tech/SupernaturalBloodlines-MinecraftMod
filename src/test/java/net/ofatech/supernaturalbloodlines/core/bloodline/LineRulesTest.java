package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static net.ofatech.supernaturalbloodlines.core.bloodline.LineRules.*;
import static org.junit.jupiter.api.Assertions.*;

class LineRulesTest {
    private static final UUID O = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID T = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID X = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-0000000000a4");

    @Test
    void npcRootsAreStableAndDistinct() {
        Set<UUID> ids = new HashSet<>();
        for (NpcOriginal o : NpcOriginal.values()) {
            ids.add(o.uuid());
        }
        assertEquals(6, ids.size());
        assertEquals(Optional.of(NpcOriginal.KLAUS), NpcOriginal.byUuid(NpcOriginal.KLAUS.uuid()));
        assertTrue(NpcOriginal.byUuid(UUID.randomUUID()).isEmpty());
        assertEquals(UUID.nameUUIDFromBytes("supernaturalbloodlines:npc_original:klaus".getBytes(StandardCharsets.UTF_8)), NpcOriginal.KLAUS.uuid());
        assertEquals("supernaturalbloodlines.original.klaus", NpcOriginal.KLAUS.translationKey());
        assertEquals(Optional.of(NpcOriginal.KOL), NpcOriginal.byId("kol"));
    }

    @Test
    void stateTable() {
        assertEquals(State.ORIGINAL, state(true, false, true, false));
        assertEquals(State.NOT_VAMPIRE, state(false, false, true, true));
        assertEquals(State.FREE, state(false, true, true, true));
        assertEquals(State.IN_LINE, state(false, true, false, true));
        assertEquals(State.SIRE_UNKNOWN, state(false, true, false, false));
    }

    @Test
    void childOfAnOriginalGetsTheOriginal() {
        assertEquals(Optional.of(O), inheritedRoot(O, true, Optional.empty()));
    }

    @Test
    void childOfAMemberGetsTheMembersRoot() {
        assertEquals(Optional.of(O), inheritedRoot(T, false, Optional.of(O)));
    }

    @Test
    void childOfALinelessSireGetsNothing() {
        assertTrue(inheritedRoot(T, false, Optional.empty()).isEmpty());
    }

    @Test
    void showsRootRules() {
        assertFalse(showsRoot(null, O, false));
        assertTrue(showsRoot(O, O, false));
        assertFalse(showsRoot(T, O, false));
        assertTrue(showsRoot(null, O, true));
    }

    @Test
    void weightsParseLeniently() {
        assertEquals(Map.of(NpcOriginal.KLAUS, 3, NpcOriginal.KOL, 0),
                parseWeights(List.of("klaus=3", "bad", "kol=-2", "nobody=1", "finn=x")));
    }

    @Test
    void pickRootIsWeightedAndSkipsTheDead() {
        Map<NpcOriginal, Integer> w = Map.of(NpcOriginal.MIKAEL, 1, NpcOriginal.KLAUS, 3);
        assertEquals(Optional.of(NpcOriginal.MIKAEL), pickRoot(w, Set.of(), 0.0));
        assertEquals(Optional.of(NpcOriginal.KLAUS), pickRoot(w, Set.of(), 0.26));
        assertEquals(Optional.of(NpcOriginal.KLAUS), pickRoot(w, Set.of(), 0.99));
        for (double roll : new double[]{0.0, 0.5, 0.99}) {
            assertEquals(Optional.of(NpcOriginal.MIKAEL), pickRoot(w, Set.of(NpcOriginal.KLAUS), roll));
        }
        assertTrue(pickRoot(Map.of(NpcOriginal.MIKAEL, 0), Set.of(), 0.5).isEmpty());
        assertTrue(pickRoot(w, Set.of(NpcOriginal.MIKAEL, NpcOriginal.KLAUS), 0.5).isEmpty());
    }

    @Test
    void pairBloodsFindsTheOriginalAndTheTarget() {
        Function<UUID, Optional<UUID>> rootOf = id -> id.equals(T) || id.equals(O) ? Optional.of(O) : id.equals(X) ? Optional.of(P) : Optional.empty();
        assertEquals(Optional.of(new BloodPair(O, T)), pairBloods(List.of(T, O), rootOf));
        assertTrue(pairBloods(List.of(O, O), rootOf).isEmpty());
        assertTrue(pairBloods(List.of(T, X), rootOf).isEmpty());
        assertTrue(pairBloods(List.of(T), rootOf).isEmpty());
    }

    private static final Optional<BloodPair> PAIR = Optional.of(new BloodPair(O, T));

    @Test
    void brokenLineRefusalOrder() {
        assertEquals(Optional.empty(), brokenLineRefusal(new BrokenLineFacts(true, true, 2, PAIR, true, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "not_witch"), brokenLineRefusal(new BrokenLineFacts(false, true, 2, PAIR, true, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "no_eclipse"), brokenLineRefusal(new BrokenLineFacts(true, false, 2, PAIR, true, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "no_blood"), brokenLineRefusal(new BrokenLineFacts(true, true, 1, PAIR, true, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "wrong_blood"), brokenLineRefusal(new BrokenLineFacts(true, true, 2, Optional.empty(), true, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "target_away"), brokenLineRefusal(new BrokenLineFacts(true, true, 2, PAIR, false, false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "target_original"), brokenLineRefusal(new BrokenLineFacts(true, true, 2, PAIR, true, true)));
        // two failures: the earlier one in the order wins
        assertEquals(Optional.of(REFUSAL_PREFIX + "not_witch"), brokenLineRefusal(new BrokenLineFacts(false, false, 0, Optional.empty(), false, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "wrong_blood"), brokenLineRefusal(new BrokenLineFacts(true, true, 2, Optional.empty(), false, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "target_away"), brokenLineRefusal(new BrokenLineFacts(true, true, 2, PAIR, false, true)));
    }

    private static ImprisonedFacts imp(boolean witch, boolean eclipse, boolean blood, boolean online, boolean original,
                                       Optional<UUID> targetRoot, Optional<UUID> trapped, boolean ring) {
        return new ImprisonedFacts(witch, eclipse, blood ? Optional.of(T) : Optional.empty(), online, original, targetRoot, trapped, ring);
    }

    @Test
    void imprisonedRefusalOrder() {
        Optional<UUID> o = Optional.of(O);
        assertEquals(Optional.empty(), imprisonedRefusal(imp(true, true, true, true, false, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "not_witch"), imprisonedRefusal(imp(false, true, true, true, false, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "no_eclipse"), imprisonedRefusal(imp(true, false, true, true, false, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "no_blood"), imprisonedRefusal(imp(true, true, false, true, false, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "target_away"), imprisonedRefusal(imp(true, true, true, false, false, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "target_original"), imprisonedRefusal(imp(true, true, true, true, true, o, o, true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "nobody_trapped"), imprisonedRefusal(imp(true, true, true, true, false, o, Optional.empty(), true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "wrong_original"), imprisonedRefusal(imp(true, true, true, true, false, o, Optional.of(P), true)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "ring_open"), imprisonedRefusal(imp(true, true, true, true, false, o, o, false)));
        // earlier wins
        assertEquals(Optional.of(REFUSAL_PREFIX + "nobody_trapped"), imprisonedRefusal(imp(true, true, true, true, false, o, Optional.empty(), false)));
        assertEquals(Optional.of(REFUSAL_PREFIX + "wrong_original"), imprisonedRefusal(imp(true, true, true, true, false, o, Optional.of(P), false)));
    }
}
