package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.network.chat.ClickEvent;
import org.junit.jupiter.api.Test;

class ProfileLinkTest {
    @Test
    void aNameLinksToTheClientOnlyCommandWithTheIdAndTheNameItShows() {
        UUID id = UUID.randomUUID();
        var c = ProfileLink.name("Hayley Marshall", id);
        assertEquals("Hayley Marshall", c.getString());
        assertEquals(ClickEvent.Action.RUN_COMMAND, c.getStyle().getClickEvent().getAction());
        assertEquals("/sbprofile " + id + " Hayley Marshall", c.getStyle().getClickEvent().getValue());
    }
}

