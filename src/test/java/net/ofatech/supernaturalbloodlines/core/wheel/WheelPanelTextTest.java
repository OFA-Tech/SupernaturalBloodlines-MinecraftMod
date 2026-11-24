package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WheelPanelTextTest {
    private static final Object FONT = new Object();
    private final AtomicInteger measured = new AtomicInteger();

    private int measure(String s) {
        measured.incrementAndGet();
        return s.length() * 6;
    }

    @BeforeEach
    void fresh() {
        WheelPanelText.clear();
    }

    private WheelPanelText.Lines frame(String description, String lock) {
        return WheelPanelText.lines(FONT, this::measure, 110, 50, 130, "Predator", "Tensura · Skills", "", "Instant", description, "Mode 1 of 3: Devour", lock);
    }

    @Test
    void aSecondFrameWithNothingChangedWrapsAndMeasuresNothing() {
        String description = "Devour a target to copy its body and skills. The mode picks what it does.";
        WheelPanelText.Lines first = frame(description, "");
        List<WheelPanelText.HelpRow> helpFirst = WheelPanelText.help(FONT, this::measure, this::measure, 126, List.of("Click", "Right-click"), List.of("use", "slot"));
        int w1 = WheelPanelText.width(FONT, "12 s", this::measure);
        int measuredAfterFirst = measured.get();
        int missesAfterFirst = WheelPanelText.misses();
        assertTrue(measuredAfterFirst > 0, "the first frame has to measure");
        for (int i = 0; i < 3; i++) {
            assertEquals(first, frame(description, ""));
            assertEquals(helpFirst, WheelPanelText.help(FONT, this::measure, this::measure, 126, List.of("Click", "Right-click"), List.of("use", "slot")));
            assertEquals(w1, WheelPanelText.width(FONT, "12 s", this::measure));
        }
        assertEquals(measuredAfterFirst, measured.get(), "a later frame measured text again");
        assertEquals(missesAfterFirst, WheelPanelText.misses(), "a later frame worked a wrap out again");
    }

    @Test
    void aShortLockReasonLeavesTheDescriptionItsFullLines() {
        String text = "Scorching air burns everything near you and keeps burning whatever the wind does, for as long as the power stays on.";
        WheelPanelText.Lines shortReason = WheelPanelText.lines(FONT, this::measure, 110, 50, 130, "Heat Wave", "Tensura", "", "Toggle", text, "", "Locked");
        WheelPanelText.Lines longReason = WheelPanelText.lines(FONT, this::measure, 110, 50, 130, "Heat Wave", "Tensura", "", "Toggle", text, "",
                "Needs Fire Resistance level 6 and a stronger body to use it");
        assertEquals(1, shortReason.lock().size());
        assertEquals(2, longReason.lock().size());
        assertTrue(shortReason.description().size() > longReason.description().size(), "a box that holds one line is shorter: " + shortReason.description() + " vs " + longReason.description());
    }

    @Test
    void theCoverNoteIsWrappedUnderTheNameAndPushesTheDescriptionDown() {
        String cover = "Also covers: Vampirism Night Vision, Werewolves Sense";
        String text = "A long description that goes on and on and on for a while.";
        WheelPanelText.Lines with = WheelPanelText.lines(FONT, this::measure, 110, 50, 130, "Sharp Sight", "Tensura · Skills", cover, "Instant", text, "", "");
        WheelPanelText.Lines without = WheelPanelText.lines(FONT, this::measure, 110, 50, 130, "Sharp Sight", "Tensura · Skills", "", "Instant", text, "", "");
        assertTrue(with.cover().size() >= 1 && with.cover().size() <= 2, "the note takes one or two lines: " + with.cover());
        assertTrue(without.cover().isEmpty());
        assertTrue(with.description().size() <= without.description().size(), "the note takes room from the description, never the other way");
        WheelPanelText.Lines tight = WheelPanelText.lines(FONT, this::measure, 110, 50, 100, "Sharp Sight", "Tensura · Skills", cover, "Instant", text, "", "");
        assertTrue(tight.description().size() < without.description().size(), "with little room the description gives way first");
    }

    @Test
    void anotherPowerAnotherWidthOrAnotherLanguageIsAnotherKey() {
        frame("Hold to charge.", "");
        int after = WheelPanelText.misses();
        frame("Hold to charge, then let go.", "");
        assertTrue(WheelPanelText.misses() > after, "another description is a new answer");
        after = WheelPanelText.misses();
        WheelPanelText.lines(FONT, this::measure, 90, 50, 130, "Predator", "Tensura · Skills", "", "Instant", "Hold to charge.", "Mode 1 of 3: Devour", "");
        assertTrue(WheelPanelText.misses() > after, "another width is a new answer");
        after = WheelPanelText.misses();
        WheelPanelText.lines(new Object(), this::measure, 110, 50, 130, "Predator", "Tensura · Skills", "", "Instant", "Hold to charge.", "Mode 1 of 3: Devour", "");
        assertTrue(WheelPanelText.misses() > after, "another font (a resource reload) is a new answer");
    }

    @Test
    void theDescriptionIsWrappedAndCutWithAnEllipsisAndGivesWayToTheLockBox() {
        String longText = "Scorching air burns everything near you and keeps burning whatever the wind does, for as long as the power stays on and the caster stands still.";
        WheelPanelText.Lines open = frame(longText, "");
        assertEquals(4, open.description().size());
        assertTrue(open.description().get(3).endsWith("\u2026"), "the last line is cut with an ellipsis: " + open.description());
        for (String line : open.description()) {
            assertTrue(line.length() * 6 <= 110, line);
        }
        WheelPanelText.Lines locked = frame(longText, "Needs Fire Resistance level 6");
        assertEquals(1, locked.description().size(), "80 - 20 - 42 - 2 leaves one line");
        assertTrue(locked.lock().size() >= 1 && locked.lock().size() <= 2, "the lock text takes one or two lines: " + locked.lock());
        assertTrue(frame("", "").description().isEmpty());
    }

    @Test
    void helpRowsAreCutToWhatIsLeftOfThePanelWidthAfterTheKey() {
        WheelPanelText.clear();
        // 6 px a letter, a bold key 7: "Shift-click" is 77 + 5 and "second option" would be 78 more, 160 on a 126 px panel
        List<WheelPanelText.HelpRow> rows = WheelPanelText.help(FONT, s -> s.length() * 7, s -> s.length() * 6, 126, List.of("Shift-click", "Click"), List.of("second option", "use"));
        assertTrue(rows.get(0).whatX() + rows.get(0).what().length() * 6 <= 126, "cut to the panel: " + rows.get(0));
        assertTrue(rows.get(0).what().length() < "second option".length());
        assertEquals("use", rows.get(1).what(), "a row that fits is left alone");
    }

    @Test
    void theStandardHelpRowsAlwaysFitAndTheExtrasGoFromTheEndWhenAMaximalLockedPanelWouldNotHoldThem() {
        int bottom = 187; // the panel's last row: 196 high, 9 of padding
        int top = 49;
        assertEquals(4, WheelPanelText.helpRows(4, bottom, top, 2, 2), "the four rows of every other power always fit");
        assertEquals(6, WheelPanelText.helpRows(6, bottom, top, 0, 0), "a power with no modes and no lock holds all six");
        assertEquals(6, WheelPanelText.helpRows(6, bottom, top, 2, 0));
        assertEquals(5, WheelPanelText.helpRows(6, bottom, top, 2, 2), "two mode lines and a two-line lock box leave room for five: the keys row goes");
        assertEquals(4, WheelPanelText.helpRows(6, bottom, top + 20, 2, 2), "two cover lines as well: never below the standard four");
    }}
