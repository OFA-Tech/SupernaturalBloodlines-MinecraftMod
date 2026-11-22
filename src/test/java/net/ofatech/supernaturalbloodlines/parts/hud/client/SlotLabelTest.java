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
    /** 1920 x 1080 at GUI scales 4, 3 and 2: the sizes of the shots. (A 1280 x 720 window at scale 3 is 427 wide and leaves a label 53 px: its columns show the panel at once.) */
    private static final int[][] SCREENS = {{480, 270}, {640, 360}, {960, 540}};
    private static final String COOLDOWN = "Ready in %s s";

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

    private static SlotLabel.Label of(String name, boolean blocked, String state, String stateWidest, String mastery, String masteryWidest, int room) {
        return SlotLabel.of(name, blocked, state, stateWidest, mastery, masteryWidest, room, SlotLabelTest::width);
    }

    @Test
    void aLabelIsTheNameAndTheStateAndNothingElse() {
        assertArrayEquals(new String[] {"Fireball", ""}, of("Fireball", false, null, null, null, null, 500).lines());
        assertArrayEquals(new String[] {"Fireball", "Ready in 4 s"}, of("Fireball", false, "Ready in 4 s", null, null, null, 500).lines());
        assertArrayEquals(new String[] {"Fireball", "Mastered"}, of("Fireball", false, " ", null, "Mastered", null, 500).lines());
        assertFalse(of("Fireball", false, "Ready in 4 s", null, null, null, 500).cut());
    }

    @Test
    void theMasteryFollowsTheStateOnlyWhereBothFitAtTheirWidest() {
        assertArrayEquals(new String[] {"Fireball", "Ready in 4 s \u00b7 42%"}, of("Fireball", false, "Ready in 4 s", "Ready in 99 s", "42%", "100%", 200).lines());
        // 200 would hold "Ready in 99 s \u00b7 100%" (20 characters: 120): 100 does not, so the state stands alone
        assertArrayEquals(new String[] {"Fireball", "Ready in 4 s"}, of("Fireball", false, "Ready in 4 s", "Ready in 99 s", "42%", "100%", 100).lines());
        // a blocked slot says why and nothing more
        assertArrayEquals(new String[] {"Fireball", "Can't use now"}, of("Fireball", true, "Can't use now", null, "42%", "100%", 500).lines());
    }

    @Test
    void aLabelIsAtMostTwoLinesAndNeverWiderThanItsRoom() {
        for (boolean blocked : new boolean[] {false, true}) {
            for (int room : new int[] {24, 60, 90, 200, 500}) {
                SlotLabel.Label l = of("A very long skill name indeed", blocked, "You cannot use this skill now.", null, "Learning 100%", null, room);
                assertTrue(l.lines().length <= SlotLabel.MAX_LINES, l.lines().length + " lines at room " + room);
                for (String line : l.lines()) {
                    assertTrue(width(line) <= room, "\"" + line + "\" is wider than " + room);
                }
            }
        }
    }

    @Test
    void aLongNameEndsInAnEllipsisAndDoesNotSendTheColumnToThePanel() {
        SlotLabel.Label l = of("Poison Resistance", false, "Ready in 4 s", "Ready in 99 s", null, null, 90);
        assertTrue(l.lines()[0].endsWith("\u2026"), l.lines()[0]);
        assertFalse(l.cut(), "the state fits: the full name is in the panel");
    }

    @Test
    void aStateThatDoesNotFitIsSaidToBeCutSoTheColumnCanShowItsPanel() {
        assertTrue(of("Bat", true, "This action is not unlocked for your faction.", null, null, null, 90).cut(), "a long reason");
        assertTrue(of("Bat", false, "Ready in 4 s", null, null, null, 30).cut(), "a state in a label with no room");
        assertTrue(of("Bat", false, null, null, "Learning 60%", null, 30).cut(), "a mastery alone");
        assertFalse(of("Predator", false, null, null, null, null, 30).cut(), "no state, nothing to cut");
    }

    @Test
    void aCountdownDoesNotMakeALabelSwitchToThePanelAndBackAsItRunsDown() {
        // room for "Ready in 9 s" (12 characters: 72) but not for "Ready in 99 s" (78): the label is judged by the widest, whatever the clock says now
        for (String now : new String[] {"Ready in 9 s", "Ready in 10 s", "Ready in 99 s"}) {
            assertTrue(of("Bite", false, now, "Ready in 99 s", null, null, 75).cut(), now + " is judged as 99 s");
        }
        for (String now : new String[] {"Ready in 9 s", "Ready in 99 s"}) {
            assertFalse(of("Bite", false, now, "Ready in 99 s", null, null, 78).cut(), now + " fits when 99 s does");
        }
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
    void atGuiScales2To4EverySlotGetsItsOwnLabelWithNoneCutAndNoneOnItsNeighbour() throws Exception {
        JsonObject lang = lang();
        String learned = lang.get("supernaturalbloodlines.hud.slot.learned").getAsString();
        for (int[] s : SCREENS) {
            HudLayout.Box[] boxes = HudLayout.labels(s[0], s[1]);
            for (String reason : REASONS) {
                SlotLabel.Label[] labels = new SlotLabel.Label[6];
                HudLayout.Box[] shown = new HudLayout.Box[6];
                for (int i = 0; i < 6; i++) {
                    int room = boxes[i].w() - SlotLabel.PAD * 2;
                    boolean blocked = i % 2 == 1;
                    labels[i] = of(i % 3 == 0 ? "Poison Resistance" : "Predator's Thirst", blocked, blocked ? lang.get(KEY + reason).getAsString() : String.format(COOLDOWN, 7), blocked ? null : String.format(COOLDOWN, 99),
                            String.format(learned.replace("%s", "%d"), 88), String.format(learned.replace("%s", "%d"), 100), room);
                    assertFalse(labels[i].cut(), s[0] + "x" + s[1] + " slot " + i + " " + reason + ": " + String.join(" | ", labels[i].lines()));
                    shown[i] = drawn(boxes[i], labels[i].lines().length);
                }
                assertArrayEquals(new boolean[] {false, false}, SlotLabel.fallbackColumns(labels), s[0] + "x" + s[1] + ": no column needs its panel just to be read");
                for (int col = 0; col < 2; col++) {
                    for (int i = col * 3; i < col * 3 + 2; i++) {
                        assertFalse(shown[i].overlaps(shown[i + 1]), s[0] + "x" + s[1] + " " + reason + ": labels " + i + " and " + (i + 1) + " overlap");
                    }
                }
            }
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
        SlotLabel.Label fits = of("Bite", false, "Ready in 4 s", null, null, null, 200);
        SlotLabel.Label cut = of("Bite", false, "Ready in 4 s", null, null, null, 30);
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
