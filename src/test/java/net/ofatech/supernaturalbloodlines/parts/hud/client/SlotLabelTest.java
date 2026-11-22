package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import org.junit.jupiter.api.Test;

class SlotLabelTest {
    private static final String KEY = "supernaturalbloodlines.hud.slot.";
    private static final String[] REASONS = {"locked", "cannot_use", "skill_missing", "action_missing"};
    private static final int[][] SCREENS = {{480, 270}, {640, 360}, {960, 540}};

    /** A stand-in for the font: 6 px a character (Minecraft's average is a little under, so this is the cautious side). */
    static int width(String s) {
        return s.length() * 6;
    }

    private static JsonObject lang() throws Exception {
        try (InputStream in = SlotLabelTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            assertNotNull(in);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static SlotLabel.Label of(String name, boolean blocked, String state, String mastery, String detail, String description, int room) {
        return SlotLabel.of(name, blocked, state, mastery, detail, description, room, SlotLabelTest::width);
    }

    @Test
    void theNameIsTheFirstLineAndTheSecondHoldsStateMasteryModeAndDescriptionInThatOrder() {
        assertArrayEquals(new String[] {"Fireball", ""}, of("Fireball", false, null, null, null, null, 500).lines());
        assertArrayEquals(new String[] {"Fireball", "Ready in 4 s \u00b7 42% \u00b7 Blade mode \u00b7 Throws fire"},
                of("Fireball", false, "Ready in 4 s", "42%", "Blade mode", "Throws fire", 500).lines());
        assertArrayEquals(new String[] {"Fireball", "Mastered \u00b7 Throws fire"}, of("Fireball", false, " ", "Mastered", null, "Throws fire", 500).lines());
        assertFalse(of("Fireball", false, "Ready in 4 s", "42%", "Blade mode", "Throws fire", 500).cut());
    }

    @Test
    void aLabelIsAtMostTwoLinesWhateverItHolds() {
        for (boolean blocked : new boolean[] {false, true}) {
            for (int room : new int[] {24, 60, 90, 200, 500}) {
                SlotLabel.Label l = of("A very long skill name indeed", blocked, "You cannot use this skill now.", "Learning 100%", "Another long mode name", "A rather long description", room);
                assertTrue(l.lines().length <= SlotLabel.MAX_LINES, l.lines().length + " lines at room " + room);
                for (String line : l.lines()) {
                    assertTrue(width(line) <= room, "\"" + line + "\" is wider than " + room);
                }
            }
        }
    }

    @Test
    void aBlockedSlotIsItsNameAndItsShortReasonAndNothingElse() {
        SlotLabel.Label l = of("Poison Resistance", true, "Can't use now", "Learning 60%", null, "Resists poison", 200);
        assertArrayEquals(new String[] {"Poison Resistance", "Can't use now"}, l.lines());
        assertFalse(l.cut());
    }

    @Test
    void whatDoesNotFitIsSaidToBeCutSoTheColumnCanShowItWhole() {
        assertTrue(of("Poison Resistance", false, null, null, null, null, 60).cut(), "the name");
        assertTrue(of("Poison Resistance", false, null, "42%", null, "Resists every kind of poison there is", 120).cut(), "the description");
        assertTrue(of("Bat", true, "This action is not unlocked for your faction.", null, null, null, 90).cut(), "the reason");
        assertFalse(of("Predator", false, null, null, null, null, 60).cut());
    }

    @Test
    void everyShortReasonIsAtMostFourteenCharactersAndKeepsItsFullSentenceUnderLong() throws Exception {
        JsonObject lang = lang();
        for (String r : REASONS) {
            String shortText = lang.get(KEY + r).getAsString();
            assertTrue(shortText.length() <= 14, r + " is \"" + shortText + "\"");
            String full = lang.get(KEY + r + SlotLabel.LONG).getAsString();
            assertTrue(full.length() > shortText.length() && full.endsWith("."), r + " keeps a sentence: " + full);
        }
    }

    /** The box a label is drawn in: level with its slot, {@code lines} tall. */
    private static HudLayout.Box drawn(HudLayout.Box box, int lines) {
        int h = HudLayout.drawnHeight(lines);
        return new HudLayout.Box(box.x(), box.y() + (box.h() - h) / 2, box.w(), h);
    }

    @Test
    void neighbouringLabelsNeverOverlapWithTheRealReasonsAtGuiScales2To4() throws Exception {
        JsonObject lang = lang();
        for (int[] s : SCREENS) {
            HudLayout.Box[] boxes = HudLayout.labels(s[0], s[1]);
            for (int col = 0; col < 2; col++) {
                for (String reason : REASONS) {
                    HudLayout.Box[] shown = new HudLayout.Box[3];
                    for (int i = 0; i < 3; i++) {
                        HudLayout.Box box = boxes[col * 3 + i];
                        int room = box.w() - SlotLabel.PAD * 2;
                        SlotLabel.Label l = of("Poison Resistance", true, lang.get(KEY + reason).getAsString(), "Learning 60%", "Blade mode", "Resists poison", room);
                        assertTrue(l.lines().length <= SlotLabel.MAX_LINES);
                        shown[i] = drawn(box, l.lines().length);
                    }
                    for (int i = 0; i + 1 < 3; i++) {
                        assertFalse(shown[i].overlaps(shown[i + 1]), reason + " labels " + i + " and " + (i + 1) + " overlap at " + s[0] + "x" + s[1] + ": " + shown[i] + " " + shown[i + 1]);
                    }
                }
            }
        }
    }

    @Test
    void twoNeighbouringBlockedSlotsDoNotCoverEachOther() throws Exception {
        JsonObject lang = lang();
        for (int[] s : SCREENS) {
            HudLayout.Box[] boxes = HudLayout.labels(s[0], s[1]);
            int room = boxes[4].w() - SlotLabel.PAD * 2;
            SlotLabel.Label middle = of("Moon Howl", true, lang.get(KEY + "locked").getAsString(), null, null, null, room);
            SlotLabel.Label bottom = of("Alpha Roar", true, lang.get(KEY + "cannot_use").getAsString(), null, null, null, room);
            assertFalse(drawn(boxes[4], middle.lines().length).overlaps(drawn(boxes[5], bottom.lines().length)), s[0] + "x" + s[1]);
            assertFalse(middle.cut() || bottom.cut(), "both fit uncut at " + s[0] + "x" + s[1]);
        }
    }

    @Test
    void aThreeLineLabelWouldOverlapItsNeighbourSoTheCapIsWhatKeepsThemApart() {
        HudLayout.Box[] boxes = HudLayout.labels(480, 270);
        assertTrue(drawn(boxes[1], 3).overlaps(drawn(boxes[2], 2)), "the premise: three lines do not fit a slot's row");
        assertFalse(drawn(boxes[1], SlotLabel.MAX_LINES).overlaps(drawn(boxes[2], SlotLabel.MAX_LINES)));
    }

    @Test
    void aColumnWithACutLabelOrNoRoomShowsItsPanelAndTheOtherKeepsItsLabels() {
        SlotLabel.Label fits = of("Bite", false, null, null, null, null, 200);
        SlotLabel.Label cut = of("Poison Resistance", false, null, null, null, null, 60);
        assertArrayEquals(new boolean[] {false, false}, SlotLabel.fallbackColumns(new SlotLabel.Label[] {fits, null, fits, fits, null, fits}));
        assertArrayEquals(new boolean[] {true, false}, SlotLabel.fallbackColumns(new SlotLabel.Label[] {fits, cut, fits, fits, fits, fits}), "one cut label sends its whole column");
        assertArrayEquals(new boolean[] {false, true}, SlotLabel.fallbackColumns(new SlotLabel.Label[] {fits, fits, fits, null, null, SlotLabel.TOO_NARROW}));
    }

    @Test
    void thePercentIsRounded() {
        assertEquals(42, SlotLabel.percent(Mastery.of(42.4, 100, 0, false)));
        assertEquals(100, SlotLabel.percent(Mastery.of(100, 100, 0, true)));
        assertEquals(60, SlotLabel.percent(Mastery.of(-40, 100, 100, false)));
    }
}
