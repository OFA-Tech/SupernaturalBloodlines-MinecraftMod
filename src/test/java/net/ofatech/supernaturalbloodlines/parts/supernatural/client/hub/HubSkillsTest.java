package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubSkills.Row;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubSkills.State;
import org.junit.jupiter.api.Test;

class HubSkillsTest {
    private static JsonElement json(String s) {
        return JsonParser.parseString(s);
    }

    private static String rule(String id, int cost) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"cost\":" + cost + ",\"kinds\":[\"rule\"]}";
    }

    /** Werewolf: S-1 (rung 1), S-2 (rung 2, after S-1). Witch: S-1 (rung 3) and S-W (rung 1). Vampire: S-V. */
    private static final SkillTable TABLE = SkillTable.parse(Map.of(
                    "s-1", json(rule("S-1", 2)), "s-2", json(rule("S-2", 3)), "s-w", json(rule("S-W", 1)), "s-v", json(rule("S-V", 1))),
            Map.of("werewolf", json("{\"skills\":[{\"skill\":\"S-1\",\"rung\":1},{\"skill\":\"S-2\",\"rung\":2,\"after\":\"S-1\"}]}"),
                    "witch", json("{\"skills\":[{\"skill\":\"S-1\",\"rung\":3},{\"skill\":\"S-W\",\"rung\":1}]}"),
                    "vampire", json("{\"skills\":[{\"skill\":\"S-V\",\"rung\":0}]}")),
            mod -> true).table();

    private static SupernaturalState state(int sp, int wolfRung, String... owned) {
        return SupernaturalState.NONE.toBuilder().sp(sp).rung(WEREWOLF, wolfRung).skills(Set.of(owned)).build();
    }

    private static Map<String, Row> byId(List<Row> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(Row::id, r -> r));
    }

    @Test
    void aWitchWerewolfSeesBothTreesAndASharedSkillOnce() {
        List<Row> rows = HubSkills.rows(TABLE, state(5, 1), List.of(WEREWOLF, WITCH));
        assertEquals(List.of("S-1", "S-2", "S-W"), rows.stream().map(Row::id).toList());
        assertEquals(WEREWOLF, rows.get(0).race());
    }

    @Test
    void statesFollowTheRulesAndCarryTheirReason() {
        Map<String, Row> rows = byId(HubSkills.rows(TABLE, state(5, 1), List.of(WEREWOLF, WITCH)));
        assertEquals(State.BUYABLE, rows.get("S-1").state());
        assertNull(rows.get("S-1").why());
        assertEquals(State.LOCKED, rows.get("S-2").state());
        assertEquals(SkillRules.Refusal.RUNG, rows.get("S-2").why());
        assertEquals(State.LOCKED, rows.get("S-W").state()); // the witch is on rung 0
        Map<String, Row> poor = byId(HubSkills.rows(TABLE, state(1, 1), List.of(WEREWOLF)));
        assertEquals(SkillRules.Refusal.SP, poor.get("S-1").why());
    }

    @Test
    void ownedSkillsAreOwnedOrDormant() {
        // S-V was bought as a vampire: as a werewolf no tree lists it, so it is dormant (and kept)
        Map<String, Row> rows = byId(HubSkills.rows(TABLE, state(0, 2, "S-1", "S-V"), List.of(WEREWOLF)));
        assertEquals(State.OWNED, rows.get("S-1").state());
        assertEquals(State.DORMANT, rows.get("S-V").state());
        assertNull(rows.get("S-V").race());
    }

    @Test
    void aGrantedOnlySkillThatCountsIsOwnedNotDormant() {
        SkillTable granted = SkillTable.parse(Map.of(
                        "s-1", json(rule("S-1", 2)),
                        "s-g", json("{\"id\":\"S-G\",\"name\":\"S-G\",\"cost\":0,\"granted_only\":true,\"kinds\":[\"rule\"]}")),
                Map.of("werewolf", json("{\"skills\":[{\"skill\":\"S-1\",\"rung\":1}]}")),
                mod -> true).table();
        Map<String, Row> rows = byId(HubSkills.rows(granted, state(0, 1, "S-G"), List.of(WEREWOLF)));
        assertEquals(State.OWNED, rows.get("S-G").state(), "a granted-only skill that counts shows as dormant");
        assertNull(rows.get("S-G").race());
    }

    @Test
    void theRowOffersAPurchaseExactlyWhenTheServerWouldAcceptIt() {
        for (SupernaturalRace creature : List.of(WEREWOLF, GHOUL, VAMPIRE)) {
            SupernaturalState s = state(3, 1, "S-V");
            List<SupernaturalRace> races = List.of(creature, WITCH);
            for (Row r : HubSkills.rows(TABLE, s, races)) {
                boolean server = SkillRules.buy(TABLE, s, creature, Optional.of(WITCH), r.id()).ok();
                assertEquals(server, r.state() == State.BUYABLE, creature + " " + r.id());
            }
        }
    }

    @Test
    void aSkillThePlayerHasOrHasTheLikeOfIsNotOfferedAgain() {
        assertEquals(false, HubSkills.State.OWNED.offersPurchase());
        assertEquals(false, HubSkills.State.COVERED.offersPurchase());
        assertEquals(true, HubSkills.State.BUYABLE.offersPurchase());
        // a locked or dormant one still shows its (disabled) button: it says why it cannot be learned
        assertEquals(true, HubSkills.State.LOCKED.offersPurchase());
        assertEquals(true, HubSkills.State.DORMANT.offersPurchase());
    }}
