package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEdit;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingKind;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class FlowAndEditTest {
    @Test
    void flowWrapsInsideTheAreaAndNeverOverlaps() {
        for (int w : new int[] {40, 90, 150, 233, 480}) {
            Rect area = new Rect(10, 20, w, 200);
            Flow.Result r = Flow.wrap(area, List.of(60, 45, 80, 30, 120, 70, 300, 25), 12, 4);
            for (int i = 0; i < r.rects().size(); i++) {
                Rect a = r.rects().get(i);
                assertTrue(a.x() >= area.x() && a.right() <= area.right() && a.y() >= area.y(), "item " + i + " outside at width " + w + ": " + a);
                for (int j = i + 1; j < r.rects().size(); j++) {
                    assertFalse(a.overlaps(r.rects().get(j)), "items " + i + " and " + j + " overlap at width " + w);
                }
            }
            assertEquals(r.rects().get(r.rects().size() - 1).bottom(), r.bottom());
        }
        assertEquals(20, Flow.wrap(new Rect(0, 20, 50, 50), List.of(), 12, 4).bottom());
    }

    @Test
    void columnsShareTheWidthEvenly() {
        List<Rect> c = Flow.columns(new Rect(0, 0, 100, 50), 3, 5, 10);
        assertEquals(3, c.size());
        assertTrue(c.get(2).right() <= 100);
        assertFalse(c.get(0).overlaps(c.get(1)));
    }

    @Test
    void typingMovesTheCaretAndBackspaceDeletes() {
        TextEdit e = new TextEdit("ab", false, 5);
        e.type('c');
        assertEquals("abc", e.text());
        e.left();
        e.backspace();
        assertEquals("ac", e.text());
        assertEquals(1, e.caret());
        e.delete();
        assertEquals("a", e.text());
        e.type('\n');
        e.newline();
        assertEquals("a", e.text(), "a one-line field takes no newline and no control character");
        for (char c : "xyzxyz".toCharArray()) {
            e.type(c);
        }
        assertEquals(5, e.text().length(), "the cap holds");
    }

    @Test
    void aListEditorKeepsLinesAndHomeEndStayOnTheLine() {
        TextEdit e = new TextEdit("one", true, 100);
        e.newline();
        e.type('t');
        e.type('w');
        assertEquals("one\ntw", e.text());
        e.home();
        assertEquals(4, e.caret());
        e.end();
        assertEquals(6, e.caret());
    }

    @Test
    void textBecomesAValueOfTheRightKind() {
        assertEquals(Optional.of(new SettingValue.Int(12)), AdminEdit.parse(SettingKind.INT, " 12 "));
        assertTrue(AdminEdit.parse(SettingKind.INT, "12.5").isEmpty());
        assertEquals(Optional.of(new SettingValue.Dbl(0.25)), AdminEdit.parse(SettingKind.DOUBLE, "0,25"));
        assertTrue(AdminEdit.parse(SettingKind.DOUBLE, "x").isEmpty());
        assertEquals(Optional.of(new SettingValue.Bool(true)), AdminEdit.parse(SettingKind.BOOL, "TRUE"));
        assertTrue(AdminEdit.parse(SettingKind.BOOL, "yes").isEmpty());
        assertEquals(Optional.of(new SettingValue.Lines(List.of("a=1", "b=2"))), AdminEdit.parse(SettingKind.LIST, "a=1\n\n  b=2  \n"));
    }

    @Test
    void formatAndParseRoundTrip() {
        for (SettingValue v : List.of(new SettingValue.Int(-4), new SettingValue.Dbl(0.5), new SettingValue.Bool(false), new SettingValue.Text("x"),
                new SettingValue.Lines(List.of("a", "b")))) {
            SettingKind kind = switch (v) {
                case SettingValue.Int i -> SettingKind.INT;
                case SettingValue.Dbl d -> SettingKind.DOUBLE;
                case SettingValue.Bool b -> SettingKind.BOOL;
                case SettingValue.Text t -> SettingKind.TEXT;
                case SettingValue.Lines l -> SettingKind.LIST;
            };
            assertEquals(Optional.of(v), AdminEdit.parse(kind, AdminEdit.format(v)));
        }
    }

    @Test
    void stateFollowsTheDraft() {
        SettingValue cur = new SettingValue.Int(5);
        assertEquals(AdminEdit.State.DEFAULT, AdminEdit.state(cur, cur, "5", false, false));
        assertEquals(AdminEdit.State.UNSAVED, AdminEdit.state(cur, cur, "6", false, false));
        assertEquals(AdminEdit.State.SAVED, AdminEdit.state(cur, cur, "5", false, true));
        assertEquals(AdminEdit.State.INVALID, AdminEdit.state(cur, cur, "5", true, true));
    }

    @Test
    void searchNeedsEveryWordAndStepsStayInRange() {
        List<String> rows = List.of("species.max_five", "species.transition_ticks", "werewolves.full_form_level");
        assertEquals(List.of("species.max_five"), AdminEdit.filter(rows, "MAX five", s -> s));
        assertEquals(rows, AdminEdit.filter(rows, "  ", s -> s));
        assertEquals(List.of(), AdminEdit.filter(rows, "zzz", s -> s));
        assertEquals(10, AdminEdit.step(9, 5, 1, 10));
        assertEquals(1, AdminEdit.step(2, -5, 1, 10));
        assertEquals("Full form level", AdminEdit.humanize("core:werewolves.full_form_level"));
    }
}
