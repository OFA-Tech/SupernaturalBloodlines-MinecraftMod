package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

class HubRitesTest {
    private static HubSocial.Rite rite(String id, int power, String faction) {
        return new HubSocial.Rite("supernaturalbloodlines:" + id, power, List.of(), List.of(), "supernaturalbloodlines:faction_rite", faction);
    }

    private static final HubSocial.Rite MOON = rite("rite_of_the_moon", 15000, "vampire_or_werewolf");
    private static final HubSocial.Rite SILVER = rite("silver_moon", 4000, "werewolf");
    private static final HubSocial.Rite CLEANSING = rite("cleansing_blood", 4000, "vampire");
    private static final HubSocial.Rite FIVE = rite("rite_of_the_five", 10000, "hunter");
    private static final HubSocial.Rite HYBRID_BLOOD = rite("hybrid_blood", 1000, "");

    @Test
    void theFactionSideDecides() {
        assertTrue(HubRites.forYou(SILVER, List.of(HYBRID), Set.of()));
        assertTrue(HubRites.forYou(MOON, List.of(ORIGINAL), Set.of()));
        assertFalse(HubRites.forYou(CLEANSING, List.of(WEREWOLF), Set.of()));
        assertTrue(HubRites.forYou(FIVE, List.of(HUNTER), Set.of()));
        assertFalse(HubRites.forYou(HYBRID_BLOOD, List.of(VAMPIRE), Set.of()));
    }

    @Test
    void aRiteYourLadderAsksForIsYours() {
        String ladder = """
                { "rungs": [ { "id": "H-0", "name": "H", "sp": 0, "ep": 0 },
                  { "id": "H-1", "name": "V", "sp": 1, "ep": 0, "tasks": [
                    { "id": "H-1a", "detector": "rite", "rite": "supernaturalbloodlines:hybrid_blood", "role": "target" } ] } ] }
                """;
        LadderTable t = LadderTable.parse(Map.of("hybrid", JsonParser.parseString(ladder)), m -> true).table();
        Set<String> asked = HubRites.fromLadders(t, List.of(HYBRID));
        assertEquals(Set.of("supernaturalbloodlines:hybrid_blood"), asked);
        assertTrue(HubRites.forYou(HYBRID_BLOOD, List.of(HYBRID), asked));
    }

    @Test
    void yoursComeFirstThenByPower() {
        List<HubRites.Listed> l = HubRites.list(List.of(MOON, CLEANSING, SILVER, FIVE), List.of(WEREWOLF), Set.of());
        assertEquals(List.of(SILVER, MOON, CLEANSING, FIVE), l.stream().map(HubRites.Listed::rite).toList());
        assertEquals(List.of(true, true, false, false), l.stream().map(HubRites.Listed::forYou).toList());
    }
}
