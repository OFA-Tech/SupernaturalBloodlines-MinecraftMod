package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules.Refusal;
import net.ofatech.supernaturalbloodlines.core.bloodline.SortRules.Slot;
import org.junit.jupiter.api.Test;

/** R148 and R149, row by row: who may be recruited, what a member weighs, what it does when its owner is away, and how a chest is tidied. */
class NpcRulesTest {
    /** A clean case: enabled, a werewolf recruiter, a beaten wolf, nothing in the way, room for more. */
    private static Refusal ok(boolean enabled, boolean werewolf, boolean wolf, boolean alpha, boolean boss, boolean bonded, float hp, int mine, int packN) {
        return NpcRules.recruit(enabled, werewolf, wolf, alpha, boss, bonded, hp, mine, 2, packN, 6);
    }

    @Test
    void aBeatenWolfIsRecruited() {
        assertEquals(Refusal.NONE, ok(true, true, true, false, false, false, 0.5f, 0, 0));
        assertEquals(Refusal.NONE, ok(true, true, true, false, false, false, 0.1f, 1, 5), "the last place of the owner and of the pack");
    }

    @Test
    void theSwitchComesFirst() {
        assertEquals(Refusal.OFF, ok(false, false, false, true, true, true, 1f, 9, 9));
    }

    @Test
    void onlyAWerewolfRecruits() {
        assertEquals(Refusal.NOT_WEREWOLF, ok(true, false, true, false, false, false, 0.1f, 0, 0));
    }

    @Test
    void onlyAWerewolfMobIsRecruited() {
        assertEquals(Refusal.NOT_A_WOLF, ok(true, true, false, false, false, false, 0.1f, 0, 0));
    }

    @Test
    void aTrialBossAndAnAlphaAreNeverRecruited() {
        assertEquals(Refusal.TRIAL_BOSS, ok(true, true, true, true, true, false, 0.1f, 0, 0), "the boss is both an Alpha and a boss: the boss reason");
        assertEquals(Refusal.ALPHA, ok(true, true, true, true, false, false, 0.1f, 0, 0));
    }

    @Test
    void aMobAlreadyBondedIsNotRecruitedAgain() {
        assertEquals(Refusal.ALREADY_BONDED, ok(true, true, true, false, false, true, 0.1f, 0, 0));
    }

    @Test
    void aWolfIsBeatenNotLured() {
        assertEquals(Refusal.NOT_BEATEN, ok(true, true, true, false, false, false, 0.51f, 0, 0));
        assertEquals(Refusal.NOT_BEATEN, ok(true, true, true, false, false, false, 1f, 0, 0));
    }

    @Test
    void theCapsAreTwoPerPlayerAndSixPerPack() {
        assertEquals(Refusal.OWNER_CAP, ok(true, true, true, false, false, false, 0.1f, 2, 2));
        assertEquals(Refusal.PACK_CAP, ok(true, true, true, false, false, false, 0.1f, 0, 6));
        assertEquals(Refusal.OWNER_CAP, ok(true, true, true, false, false, false, 0.1f, 2, 6), "the owner's cap is told first");
        assertEquals(Refusal.OWNER_CAP, NpcRules.recruit(true, true, true, false, false, false, 0.1f, 0, 0, 0, 6), "a cap of 0 is nobody");
    }

    @Test
    void whatAMemberWeighs() {
        assertEquals(0.5, NpcRules.weight(Source.BROTHER, 0.5, 0.25));
        assertEquals(0.25, NpcRules.weight(Source.TENSURA, 0.5, 0.25));
        assertEquals(0, NpcRules.weight(Source.SPIRIT, 0.5, 0.25), "a Wolf Spirit counts for nothing");
        assertEquals(0, NpcRules.weight(Source.BROTHER, -1, 0.25), "a weight below 0 counts as 0");
    }

    @Test
    void theBonusKeepsItsStepAndItsCapWithThePackBonus() {
        assertEquals(0.05, NpcRules.STEP);
        assertEquals(5, NpcRules.CAP);
    }

    /** The bonus table: players as before, NPCs by weight, their part of the sum capped at 40% of the cap (2 steps). */
    @Test
    void thePlayerOnlyBonusIsUnchanged() {
        assertEquals(0, NpcRules.bonus(2, 2, 0, 3, 0.4));
        assertEquals(0.15, NpcRules.bonus(3, 3, 0, 3, 0.4));
        assertEquals(0.25, NpcRules.bonus(5, 5, 0, 3, 0.4));
        assertEquals(0.25, NpcRules.bonus(6, 9, 0, 3, 0.4), "the cap");
        assertEquals(0.1, NpcRules.bonus(2, 2, 0, 2, 0.4), "Pack Heart: two are enough");
    }

    @Test
    void aBrotherCountsHalfTowardTheMinimumAndTheSum() {
        // 2 players + 2 Brothers (1.0): 3.0 reaches the minimum of 3; the sum is 2 + 1.0 = 3 steps
        assertEquals(0.15, NpcRules.bonus(2, 2, 1.0, 3, 0.4));
        // 2 players + 1 Brother (0.5): 2.5, under the minimum
        assertEquals(0, NpcRules.bonus(2, 2, 0.5, 3, 0.4));
        // 3 players + 6 Brothers (3.0): the NPC part is capped at 2 steps: 3 + 2 = 5 steps = the cap
        assertEquals(0.25, NpcRules.bonus(3, 3, 3.0, 3, 0.4));
        // 3 bitten players (doubled 6) + 1 Brother: the cap holds
        assertEquals(0.25, NpcRules.bonus(3, 6, 0.5, 3, 0.4));
    }

    @Test
    void noOneFarmsTheBonusFromWolvesAlone() {
        // no players at all: 6 Brothers weigh 3.0, the minimum is met, but the NPC part is capped at 2 steps: 10%, not 15% or more
        assertEquals(0.1, NpcRules.bonus(0, 0, 3.0, 3, 0.4));
        // an Alpha with 2 Brothers of their own (1.0) is under the minimum
        assertEquals(0, NpcRules.bonus(0, 0, 1.0, 3, 0.4));
        // a larger share lifts the NPC cap, a share of 0 removes it
        assertEquals(0.15, NpcRules.bonus(0, 0, 3.0, 3, 0.6));
        assertEquals(0, NpcRules.bonus(0, 0, 3.0, 3, 0.0));
    }

    @Test
    void theBonusTableForZeroToEightBrothersWithAndWithoutThreePlayers() {
        double[] without = {0, 0, 0, 0, 0, 0, 0.1, 0.1, 0.1};   // 0..8 NPCs of weight 0.5; the minimum of 3 needs 6; capped at 2 steps
        double[] with = {0.15, 0.175, 0.2, 0.225, 0.25, 0.25, 0.25, 0.25, 0.25}; // 3 players: 3 steps + the NPC part (0.5 each, up to 2)
        for (int n = 0; n <= 8; n++) {
            assertEquals(without[n], NpcRules.bonus(0, 0, n * 0.5, 3, 0.4), 1e-9, n + " Brothers alone");
            assertEquals(with[n], NpcRules.bonus(3, 3, n * 0.5, 3, 0.4), 1e-9, n + " Brothers with 3 players");
        }
    }

    @Test
    void anOwnerAwayMeansTheMemberRests() {
        assertEquals(Order.GUARD, NpcRules.effectiveOrder(Order.GUARD, true, true));
        assertEquals(Order.FOLLOW, NpcRules.effectiveOrder(Order.FOLLOW, true, true));
        assertEquals(Order.REST, NpcRules.effectiveOrder(Order.FOLLOW, false, true), "offline");
        assertEquals(Order.REST, NpcRules.effectiveOrder(Order.GUARD, true, false), "dead");
        assertEquals(Order.REST, NpcRules.effectiveOrder(Order.GUARD, false, false));
    }

    @Test
    void theOwnerAndThePacksLeaderMayOrder() {
        assertTrue(NpcRules.mayOrder(true, false));
        assertTrue(NpcRules.mayOrder(false, true));
        assertFalse(NpcRules.mayOrder(false, false), "a pack mate who is not the leader, a stranger");
    }

    @Test
    void onlyABrotherTakesOurOrders() {
        assertTrue(NpcRules.hasOurOrders(Source.BROTHER));
        assertFalse(NpcRules.hasOurOrders(Source.TENSURA));
        assertFalse(NpcRules.hasOurOrders(Source.SPIRIT));
    }

    @Test
    void anOrderIsReadByName() {
        assertEquals(Order.GUARD, Order.of("guard"));
        assertEquals(Order.REST, Order.of("REST"));
        assertNull(Order.of("hunt"));
    }

    @Test
    void theBondSurvivesTheCodecAndEveryFieldIsOptional() {
        UUID owner = UUID.randomUUID();
        NpcBond b = new NpcBond(owner, Source.TENSURA, Order.GUARD, true, true, 12345L, 99L);
        assertEquals(b, NpcBond.CODEC.parse(JsonOps.INSTANCE, NpcBond.CODEC.encodeStart(JsonOps.INSTANCE, b).getOrThrow()).getOrThrow());
        NpcBond empty = NpcBond.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(NpcBond.NONE, empty);
        assertFalse(empty.bonded());
        assertTrue(b.bonded());
        assertEquals(Order.REST, b.withOrder(Order.REST, 0).order());
        assertTrue(NpcBond.brother(owner, 1, 2).source() == Source.BROTHER && NpcBond.tensura(owner, 1).source() == Source.TENSURA);
    }

    private static final ToIntLike CATEGORY = id -> id.startsWith("minecraft:diamond") ? 0 : id.startsWith("minecraft:bone") ? 1 : 2;

    private interface ToIntLike extends java.util.function.ToIntFunction<String> {
    }

    @Test
    void sortingMergesPartialStacksAndOrdersByCategoryThenId() {
        List<Slot> in = List.of(new Slot("minecraft:bone", 10, 64), new Slot("minecraft:stick", 3, 64), new Slot("minecraft:bone", 60, 64),
                new Slot("minecraft:diamond_sword", 1, 1), new Slot("minecraft:diamond_sword", 1, 1), new Slot("minecraft:stick", 0, 64));
        List<Slot> out = SortRules.sort(in, CATEGORY);
        assertEquals(List.of(new Slot("minecraft:diamond_sword", 1, 1), new Slot("minecraft:diamond_sword", 1, 1), new Slot("minecraft:bone", 64, 64),
                new Slot("minecraft:bone", 6, 64), new Slot("minecraft:stick", 3, 64)), out);
    }

    @Test
    void sortingNeverLosesOrInventsItems() {
        List<Slot> in = List.of(new Slot("a", 5, 16), new Slot("b", 7, 16), new Slot("a", 30, 16), new Slot("c", 16, 16));
        List<Slot> out = SortRules.sort(in, id -> 0);
        assertEquals(58, out.stream().mapToInt(Slot::count).sum());
        assertEquals(35, out.stream().filter(s -> s.id().equals("a")).mapToInt(Slot::count).sum());
        assertTrue(out.stream().allMatch(s -> s.count() <= s.max()));
        assertEquals(List.of(), SortRules.sort(List.of(), id -> 0));
    }

    @Test
    void aBrotherHuntsOnlyAtNightOnPostAfterItsPeriodAndNotInAFight() {
        long period = 12000;
        assertTrue(NpcRules.huntDue(true, true, Order.GUARD, true, period, period, false));
        assertTrue(NpcRules.huntDue(true, true, Order.REST, true, Long.MAX_VALUE, period, false), "the first trip is due at once");
        assertFalse(NpcRules.huntDue(false, true, Order.GUARD, true, period, period, false), "the switch");
        assertFalse(NpcRules.huntDue(true, false, Order.GUARD, true, period, period, false), "Hunt off");
        assertFalse(NpcRules.huntDue(true, true, Order.FOLLOW, true, period, period, false), "a follower has no post to come back to");
        assertFalse(NpcRules.huntDue(true, true, Order.GUARD, false, period, period, false), "daytime");
        assertFalse(NpcRules.huntDue(true, true, Order.GUARD, true, period - 1, period, false), "too soon");
        assertFalse(NpcRules.huntDue(true, true, Order.GUARD, true, period, period, true), "in a fight");
    }

    @Test
    void aTensuraSubordinateJoinsOnlyOneStepDownAndWithinTheCap() {
        assertTrue(NpcRules.tensuraJoins(true, true, true, false, false, 0, 6), "a subordinate of a pack member");
        assertTrue(NpcRules.tensuraJoins(true, true, false, true, false, 5, 6), "a subordinate of a Brother of a pack member, the last place");
        assertFalse(NpcRules.tensuraJoins(true, true, false, false, false, 0, 6), "the subordinate of a subordinate, or of nobody in a pack");
        assertFalse(NpcRules.tensuraJoins(false, true, true, false, false, 0, 6), "the bridge is switched off");
        assertFalse(NpcRules.tensuraJoins(true, false, true, false, false, 0, 6), "an untamed one");
        assertFalse(NpcRules.tensuraJoins(true, true, true, false, false, 6, 6), "the pack is full");
        assertTrue(NpcRules.tensuraJoins(true, true, true, false, true, 6, 6), "a member is not put out for the cap");
        assertFalse(NpcRules.tensuraJoins(true, true, true, false, false, 0, 0), "a cap of 0 is nobody");
    }

    @Test
    void aMemberNeverTakesTheSpark() {
        assertTrue(NpcRules.mayTakeSpark(true, false));
        assertFalse(NpcRules.mayTakeSpark(false, true), "a Brother that kills an Alpha");
        assertFalse(NpcRules.mayTakeSpark(false, false), "any other mob");
        assertFalse(NpcRules.mayTakeSpark(true, true), "never a member, whatever it is");
    }
}