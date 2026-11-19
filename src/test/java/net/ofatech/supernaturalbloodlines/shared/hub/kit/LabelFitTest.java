package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/**
 * The text rules every theme shares, with a fixed width per character standing in for the font (6 px, 4.5 for the small text the ladders use): a
 * label never runs into its neighbour, and a cut label ends with an ellipsis, never mid-word without a mark.
 */
class LabelFitTest {
    private static final ToIntFunction<String> NORMAL = s -> s.length() * 6;
    private static final ToIntFunction<String> SMALL = s -> (int) Math.ceil(s.length() * 4.5);
    private static final int[] WIDTHS = {480, 960};
    private static final List<String> RUNGS = List.of("Awakened", "Unbound", "Thirst Mastered", "Venom", "Packbound", "Moonblood", "Alpha of Alphas", "True Alpha");

    @Test
    void cutFindsTheSameLongestPrefixAsAStepByStepScanButInLogarithmicMeasures() {
        String text = "Endure thirst of 15 or more (days) and then some more words after";
        for (int width = 0; width <= text.length() * 6 + 6; width++) {
            int w = width;
            String expected = text;
            if (NORMAL.applyAsInt(text) > w) {
                int room = w - NORMAL.applyAsInt("\u2026");
                int end = text.length();
                while (end > 0 && NORMAL.applyAsInt(text.substring(0, end)) > room) {
                    end--;
                }
                expected = end == 0 ? (room >= 0 ? "\u2026" : "") : text.substring(0, end).stripTrailing() + "\u2026";
            }
            assertEquals(expected, LabelFit.cut(text, w, NORMAL), "width " + w);
        }
        assertEquals("\u2026", LabelFit.cut(text, 6, NORMAL), "only the ellipsis fits: a bare ellipsis, not nothing");
        assertEquals("", LabelFit.cut(text, 5, NORMAL), "not even the ellipsis fits");
        int[] calls = {0};
        LabelFit.cut(text.repeat(40), 100, s -> {
            calls[0]++;
            return s.length() * 6;
        });
        assertTrue(calls[0] <= 20, "cut measured " + calls[0] + " times for a text of " + text.length() * 40 + " characters");
    }

    @Test
    void aLabelWrapsAtSpacesAndEndsWithAnEllipsisOnlyWhenItMust() {
        assertEquals(List.of("Rite of the", "Silver Moon"), LabelFit.wrap("Rite of the Silver Moon", 70, 2, NORMAL));
        assertEquals(List.of("Rite of"), LabelFit.wrap("Rite of", 70, 2, NORMAL));
        List<String> cut = LabelFit.wrap("Rite of Purification (Holy Water) of the Moon", 70, 2, NORMAL);
        assertEquals(2, cut.size());
        assertTrue(cut.get(1).endsWith("\u2026"), cut.toString());
        for (String line : cut) {
            assertTrue(NORMAL.applyAsInt(line) <= 70, line);
        }
    }

    @Test
    void aWordLongerThanTheLineIsCutWithAnEllipsisNotBroken() {
        List<String> lines = LabelFit.wrap("Pneumonoultramicroscopic", 60, 2, NORMAL);
        assertTrue(lines.stream().allMatch(l -> NORMAL.applyAsInt(l) <= 60), lines.toString());
        assertTrue(lines.get(lines.size() - 1).endsWith("\u2026"), lines.toString());
    }

    /** The stage the hexagon ladder is drawn on, at each GUI width: Tensura's moon stage (the only look that draws {@link LadderLabels}; the Blood Moon's ladder is a column of names and the Grimoire lists the rungs). */
    private static List<Rect> stages() {
        List<Rect> stages = new ArrayList<>();
        for (int w : WIDTHS) {
            int h = w * 9 / 16;
            stages.add(TensuraHubLayout.of(w, h, 8, 4).region(TensuraHubLayout.MOON));
        }
        return stages;
    }

    @Test
    void ladderLabelsNeverOverlapOrLeaveTheStageOnTheStagesAtGuiWidths480And960() {
        for (Rect stage : stages()) {
            for (int n : new int[] {5, 6, 8}) {
                List<LadderLabels.Placed> placed = LadderLabels.place(RUNGS.subList(0, n), stage, 18, 100, SMALL);
                List<Rect> all = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    for (Rect box : placed.get(i).boxes()) {
                        // a box carries 4 pixels of padding after its text; the text itself must stay inside the stage and 2 pixels clear of its neighbour
                        Rect text = new Rect(box.x(), box.y(), box.w() - 4, box.h());
                        assertTrue(text.x() >= stage.x() && text.right() <= stage.right(), RUNGS.get(i) + " leaves the stage (" + stage + ")");
                        for (Rect other : all) {
                            Rect near = new Rect(other.x() - 2, other.y(), other.w() - 4 + 4, other.h());
                            assertFalse(text.overlaps(near), "ladder labels overlap with " + n + " rungs on " + stage + " (" + text + " vs " + other + ")");
                        }
                        all.add(box);
                    }
                }
            }
        }
    }
    @Test
    void radialLabelsFitTheGapBetweenNeighboursOnTheRealRing() {
        for (int count = 4; count <= 14; count++) {
            for (int radius = 50; radius <= 220; radius += 10) {
                int w = LabelFit.radialWidth(radius, count, 36, 110);
                // the nodes as the Skills page really places them, not a formula copied from the code under test
                List<Radial.Node> ring = Radial.ring(0, 0, radius, count);
                double gap = Double.MAX_VALUE;
                for (int i = 0; i < count; i++) {
                    Radial.Node a = ring.get(i);
                    Radial.Node b = ring.get((i + 1) % count);
                    gap = Math.min(gap, Math.hypot(a.x() - b.x(), a.y() - b.y()));
                }
                assertTrue(w <= Math.max(36, gap - 2), count + " nodes on " + radius + ": a label " + w + " wide against nodes " + gap + " apart");
                assertTrue(w >= 36 && w <= 110);
            }
        }
    }
    @Test
    void cardTitlesWrapToTheirCardAtBothWidths() {
        for (int width : WIDTHS) {
            int stage = width * 3 / 5;
            int cols = Math.max(1, Math.min(3, stage / 190));
            int card = (stage - 6 * (cols - 1)) / cols;
            for (String name : List.of("Rite of Purification (Holy Water)", "Curse of Magicule Drain", "Rite of the Silver Moon", "Doppelg\u00e4nger Blood")) {
                List<String> lines = LabelFit.wrap(name, card - 46, 2, NORMAL);
                assertTrue(lines.size() <= 2);
                for (String line : lines) {
                    assertTrue(NORMAL.applyAsInt(line) <= card - 46, name + " -> " + line);
                }
                if (lines.size() == 2 && NORMAL.applyAsInt(name) > 2 * (card - 46)) {
                    assertTrue(lines.get(1).endsWith("\u2026"), name);
                }
            }
        }
    }

    @Test
    void theTitleStripShortensTheTitleBeforeTheStatsLoseParts() {
        // the strings TensuraHubTheme.drawFooter builds, with the title bar's brackets
        List<String> titles = List.of("\u300a Status \u00b7 Path of the Hybrid \u300b", "\u300a Status \u00b7 Path \u300b", "\u300a Path \u300b");
        List<String> stats = List.of("Beastfolk \u00b7 6 SP \u00b7 1,028 EP \u00b7 3 days", "Beastfolk \u00b7 6 SP \u00b7 1,028 EP", "6 SP \u00b7 1,028 EP", "6 SP");
        for (int available = 80; available <= 700; available += 20) {
            LabelFit.Header h = LabelFit.header(titles, stats, available, 14, NORMAL, NORMAL);
            int used = NORMAL.applyAsInt(h.title()) + (h.stats().isEmpty() ? 0 : 14 + NORMAL.applyAsInt(h.stats()));
            assertTrue(used <= available, "title and stats overlap at " + available + ": " + h);
            if (available >= 360) {
                assertEquals(stats.get(0), h.stats(), "the stats are kept whole while a shorter title makes room, at " + available);
            }
        }
        assertEquals(titles.get(0), LabelFit.header(titles, stats, 700, 14, NORMAL, NORMAL).title());
    }

    @Test
    void theRealTitleStripsAtBothWidthsHoldTheirTitleAndStats() {
        for (int width : WIDTHS) {
            HubLayout l = layoutOf(width);
            int available = l.chrome().themeButton().x() - 8 - (l.frame().x() + 10);
            LabelFit.Header h = LabelFit.header(List.of("\u300a Status \u00b7 Path of the Hybrid \u300b", "\u300a Status \u00b7 Path \u300b", "\u300a Path \u300b"),
                    List.of("Beastfolk \u00b7 6 SP \u00b7 1,028 EP \u00b7 3 days", "6 SP \u00b7 1,028 EP", "6 SP"), available, 14, s -> (int) (s.length() * 6 * 1.25f), NORMAL);
            int used = (int) (h.title().length() * 6 * 1.25f) + (h.stats().isEmpty() ? 0 : 14 + NORMAL.applyAsInt(h.stats()));
            assertTrue(used <= available, width + ": " + h + " needs " + used + " of " + available);
            assertFalse(h.stats().isEmpty(), width + ": the stats were dropped altogether");
        }
    }

    private static HubLayout layoutOf(int guiWidth) {
        int height = guiWidth * 9 / 16;
        return net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout.of(guiWidth, height, 8, 4);
    }
}
