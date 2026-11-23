package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/** Which rings the wheel draws. Pure. */
public final class WheelCategories {
    private WheelCategories() {
    }

    /** The inner ring: the sources that have an entry and whose mod is installed, in the fixed order whatever the input order. */
    public static List<PowerSource> visible(Collection<PowerSource> withEntries, Predicate<PowerSource> installed) {
        List<PowerSource> out = new ArrayList<>();
        for (PowerSource s : PowerSource.displayOrder()) {
            if (withEntries.contains(s) && installed.test(s)) {
                out.add(s);
            }
        }
        return out;
    }

    /** The middle ring of a source: empty (skip the ring) unless it is Tensura with at least two non-empty groups. */
    public static List<PowerGroup> groups(PowerSource source, Collection<PowerGroup> withEntries) {
        if (source != PowerSource.TENSURA) {
            return List.of();
        }
        List<PowerGroup> out = new ArrayList<>();
        for (PowerGroup g : PowerGroup.displayOrder()) {
            if (withEntries.contains(g)) {
                out.add(g);
            }
        }
        return out.size() < 2 ? List.of() : List.copyOf(out);
    }
}
