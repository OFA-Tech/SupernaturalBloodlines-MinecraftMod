package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusOrder;
import org.junit.jupiter.api.Test;

/**
 * Walks the keyboard focus the way the screen does (Tab is {@code HubFocus.move} over {@code FocusOrder.order} of the frame's targets): every target is
 * reached, none twice, in the order the page lists them (the tabs, the stage, the panel, the buttons), it wraps, and Shift+Tab undoes Tab.
 */
class FocusWalkTest {
    /**
     * The ids of a frame of the Path page with a ladder bookmark row, a task list, the info pane's buttons and the chrome. Written by hand, not read from a real
     * frame (a frame needs a {@code HubScreen} and so a running Minecraft): the ids copy the prefixes the pages register ({@code tab:}, {@code seal:}, {@code theme},
     * {@code close}, {@code ladder:}, {@code phase:}, {@code task:}, {@code detail:}), and if a page registers a new prefix, add it here and to {@code FocusOrder.zoneOf}.
     */
    private static final List<String> FRAME = List.of("tab:PATH", "tab:SKILLS", "tab:QUESTS", "tab:RITES", "tab:BLOODLINE", "tab:PACK", "tab:GUIDE", "seal:tensura", "seal:enchanted",
            "theme", "close", "ladder:0", "ladder:1", "phase:hybrid:awakened", "task:a", "task:b", "task:c", "line:root", "line:anc:0", "line:sire", "line:you",
            "line:down:0", "line:down:1", "detail:track", "detail:claim");

    @Test
    void tabReachesEveryTargetOnceAndWraps() {
        List<String> ring = FocusOrder.order(FRAME);
        assertEquals(new HashSet<>(FRAME), new HashSet<>(ring), "the order lost or invented a target");
        assertEquals(FRAME.size(), ring.size());
        Set<String> seen = new HashSet<>();
        String at = null;
        for (int i = 0; i < ring.size(); i++) {
            at = HubFocus.move(ring, at, 1);
            assertTrue(seen.add(at), "Tab visited " + at + " twice before every target was reached");
        }
        assertEquals(ring.size(), seen.size());
        assertEquals(ring.get(0), HubFocus.move(ring, at, 1), "Tab after the last target wraps to the first");
    }

    @Test
    void shiftTabRetracesTab() {
        List<String> ring = FocusOrder.order(FRAME);
        List<String> forward = new ArrayList<>();
        String at = null;
        for (int i = 0; i < ring.size(); i++) {
            at = HubFocus.move(ring, at, 1);
            forward.add(at);
        }
        for (int i = forward.size() - 1; i > 0; i--) {
            at = HubFocus.move(ring, at, -1);
            assertEquals(forward.get(i - 1), at, "Shift+Tab from " + forward.get(i));
        }
    }

    @Test
    void theNumberKeysReachOnlyThePagesThatShow() {
        for (int pages = 5; pages <= 8; pages++) {
            for (int k = 0; k < 8; k++) {
                var jump = FocusOrder.pageForKey(49 + k, pages);
                assertEquals(k < pages, jump.isPresent(), "key " + (k + 1) + " with " + pages + " pages");
            }
        }
    }
}
