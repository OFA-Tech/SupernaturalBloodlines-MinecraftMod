package net.ofatech.supernaturalbloodlines.parts.species.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.Nullable;

/**
 * What the Memory Claws taught somebody (R183): a <b>snapshot</b> of a player's sire and genes as that player knew them the moment the claws were used.
 * Whoever used them reads the snapshot on that player's Profile page; the player's later discoveries are never a live feed to them (using the claws again
 * takes a new snapshot). Held per user and subject, never shared with a pack. Sizes are capped ({@value #MAX_SUBJECTS} subjects each, {@value #MAX_OBSERVERS} users, the
 * oldest dropped first), and a save that is damaged loads what it can.
 */
public final class MemorySnapshots extends SavedData {
    public static final String NAME = "supernaturalbloodlines_memory_snapshots";
    public static final int MAX_OBSERVERS = 1024;
    public static final int MAX_SUBJECTS = 64;

    /** The values as the Profile page carries them: {@code null} is "nothing to say" (shown as "?"), an empty list is an explicit "none". */
    public record Memory(@Nullable List<String> sire, @Nullable List<String> genes) {
        public Memory {
            sire = sire == null ? null : List.copyOf(sire);
            genes = genes == null ? null : List.copyOf(genes);
        }
    }

    private final LinkedHashMap<UUID, LinkedHashMap<UUID, Memory>> byObserver = new LinkedHashMap<>();

    public static MemorySnapshots get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(MemorySnapshots::new, MemorySnapshots::load, null), NAME);
    }

    /** {@code observer} read {@code subject}: this replaces whatever they read before. */
    public void put(UUID observer, UUID subject, Memory memory) {
        LinkedHashMap<UUID, Memory> mine = byObserver.remove(observer);
        if (mine == null) {
            mine = new LinkedHashMap<>();
        }
        byObserver.put(observer, mine);
        mine.remove(subject);
        mine.put(subject, memory);
        while (mine.size() > MAX_SUBJECTS) {
            mine.remove(mine.keySet().iterator().next());
        }
        while (byObserver.size() > MAX_OBSERVERS) {
            byObserver.remove(byObserver.keySet().iterator().next());
        }
        setDirty();
    }

    /** A read never creates an entry and never moves one. */
    public Optional<Memory> of(UUID observer, UUID subject) {
        LinkedHashMap<UUID, Memory> mine = byObserver.get(observer);
        return mine == null ? Optional.empty() : Optional.ofNullable(mine.get(subject));
    }

    public int observerCount() {
        return byObserver.size();
    }

    public int subjectCount(UUID observer) {
        LinkedHashMap<UUID, Memory> mine = byObserver.get(observer);
        return mine == null ? 0 : mine.size();
    }

    // ---- storage ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag observers = new ListTag();
        byObserver.forEach((observer, subjects) -> {
            CompoundTag o = new CompoundTag();
            o.putUUID("observer", observer);
            ListTag list = new ListTag();
            subjects.forEach((subject, memory) -> {
                CompoundTag m = new CompoundTag();
                m.putUUID("subject", subject);
                if (memory.sire() != null) {
                    m.put("sire", strings(memory.sire()));
                }
                if (memory.genes() != null) {
                    m.put("genes", strings(memory.genes()));
                }
                list.add(m);
            });
            o.put("memories", list);
            observers.add(o);
        });
        tag.put("observers", observers);
        return tag;
    }

    public static MemorySnapshots load(CompoundTag tag, HolderLookup.Provider registries) {
        MemorySnapshots out = new MemorySnapshots();
        for (Tag t : tag.getList("observers", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag o = (CompoundTag) t;
                if (!o.hasUUID("observer")) {
                    continue;
                }
                for (Tag s : o.getList("memories", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) s;
                    if (m.hasUUID("subject")) {
                        out.put(o.getUUID("observer"), m.getUUID("subject"),
                                new Memory(m.contains("sire", Tag.TAG_LIST) ? strings(m.getList("sire", Tag.TAG_STRING)) : null,
                                        m.contains("genes", Tag.TAG_LIST) ? strings(m.getList("genes", Tag.TAG_STRING)) : null));
                    }
                }
            } catch (RuntimeException damaged) {
                // a bad entry is skipped, not the file
            }
        }
        out.setDirty(false);
        return out;
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v)));
        return list;
    }

    private static List<String> strings(ListTag list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }
}
