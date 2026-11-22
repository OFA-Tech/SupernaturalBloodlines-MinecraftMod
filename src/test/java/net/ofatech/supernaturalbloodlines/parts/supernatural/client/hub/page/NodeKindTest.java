package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubSkills.State;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.NodeLook.Kind;
import org.junit.jupiter.api.Test;

class NodeKindTest {
    @Test
    void aSkillThePlayerHasIsOwnedAnUnreachableOneIsLockedAndABuyableOneIsAvailable() {
        assertEquals(Kind.OWNED, BookSkillsPage.kindOf(State.OWNED));
        assertEquals(Kind.OWNED, BookSkillsPage.kindOf(State.COVERED));
        assertEquals(Kind.LOCKED, BookSkillsPage.kindOf(State.LOCKED));
        assertEquals(Kind.LOCKED, BookSkillsPage.kindOf(State.DORMANT));
        assertEquals(Kind.AVAILABLE, BookSkillsPage.kindOf(State.BUYABLE));
    }
}
