package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class FocusOrderTest {
    @Test
    void tabGoesNavThenStageThenPanelThenActions() {
        List<String> registered = List.of("tab:PATH", "tab:SKILLS", "seal:vampirism", "skill:a", "detail:claim", "skill:b", "panel:note", "theme", "detail:track");
        assertEquals(List.of("tab:PATH", "tab:SKILLS", "seal:vampirism", "theme", "skill:a", "skill:b", "panel:note", "detail:claim", "detail:track"),
                FocusOrder.order(registered));
    }

    @Test
    void theOrderInsideAZoneIsTheOrderTheyWereDrawnIn() {
        assertEquals(List.of("a", "b", "c"), FocusOrder.order(List.of("a", "b", "c")));
        assertEquals(FocusOrder.Zone.STAGE, FocusOrder.zoneOf("task:x"));
        assertEquals(FocusOrder.Zone.NAV, FocusOrder.zoneOf("close"));
        assertEquals(FocusOrder.Zone.ACTIONS, FocusOrder.zoneOf("action:buy"));
    }

    @Test
    void numberKeysJumpToThePagesThatShow() {
        assertEquals(OptionalInt.of(0), FocusOrder.pageForKey(49, 6));
        assertEquals(OptionalInt.of(5), FocusOrder.pageForKey(54, 6));
        assertTrue(FocusOrder.pageForKey(55, 6).isEmpty(), "key 7 with six pages does nothing");
        assertEquals(OptionalInt.of(7), FocusOrder.pageForKey(328, 9));
        assertEquals(OptionalInt.of(2), FocusOrder.pageForKey(323, 8));
        assertEquals(OptionalInt.of(8), FocusOrder.pageForKey(57, 9), "9 is the ninth page after Home (Admin for an operator)");
        assertTrue(FocusOrder.pageForKey(57, 8).isEmpty(), "key 9 with eight pages does nothing");
        assertTrue(FocusOrder.pageForKey(48, 9).isEmpty(), "0 is Home, not a position");
        assertTrue(FocusOrder.isHomeKey(48) && FocusOrder.isHomeKey(320) && !FocusOrder.isHomeKey(49));
        assertTrue(FocusOrder.pageForKey(65, 8).isEmpty());
    }
}
