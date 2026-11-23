package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;

/**
 * Rule C: one entry for a power several mods give. First the tiers of one skill collapse (only the highest owned stays), then the groups apply in file order. Pure.
 * <ul>
 * <li>A group applies only when at least two of its members are among the entries still shown.</li>
 * <li>The entry that stays is the side that is on right now (so the player can turn it off), else the main member when the player has it, else the next present member in file
 * order. A missing main never hides the others.</li>
 * <li>An entry hidden by an earlier group stays hidden and takes no part in a later one.</li>
 * <li>A skill with several modes is never matched, hidden or kept as a main: a mode inside a bigger skill is not the same power (owner, 2026-11-22).</li>
 * </ul>
 */
public final class Dedup {
    private Dedup() {
    }

    /** Anything the wheel lists: a power and how many modes it has. */
    public interface Item {
        PowerRef ref();

        int modeCount();

        /** Whether this entry may be matched, hidden or kept by a group: a skill with modes never is. A power that is not the same thing for this player (another race's ability) overrides it. */
        default boolean groupable() {
            return modeCount() <= 1;
        }
    }

    /**
     * @param shown the entries to list, in the input order
     * @param hidden the entries a group hid, and the group
     * @param groupOf the group a shown entry stands for (the first one when it stands for two)
     * @param collapsed the lower tiers a chain hid
     */
    public record Result<E>(List<E> shown, Map<E, OverlapGroup> hidden, Map<E, OverlapGroup> groupOf, List<E> collapsed) {
    }

    /** No group has an active side. */
    public static final ToIntFunction<String> NONE_ACTIVE = id -> -1;

    public static <E extends Item> Result<E> apply(List<E> entries, OverlapTable table) {
        return apply(entries, table, NONE_ACTIVE);
    }

    /** @param activeSide the side that is on in a group (its index among the members), -1 for none */
    public static <E extends Item> Result<E> apply(List<E> entries, OverlapTable table, ToIntFunction<String> activeSide) {
        Set<E> out = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<E> collapsed = new ArrayList<>();
        for (List<PowerRef> chain : table.chains()) {
            E highest = null;
            int best = -1;
            for (E e : entries) {
                int at = chain.indexOf(e.ref());
                if (at > best) {
                    best = at;
                    highest = e;
                }
            }
            for (E e : entries) {
                if (chain.contains(e.ref()) && e != highest) {
                    out.add(e);
                    collapsed.add(e);
                }
            }
        }
        Map<E, OverlapGroup> hidden = new IdentityHashMap<>();
        Map<E, OverlapGroup> groupOf = new IdentityHashMap<>();
        for (OverlapGroup g : table.groups()) {
            Map<Integer, E> bySide = new LinkedHashMap<>();
            for (E e : entries) {
                if (out.contains(e) || !e.groupable()) {
                    continue;
                }
                int side = table.sideOf(g, e.ref());
                if (side >= 0) {
                    bySide.putIfAbsent(side, e);
                }
            }
            if (bySide.size() < 2) {
                continue;
            }
            E keep = bySide.get(activeSide.applyAsInt(g.id()));
            if (keep == null) {
                keep = bySide.get(g.main());
            }
            if (keep == null) {
                keep = bySide.entrySet().stream().min(java.util.Map.Entry.comparingByKey()).orElseThrow().getValue();
            }
            for (E e : bySide.values()) {
                if (e != keep) {
                    out.add(e);
                    hidden.put(e, g);
                }
            }
            groupOf.putIfAbsent(keep, g);
        }
        List<E> shown = new ArrayList<>();
        for (E e : entries) {
            if (!out.contains(e)) {
                shown.add(e);
            }
        }
        Map<E, OverlapGroup> hiddenOrdered = new LinkedHashMap<>();
        Map<E, OverlapGroup> groupOrdered = new LinkedHashMap<>();
        for (E e : entries) {
            if (hidden.containsKey(e)) {
                hiddenOrdered.put(e, hidden.get(e));
            }
            if (groupOf.containsKey(e) && !out.contains(e)) {
                groupOrdered.put(e, groupOf.get(e));
            }
        }
        return new Result<>(shown, hiddenOrdered, groupOrdered, collapsed);
    }

}
