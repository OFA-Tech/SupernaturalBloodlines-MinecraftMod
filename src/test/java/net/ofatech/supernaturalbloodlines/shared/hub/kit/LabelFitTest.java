package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonLayout;
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

    @Test
    void ladderLabelsNeverOverlapOrLeaveTheStageAtGuiWidths480And960() {
        for (HubLayout layout : List.of(layoutOf("tensura", 480), layoutOf("tensura", 960), BloodMoonLayout.of(480, 270, 8, 3), BloodMoonLayout.of(960, 540, 8, 3))) {
            Rect stage = layout.region(layout.regions().containsKey("moon") ? "moon" : "full");
            for (int n : new int[] {5, 6, 8}) {
                int hex = 18;
                int span = Math.min(stage.w() - 16, n * 70);
                int step = (span - hex) / (n - 1);
                int x0 = stage.x() + (stage.w() - (step * (n - 1) + hex)) / 2;
                List<Rect> boxes = new ArrayList<>();
                boolean stagger = LabelFit.needsStagger(RUNGS.subList(0, n), step, SMALL);
                for (int i = 0; i < n; i++) {
                    List<String> lines = LabelFit.wrap(RUNGS.get(i), LabelFit.ladderWidth(stagger ? 2 * step : step, 90), 2, SMALL);
                    int w = lines.stream().mapToInt(SMALL::applyAsInt).max().orElse(0);
                    int x = x0 + i * step + hex / 2;
                    Rect box = new Rect(Math.max(stage.x(), Math.min(stage.right() - w, x - w / 2)), stagger && i % 2 == 1 ? 18 : 0, w, 16);
                    assertTrue(box.x() >= stage.x() && box.right() <= stage.right(), RUNGS.get(i) + " leaves the stage at " + stage.w());
                    boxes.add(box);
                }
                for (int i = 1; i < boxes.size(); i++) {
                    assertFalse(boxes.get(i - 1).overlaps(boxes.get(i)), "ladder labels " + (i - 1) + " and " + i + " overlap with " + n + " rungs on a stage " + stage.w() + " wide");
                }
            }
        }
    }

    @Test
    void radialLabelsFitTheChordBetweenNeighbours() {
        for (int count = 4; count <= 14; count++) {
            for (int radius = 50; radius <= 220; radius += 10) {
                int w = LabelFit.radialWidth(radius, count, 36, 110);
                int chord = (int) (2 * radius * Math.sin(Math.PI / count));
                assertTrue(w <= Math.max(36, chord - 4), count + " nodes on " + radius + ": " + w + " vs the chord " + chord);
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
        List<String> titles = List.of("Status \u00b7 Path of the Hybrid", "Status \u00b7 Path", "Path");
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
            HubLayout l = layoutOf("tensura", width);
            int available = l.chrome().themeButton().x() - 8 - (l.frame().x() + 10);
            LabelFit.Header h = LabelFit.header(List.of("Status \u00b7 Path of the Hybrid", "Status \u00b7 Path", "Path"),
                    List.of("Beastfolk \u00b7 6 SP \u00b7 1,028 EP \u00b7 3 days", "6 SP \u00b7 1,028 EP", "6 SP"), available, 14, s -> (int) (s.length() * 6 * 1.25f), NORMAL);
            int used = (int) (h.title().length() * 6 * 1.25f) + (h.stats().isEmpty() ? 0 : 14 + NORMAL.applyAsInt(h.stats()));
            assertTrue(used <= available, width + ": " + h + " needs " + used + " of " + available);
            assertFalse(h.stats().isEmpty(), width + ": the stats were dropped altogether");
        }
    }

    private static HubLayout layoutOf(String theme, int guiWidth) {
        int height = guiWidth * 9 / 16;
        return net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout.of(guiWidth, height, 8, 4);
    }
}
