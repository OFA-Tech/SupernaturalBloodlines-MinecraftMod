package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What the wheel shows from the list of powers: the mods of the inner ring (only those with a power and installed, in the fixed order), the groups of the open mod (only
 * Tensura has them, and only when two are not empty) and the powers of the open group. Which mod and group are open is {@link Open}. Pure.
 */
public final class WheelModel<E extends WheelModel.Entry> {
    /** A power as the model needs it. */
    public interface Entry {
        PowerRef ref();

        PowerGroup group();
    }

    /** The open path: the open mod's index in {@link #mods()} and the open group's in {@link #groups(PowerSource)} (0 for none), and the page. */
    public record Open(int mod, int group, int page) {
        public static final Open FIRST = new Open(0, 0, 0);
    }

    private final List<E> entries;
    private final List<PowerSource> mods;

    public WheelModel(List<E> entries, Predicate<PowerSource> installed) {
        this.entries = List.copyOf(entries);
        List<PowerSource> have = new ArrayList<>();
        for (E e : this.entries) {
            have.add(e.ref().source());
        }
        this.mods = WheelCategories.visible(have, installed);
    }

    public List<PowerSource> mods() {
        return mods;
    }

    /** The groups of a mod's middle ring: empty unless it is Tensura with at least two non-empty groups. */
    public List<PowerGroup> groups(PowerSource source) {
        List<PowerGroup> have = new ArrayList<>();
        for (E e : entries) {
            if (e.ref().source() == source) {
                have.add(e.group());
            }
        }
        return WheelCategories.groups(source, have);
    }

    /** The powers of a mod and group, in the list's order; the group is ignored for a mod with no middle ring. */
    public List<E> powers(PowerSource source, PowerGroup group) {
        boolean byGroup = !groups(source).isEmpty();
        List<E> out = new ArrayList<>();
        for (E e : entries) {
            if (e.ref().source() == source && (!byGroup || e.group() == group)) {
                out.add(e);
            }
        }
        return out;
    }

    /** The powers of the open path. */
    public List<E> powers(Open open) {
        if (mods.isEmpty()) {
            return List.of();
        }
        PowerSource source = mods.get(clamp(open.mod(), mods.size()));
        List<PowerGroup> groups = groups(source);
        return powers(source, groups.isEmpty() ? PowerGroup.MAIN : groups.get(clamp(open.group(), groups.size())));
    }

    /** The open path with the mod hovered or clicked: its first group is open, on the first page. */
    public Open openMod(int mod) {
        return new Open(clamp(mod, mods.size()), 0, 0);
    }

    /** The path with a group chosen: the first page of it. */
    public Open openGroup(Open open, int group) {
        PowerSource source = mods.isEmpty() ? null : mods.get(clamp(open.mod(), mods.size()));
        int n = source == null ? 0 : groups(source).size();
        return new Open(open.mod(), clamp(group, n), 0);
    }

    /** The path after a mod, group or list change: a mod or group that is gone falls back to the first. */
    public Open fit(Open open) {
        if (mods.isEmpty()) {
            return Open.FIRST;
        }
        int mod = clamp(open.mod(), mods.size());
        int groups = groups(mods.get(mod)).size();
        int pages = Math.max(1, (powers(new Open(mod, clamp(open.group(), groups), 0)).size() + WheelLayout.PAGE_SIZE - 1) / WheelLayout.PAGE_SIZE);
        return new Open(mod, clamp(open.group(), groups), Math.max(0, Math.min(open.page(), pages - 1)));
    }

    private static int clamp(int i, int n) {
        return n <= 0 ? 0 : Math.max(0, Math.min(i, n - 1));
    }
}
