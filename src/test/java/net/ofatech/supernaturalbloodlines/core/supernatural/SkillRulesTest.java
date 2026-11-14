package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SkillRulesTest {
    private static final SupernaturalRace WOLF = SupernaturalRace.WEREWOLF;
    private static final Optional<SupernaturalRace> WITCH = Optional.of(SupernaturalRace.WITCH);

    private static JsonElement json(String s) {
        return JsonParser.parseString(s);
    }

    private static String rule(String id, int cost) {
        return "{\"id\":\"" + id + "\",\"cost\":" + cost + ",\"kinds\":[\"rule\"]}";
    }

    /** Werewolf: S-1 (rung 1), S-2 (rung 2, after S-1), S-Q (rung 1, quest Q-1). Witch: S-1 (rung 3) and S-W (rung 1). */
    private static final SkillTable TABLE = SkillTable.parse(Map.of(
                    "s-1", json(rule("S-1", 2)), "s-2", json(rule("S-2", 3)), "s-q", json(rule("S-Q", 1)), "s-w", json(rule("S-W", 1)),
                    "s-g", json("{\"id\":\"S-G\",\"cost\":0,\"granted_only\":true,\"kinds\":[\"rule\"]}")),
            Map.of("werewolf", json("{\"skills\":[{\"skill\":\"S-1\",\"rung\":1},{\"skill\":\"S-2\",\"rung\":2,\"after\":\"S-1\"},"
                            + "{\"skill\":\"S-Q\",\"rung\":1,\"quest\":\"Q-1\"}]}"),
                    "witch", json("{\"skills\":[{\"skill\":\"S-1\",\"rung\":3},{\"skill\":\"S-W\",\"rung\":1}]}")),
            mod -> true).table();

    private static SupernaturalState state(int sp, int wolfRung, int witchRung, String... owned) {
        return SupernaturalState.NONE.toBuilder().sp(sp).spEarned(sp)
                .rung(WOLF, wolfRung).rung(SupernaturalRace.WITCH, witchRung).skills(Set.of(owned)).build();
    }

    private static SkillRules.BuyResult buy(SupernaturalState s, String id) {
        return SkillRules.buy(TABLE, s, WOLF, WITCH, id);
    }

    @Test
    void aPurchasePaysAndOwns() {
        SkillRules.BuyResult r = buy(state(5, 1, 0), "S-1");
        assertTrue(r.ok());
        assertNull(r.refusal());
        assertEquals(3, r.state().sp());
        assertEquals(5, r.state().spEarned());
        assertEquals(Set.of("S-1"), r.state().skills());
    }

    @Test
    void eachRefusalHasItsOwnReason() {
        assertEquals(SkillRules.Refusal.UNKNOWN_SKILL, buy(state(9, 9, 9), "S-NOPE").refusal());
        assertEquals(SkillRules.Refusal.GRANTED_ONLY, buy(state(9, 9, 9), "S-G").refusal());
        assertEquals(SkillRules.Refusal.ALREADY_OWNED, buy(state(9, 9, 9, "S-1"), "S-1").refusal());
        assertEquals(SkillRules.Refusal.NOT_IN_TREE, SkillRules.buy(TABLE, state(9, 9, 9), SupernaturalRace.VAMPIRE, Optional.empty(), "S-1").refusal());
        assertEquals(SkillRules.Refusal.RUNG, buy(state(9, 0, 0), "S-1").refusal());
        assertEquals(SkillRules.Refusal.PREREQUISITE, buy(state(9, 2, 0), "S-2").refusal());
        assertEquals(SkillRules.Refusal.QUEST, buy(state(9, 1, 0), "S-Q").refusal());
        assertEquals(SkillRules.Refusal.SP, buy(state(1, 1, 0), "S-1").refusal());
        assertTrue(buy(state(9, 2, 0, "S-1"), "S-2").ok());
        assertTrue(buy(state(9, 1, 0).toBuilder().quests(Set.of("Q-1")).build(), "S-Q").ok());
    }

    @Test
    void aRefusalChangesNothingAndTheKeyIsFilled() {
        SupernaturalState before = state(1, 1, 0);
        SkillRules.BuyResult r = buy(before, "S-1");
        assertFalse(r.ok());
        assertEquals(before, r.state());
        for (SkillRules.Refusal refusal : SkillRules.Refusal.values()) {
            assertTrue(refusal.langKey().startsWith("supernaturalbloodlines.supernatural.skill.refuse."), refusal.langKey());
        }
    }

    @Test
    void aGhoulBuysNothing() {
        SupernaturalState s = state(9, 9, 9);
        assertEquals(SkillRules.Refusal.GHOUL_FROZEN, SkillRules.buy(TABLE, s, SupernaturalRace.GHOUL, Optional.empty(), "S-1").refusal());
        assertEquals(SkillRules.Refusal.GHOUL_FROZEN, SkillRules.buy(TABLE, s, SupernaturalRace.GHOUL, WITCH, "S-W").refusal());
        assertEquals(SkillRules.Refusal.GHOUL_FROZEN, SkillRules.buy(TABLE, s, SupernaturalRace.GHOUL, Optional.empty(), "S-NOPE").refusal());
    }

    @Test
    void aSkillInBothTreesIsBoughtOnceAndMeetsEitherTree() {
        // the rung 3 of the witch tree is not reached, but the werewolf tree asks only for 1
        SkillRules.BuyResult first = buy(state(9, 1, 0), "S-1");
        assertTrue(first.ok());
        assertEquals(7, first.state().sp());
        assertEquals(SkillRules.Refusal.ALREADY_OWNED, buy(first.state(), "S-1").refusal());
        // reaching it only in the witch tree is enough as well
        assertTrue(buy(state(9, 0, 3), "S-1").ok());
        assertEquals(Set.of("S-1"), SkillRules.active(TABLE, first.state(), WOLF, WITCH));
        assertEquals(Set.of("S-1"), SkillRules.active(TABLE, first.state(), WOLF, Optional.empty()));
        assertEquals(Set.of("S-1"), SkillRules.active(TABLE, first.state(), SupernaturalRace.WITCH, Optional.empty()));
    }

    @Test
    void aSkillIsActiveOnlyWhileATreeOfThePlayerListsIt() {
        SupernaturalState s = state(0, 5, 5, "S-1", "S-W", "S-G");
        assertEquals(Set.of("S-1", "S-W", "S-G"), SkillRules.active(TABLE, s, WOLF, WITCH));
        assertEquals(Set.of("S-1", "S-G"), SkillRules.active(TABLE, s, WOLF, Optional.empty()));
        assertEquals(Set.of("S-G"), SkillRules.active(TABLE, s, SupernaturalRace.VAMPIRE, Optional.empty()));
        assertEquals(Set.of(), SkillRules.active(TABLE, s, SupernaturalRace.GHOUL, WITCH));
        assertTrue(SkillRules.owns(TABLE, s, WOLF, Optional.empty(), "S-1"));
        assertFalse(SkillRules.owns(TABLE, s, WOLF, Optional.empty(), "S-W"));
        assertFalse(SkillRules.owns(TABLE, s, WOLF, WITCH, "S-2"));
    }
}
