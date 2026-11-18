package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

/** A switch reads "On" or "Off": its tooltip and narration must say which setting that is about. */
class ToggleNameTest {
    @Test
    void theNarratedNameIsTheSettingThenItsState() {
        // no language is loaded in a unit test, so the state shows as its key
        assertEquals("Protect my True Alpha path: supernaturalbloodlines.hub.switch.on", PageWidgets.toggleName(Component.literal("Protect my True Alpha path"), true).getString());
        assertEquals("Protect my True Alpha path: supernaturalbloodlines.hub.switch.off", PageWidgets.toggleName(Component.literal("Protect my True Alpha path"), false).getString());
    }
}
