package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.jetbrains.annotations.Nullable;

/**
 * What the hub works out about text and labels every frame from the same inputs: how a name is cut or wrapped, how a paragraph with a drop cap lays out, where the
 * names round a ring stand. The key holds the font, the text and every size, so a changed language, window size or datum is a new key and never a stale answer;
 * {@link #clear} runs when a screen starts or ends. Each table is bounded ({@link Memo}). The render thread only.
 */
public final class TextCache {
    private record CutKey(Object font, String text, int width) {
    }

    private record WrapKey(Object font, String text, int width, int lines) {
    }

    private record CapKey(Object font, String rest, int width, int indent, int capLines) {
    }

    private record RingKey(List<RingLabels.Request> asked, List<Rect> blocked, Rect bounds, int minWidth, int gap) {
    }

    private static final Memo<CutKey, String> CUTS = new Memo<>(512);
    private static final Memo<WrapKey, List<String>> WRAPS = new Memo<>(512);
    private static final Memo<CapKey, List<DropCap.Line>> CAPS = new Memo<>(32);
    private static final Memo<RingKey, List<@Nullable Rect>> RINGS = new Memo<>(32);

    private TextCache() {
    }

    public static String cut(Object font, String text, int width, ToIntFunction<String> measure) {
        return CUTS.get(new CutKey(font, text, width), () -> LabelFit.cut(text, width, measure));
    }

    public static List<String> wrap(Object font, String text, int width, int maxLines, ToIntFunction<String> measure) {
        return WRAPS.get(new WrapKey(font, text, width, maxLines), () -> LabelFit.wrap(text, width, maxLines, measure));
    }

    public static List<DropCap.Line> dropCap(Object font, String rest, int width, int indent, int capLines, ToIntFunction<String> measure) {
        return CAPS.get(new CapKey(font, rest, width, indent, capLines), () -> DropCap.layout(rest, width, indent, capLines, measure));
    }

    public static List<@Nullable Rect> ring(List<RingLabels.Request> asked, List<Rect> blocked, Rect bounds, int minWidth, int gap) {
        return RINGS.get(new RingKey(List.copyOf(asked), List.copyOf(blocked), bounds, minWidth, gap), () -> RingLabels.place(asked, blocked, bounds, minWidth, gap));
    }

    public static void clear() {
        CUTS.clear();
        WRAPS.clear();
        CAPS.clear();
        RINGS.clear();
    }

    /** How many answers were worked out (not reused) since the start: what a test counts. */
    public static int misses() {
        return CUTS.misses() + WRAPS.misses() + CAPS.misses() + RINGS.misses();
    }
}