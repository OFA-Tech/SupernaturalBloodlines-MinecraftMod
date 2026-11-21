package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.hud.MasteryBar;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import org.junit.jupiter.api.Test;

/** Every text colour of the HUD on the panel it is drawn on (the mode pill, the tooltips, the tracker) is readable, in all three looks. */
class HudContrastTest {
    private static final int MUTED = 0xFFB8B0B4;
    private static final int BAD = 0xFFFF7A70;

    @Test
    void panelTextIsReadableInEveryLook() {
        for (HudStyle.Style s : HudStyle.Style.values()) {
            int face = 0xFF000000 | HudChrome.face(s);
            int[][] pairs = {{HudChrome.ink(s), face}, {MUTED, face}, {BAD, face}};
            String[] what = {"ink", "muted", "refusal"};
            for (int i = 0; i < pairs.length; i++) {
                assertTrue(Contrast.ratio(pairs[i][0], pairs[i][1]) >= 4.5,
                        s + " " + what[i] + " is " + Contrast.ratio(pairs[i][0], pairs[i][1]) + ":1 on the panel");
            }
        }
    }

    @Test
    void everyMasteryBarFillIsAtLeast3To1AgainstItsTrack() {
        for (SlotBadge.Look look : SlotBadge.Look.values()) {
            for (Mastery.State state : Mastery.State.values()) {
                double ratio = Contrast.ratio(MasteryBar.color(look, state), MasteryBar.TRACK_INNER);
                assertTrue(ratio >= 3.0, look + " " + state + " fill is " + ratio + ":1 against the track");
            }
        }
    }

    @Test
    void theKeyLettersAreReadableOnTheirBadge() {
        // Grimoire: pale letter on the red wax, Tensura: teal on the dark tab, Blood Moon: gold on the slot face
        assertTrue(Contrast.ratio(0xFFF4E2C0, 0xFF8E1A1A) >= 4.5, "wax badge");
        assertTrue(Contrast.ratio(0xFF3FD0D6, 0xFF0A1A28) >= 4.5, "tensura tab");
        assertTrue(Contrast.ratio(0xFFFFD36B, 0xFF140810) >= 4.5, "blood moon letter");
    }
}
