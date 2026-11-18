package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class RequestToastFitTest {
    @Test
    void aLineThatFitsIsWholeAndOneThatDoesNotEndsInAnEllipsis() {
        List<String> shown = RequestToast.fitted(List.of(Component.literal("Klaus: Sire call"), Component.literal("A very long request name that cannot fit")), 30, String::length);
        assertEquals("Klaus: Sire call", shown.get(0));
        assertTrue(shown.get(1).endsWith("\u2026") && shown.get(1).length() <= 30, shown.get(1));
        assertTrue(shown.get(1).length() < "A very long request name that cannot fit".length());
    }
}

