package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HubClientConfigNextTest {
    private static final HubClientConfig.ThemeId G = HubClientConfig.ThemeId.GRIMOIRE;
    private static final HubClientConfig.ThemeId B = HubClientConfig.ThemeId.BLOOD_MOON;
    private static final HubClientConfig.ThemeId T = HubClientConfig.ThemeId.TENSURA;

    @Test
    void theButtonCyclesEveryLookThatCanBeDrawn() {
        assertEquals(B, HubClientConfig.next(G, id -> true));
        assertEquals(T, HubClientConfig.next(B, id -> true));
        assertEquals(G, HubClientConfig.next(T, id -> true));
    }

    @Test
    void withoutTensuraTheTensuraLookIsSkipped() {
        assertEquals(G, HubClientConfig.next(B, id -> !id.equals("tensura")));
        assertEquals(B, HubClientConfig.next(G, id -> !id.equals("tensura")));
    }

    @Test
    void theGrimoireAlwaysWorksEvenWhenNothingIsRegistered() {
        assertEquals(G, HubClientConfig.next(B, id -> false));
        assertEquals(G, HubClientConfig.next(G, id -> false));
    }
}
