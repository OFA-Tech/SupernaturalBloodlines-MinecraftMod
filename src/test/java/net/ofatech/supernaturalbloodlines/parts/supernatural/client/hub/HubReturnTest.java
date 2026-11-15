package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubReturn.OnScreen;
import org.junit.jupiter.api.Test;

class HubReturnTest {
    private static final String TENSURA = "io.github.manasmods.tensura.client.screen.";

    @Test
    void escOutOfTheLaunchedScreenReopensTheSamePageOnce() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.BLOODLINE, TENSURA);
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER, TENSURA + "MainScreen")); // the other mod's screen is open
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER, TENSURA + "AbilityCategoriesScreen")); // its own tabs
        assertEquals(Optional.of(HubPage.BLOODLINE), r.tick(OnScreen.NONE, null));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE, null)); // once
        assertFalse(r.waiting());
    }

    @Test
    void openingTheHubAnotherWayForgetsTheReturn() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.PATH, TENSURA);
        assertEquals(Optional.empty(), r.tick(OnScreen.HUB, null));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE, null));
    }

    @Test
    void aScreenOutsideTheLaunchedModForgetsTheReturn() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.PATH, TENSURA);
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER, "net.minecraft.client.gui.screens.inventory.InventoryScreen"));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE, null)); // closing the inventory is not Esc out of Tensura
    }

    @Test
    void theDeathScreenForgetsTheReturn() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.SKILLS, "de.teamlapen.");
        assertEquals(Optional.empty(), r.tick(OnScreen.OTHER, "net.minecraft.client.gui.screens.DeathScreen"));
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE, null)); // respawning does not open the hub
    }

    @Test
    void leavingTheWorldForgetsTheReturn() {
        HubReturn r = new HubReturn();
        r.launched(HubPage.RITES, TENSURA);
        r.clear(); // LoggingOut
        assertEquals(Optional.empty(), r.tick(OnScreen.NONE, null));
    }

    @Test
    void nothingLaunchedNothingReopens() {
        assertEquals(Optional.empty(), new HubReturn().tick(OnScreen.NONE, null));
    }
}
