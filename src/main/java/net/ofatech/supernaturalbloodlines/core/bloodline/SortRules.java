package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * How a chest is tidied by a member with {@code sort} on (R148): partial stacks of one item are merged into full ones, then the stacks are ordered by
 * category and then by item id. Pure: a slot is an item id, a count and the most it stacks to.
 */
public final class SortRules {
    public record Slot(String id, int count, int max) {
    }

    private SortRules() {
    }

    /** The merged and ordered slots: the same items in the same total counts, in as few stacks as possible. Empty slots are dropped. */
    public static List<Slot> sort(List<Slot> slots, ToIntFunction<String> category) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Integer> maxes = new LinkedHashMap<>();
        for (Slot s : slots) {
            if (s.count() <= 0) {
                continue;
            }
            totals.merge(s.id(), s.count(), Integer::sum);
            maxes.putIfAbsent(s.id(), Math.max(1, s.max()));
        }
        List<Slot> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            int left = e.getValue();
            int max = maxes.get(e.getKey());
            while (left > 0) {
                int n = Math.min(max, left);
                out.add(new Slot(e.getKey(), n, max));
                left -= n;
            }
        }
        out.sort(Comparator.<Slot>comparingInt(s -> category.applyAsInt(s.id())).thenComparing(Slot::id).thenComparing(Comparator.comparingInt(Slot::count).reversed()));
        return out;
    }
}
