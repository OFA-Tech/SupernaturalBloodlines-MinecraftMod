package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class DetailTest {
    private static Component c(String s) {
        return Component.literal(s);
    }

    @Test
    void aDetailHoldsWhatAPageSaysAboutItsSelection() {
        Detail d = Detail.of(c("Venom Fang")).subtitle(c("Hybrid")).source(SourceTag.SB).pips(2, 3).body(c("Your bite poisons.")).field(c("Next rank"), c("3 SP"))
                .reward(c("+3 SP")).action("learn", c("Learn"), true, true, () -> { }).action("hud", c("Put in a HUD slot"), false, true, () -> { }).build();
        assertEquals(2, d.pipsFilled());
        assertEquals(3, d.pips());
        assertEquals(2, d.actions().size());
        assertEquals(1, d.fields().size());
    }

    @Test
    void onlyOneActionIsThePrimaryOne() {
        assertThrows(IllegalArgumentException.class, () -> Detail.of(c("x")).action("a", c("A"), true, true, () -> { }).action("b", c("B"), true, true, () -> { }).build());
    }

    @Test
    void pipsAreClamped() {
        Detail d = Detail.of(c("x")).pips(9, 3).build();
        assertEquals(3, d.pipsFilled());
        assertEquals(0, Detail.of(c("x")).pips(-4, -2).build().pips());
    }
}
