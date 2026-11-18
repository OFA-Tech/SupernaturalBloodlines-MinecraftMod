package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

/** A switch reads "On" or "Off": its tooltip and narration must say which setting that is about. */
class ToggleNameTest {
    @Test
    void theNarratedNameHasTheSettingAndItsState() {
        Component on = PageWidgets.toggleName(Component.literal("Protect my True Alpha path"), true);
        Component off = PageWidgets.toggleName(Component.literal("Protect my True Alpha path"), false);
        assertTrue(on.getString().contains("Protect my True Alpha path"), on.getString());
        assertTrue(off.getString().contains("Protect my True Alpha path"), off.getString());
        assertNotEquals(on.getString(), off.getString(), "the state is part of the name");
    }
}
