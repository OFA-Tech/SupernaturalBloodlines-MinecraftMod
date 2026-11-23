package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.DetailRows;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import org.jetbrains.annotations.Nullable;

/**
 * The task rows of the Tensura info pane, worked out once for the same rows, width and room and kept: the page builds its detail again every frame, so what a frame may
 * not do is wrap text, measure it, build counters or a "+N more" component again. Equal rows (their text, source and progress) give back the very same {@link Laid}.
 */
final class TensuraRowLayout {
    static final int GAP = 4;
    static final int MORE_H = 10;
    static final int COMPACT_H = 26;

    /** @param lines per shown row; @param counts per shown row ("3/5", a check or nothing); @param tagW per shown row, 0 without a source tag. */
    record Laid(DetailRows.Plan plan, List<List<String>> lines, List<String> counts, int[] tagW, @Nullable Component more) {
    }

    private static Object cachedFont;
    private static List<Detail.Row> cachedRows;
    private static int cachedW = -1;
    private static int cachedRoom = -1;
    private static Laid cached;

    private TensuraRowLayout() {
    }

    static void clear() {
        cachedRows = null;
        cached = null;
    }

    static Laid of(Object font, List<Detail.Row> rows, int w, int room, ToIntFunction<String> measure, ToIntFunction<SourceTag> tag) {
        if (cached != null && w == cachedW && room == cachedRoom && font == cachedFont && rows.equals(cachedRows)) {
            return cached;
        }
        int n = rows.size();
        int[] tagW = new int[n];
        int[] fullH = new int[n];
        int[] compactH = new int[n];
        List<List<String>> full = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Detail.Row row = rows.get(i);
            tagW[i] = row.source() == null ? 0 : tag.applyAsInt(row.source()) + 4;
            List<String> two = LabelFit.wrap(row.label().getString(), w - tagW[i] - 12, 2, measure);
            full.add(two);
            fullH[i] = 16 + 10 * two.size();
            compactH[i] = COMPACT_H;
        }
        DetailRows.Plan plan = DetailRows.plan(fullH, compactH, room, GAP, MORE_H);
        List<List<String>> lines = new ArrayList<>(plan.shown());
        List<String> counts = new ArrayList<>(plan.shown());
        for (int i = 0; i < plan.shown(); i++) {
            Detail.Row row = rows.get(i);
            lines.add(plan.compact() ? LabelFit.wrap(row.label().getString(), w - tagW[i] - 12, 1, measure) : full.get(i));
            counts.add(row.total() > 1 ? row.done() + "/" + row.total() : row.complete() ? "\u2713" : "");
        }
        Component more = plan.moreLine() ? Component.translatable("supernaturalbloodlines.hub.detail.more_tasks", plan.more()) : null;
        cachedFont = font;
        cachedRows = List.copyOf(rows);
        cachedW = w;
        cachedRoom = room;
        cached = new Laid(plan, lines, counts, tagW, more);
        return cached;
    }
}
