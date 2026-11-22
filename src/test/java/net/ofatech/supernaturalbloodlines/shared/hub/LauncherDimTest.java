package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LauncherDimTest {
    @Test
    void aDimmedSealKeepsItsAlphaAndMovesEachChannelTowardsGrey() {
        int dim = HubHooks.Launcher.dim(0xFFD01020);
        assertEquals(0xFF, dim >>> 24, "alpha");
        int r = (dim >> 16) & 0xFF;
        int g = (dim >> 8) & 0xFF;
        int b = dim & 0xFF;
        assertTrue(r < 0xD0 && r > 0x60, "red moves down towards grey: " + r);
        assertTrue(g > 0x10 && g < 0x60, "green moves up towards grey: " + g);
        assertTrue(b > 0x20 && b < 0x60, "blue moves up towards grey: " + b);
    }

    @Test
    void aGreyStaysGreyAndTheDimmedLauncherKeepsItsOtherFields() {
        assertEquals(0xFF606060, HubHooks.Launcher.dim(0xFF606060));
        var l = new HubHooks.Launcher("e", "E", 0xFFD01020, net.minecraft.network.chat.Component.literal("x"), "pkg", () -> true, () -> { });
        var d = l.dimmed();
        assertEquals(l.id(), d.id());
        assertEquals(l.letter(), d.letter());
        assertEquals(HubHooks.Launcher.dim(l.color()), d.color());
    }
}
