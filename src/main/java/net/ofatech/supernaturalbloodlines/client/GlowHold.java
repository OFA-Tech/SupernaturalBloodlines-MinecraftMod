package net.ofatech.supernaturalbloodlines.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which entities this client outlines, until when, and what their glow flag was before. No Minecraft types, so it can be
 * unit-tested; {@link HunterSenseClient} feeds it the flag it reads each frame and writes what it returns.
 * <p>
 * The client cannot ask the server whether an entity really glows (remote entities' effects never reach it), so the base
 * is the flag as first read, and it is corrected to false when a frame reads false: that means the server cleared the
 * flag over our write.
 * <p>
 * shortcut: a glow that starts during the hold reads as our own write, so the restore at the end turns it off until
 * the server sends the flag again; upgrade with a client mixin on {@code Minecraft.shouldEntityAppearGlowing} when the species part has a mixin free.
 */
final class GlowHold {
    private static final class Entry {
        long until;
        boolean known;
        boolean base;
    }

    private final Map<Integer, Entry> entries = new HashMap<>();

    /** Starts or refreshes the hold on {@code ids}; a refresh keeps the base that was already read. */
    void hold(Iterable<Integer> ids, long until) {
        for (int id : ids) {
            entries.computeIfAbsent(id, k -> new Entry()).until = until;
        }
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    void clear() {
        entries.clear();
    }

    /** A copy, so the caller may drop entries while it walks them. */
    List<Integer> ids() {
        return new ArrayList<>(entries.keySet());
    }

    /**
     * One frame for an entity that is in the level.
     *
     * @param flagNow the entity's glow flag as it is now, before this frame's write
     * @return the flag to write; at the end of the hold, the base (and the entry is dropped)
     */
    boolean frame(int id, boolean flagNow, long now) {
        Entry e = entries.get(id);
        if (e == null) {
            return flagNow;
        }
        if (!e.known) {
            e.known = true;
            e.base = flagNow;
        } else if (!flagNow) {
            e.base = false;
        }
        if (now >= e.until) {
            entries.remove(id);
            return e.base;
        }
        return true;
    }

    /** An entity that is not in the level (out of range, dead): nothing to write, the entry goes when it expires. */
    void missing(int id, long now) {
        Entry e = entries.get(id);
        if (e != null && now >= e.until) {
            entries.remove(id);
        }
    }
}
