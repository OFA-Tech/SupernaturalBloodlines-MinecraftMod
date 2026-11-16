package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SourceTagTest {
    @Test
    void everyModHasItsTag() {
        assertEquals(SourceTag.VMP, SourceTag.of("vampirism"));
        assertEquals(SourceTag.WW, SourceTag.of("werewolves"));
        assertEquals(SourceTag.ENC, SourceTag.of("enchanted"));
        assertEquals(SourceTag.TEN, SourceTag.of("Tensura"));
        assertEquals(SourceTag.SB, SourceTag.of("species"));
        assertEquals(SourceTag.SB, SourceTag.of(null));
        assertEquals(SourceTag.SB, SourceTag.of("some_other_mod"));
    }

    @Test
    void theLabelsAreTheDesignsAndTheSourcesAreWhatThemesColour() {
        assertEquals("SB VMP WW ENC TEN", String.join(" ", java.util.Arrays.stream(SourceTag.values()).map(SourceTag::label).toList()));
        for (SourceTag t : SourceTag.values()) {
            assertEquals(t, SourceTag.of(t.source()));
        }
    }
}
