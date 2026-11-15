package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubReturn.OnScreen;
import org.junit.jupiter.api.Test;

class HubReturnTest {
    @Test
    void escOutOfTheLaunchedScreenReopensTheSamePageOnce() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.BLOODLINE);
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER)); // the other mod's screen is open
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER)); // still in it (its own tabs)
        assertEquals(Optional.of(HubPage.BLOODLINE), r.tick(OnScreen.NONE));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE)); // once
        assertFalse(r.waiting());
    }

    @Test
    void openingTheHubAnotherWayForgetsTheReturn() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.PATH);
        assertEquals(Optional.empty(), r.tick(OnScreen.HUB));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE));
    }

    @Test
    void nothingLaunchedNothingReopens() {
        assertEquals(Optional.empty(), new HubReturn().tick(OnScreen.NONE));
    }
}
