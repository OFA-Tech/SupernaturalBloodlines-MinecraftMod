package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/**
 * Reviewer panel test: the Tensura ladder (the only one drawn with LadderLabels) on its real stage, at narrow and wide GUI widths and with the tab and
 * seal counts a screen really passes (5 to 7 pages, 2 to 4 mods). Labels must stay inside the stage and 2 pixels apart, in both directions.
 */
class PanelLadderLayoutTest {
    private static final ToIntFunction<String> SMALL = s -> (int) Math.ceil(s.length() * 4.5);
    private static final List<String> RUNGS = List.of("Awakened", "Unbound", "Thirst Mastered", "Venom", "Packbound", "Moonblood", "Alpha of Alphas", "True Alpha");

    /** The text of a label box: its box less the 4 pixels of padding. */
    private static Rect text(Rect box) {
        return new Rect(box.x(), box.y(), box.w() - 4, box.h());
    }

    /** The text grown by 2 pixels on each side: another label's text must not reach into it. */
    private static Rect clearance(Rect text) {
        return new Rect(text.x() - 2, text.y(), text.w() + 4, text.h());
    }

    @Test
    void tensuraLadderLabelsStayInsideAndApartOnNarrowAndWideScreens() {
        List<String> problems = new ArrayList<>();
        for (int w : new int[] {320, 427, 480, 960}) {
            int h = w * 9 / 16;
            for (int tabs = 5; tabs <= 7; tabs++) {
                for (int seals = 2; seals <= 4; seals++) {
                    Rect stage = TensuraHubLayout.of(w, h, tabs, seals).region(TensuraHubLayout.MOON);
                    for (int n = 5; n <= 8; n++) {
                        String at = "gui " + w + " tabs " + tabs + " seals " + seals + " rungs " + n + " stage " + stage;
                        List<LadderLabels.Placed> placed = LadderLabels.place(RUNGS.subList(0, n), stage, 18, 100, SMALL);
                        List<Rect> all = new ArrayList<>();
                        for (int i = 0; i < n; i++) {
                            for (Rect box : placed.get(i).boxes()) {
                                Rect t = text(box);
                                if (t.x() < stage.x() || t.right() > stage.right()) {
                                    problems.add(at + ": " + RUNGS.get(i) + " leaves the stage at " + t);
                                }
                                for (Rect other : all) {
                                    if (t.overlaps(clearance(text(other)))) {
                                        problems.add(at + ": " + RUNGS.get(i) + " " + t + " runs into " + text(other));
                                    }
                                    if (text(other).overlaps(clearance(t))) {
                                        problems.add(at + ": " + RUNGS.get(i) + " " + text(other) + " runs into " + t);
                                    }
                                }
                                all.add(box);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems.subList(0, Math.min(10, problems.size()))) + "\ntotal " + problems.size());
    }
}
