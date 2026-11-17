package net.ofatech.supernaturalbloodlines.parts.species.line;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import org.jetbrains.annotations.Nullable;

/**
 * The line of every vampire, stored server-wide so line death, the rites and offline players need no loaded data.
 * {@code member -> root Original}; a root is a player Original's own UUID or an {@link NpcOriginal}'s fixed one.
 * An Original's own entry is {@code rootOf(o) == o} and it is never listed among its own members.
 */
public final class LineIndex extends SavedData {
    public enum Kind { BROKEN_LINE, IMPRISONED }

    public record Freed(UUID from, Kind kind) {
    }

    public record Member(UUID id, boolean player) {
    }

    private record Entry(UUID root, boolean player, @Nullable Freed freed) {
    }

    private static final String NAME = "supernaturalbloodlines_lines";
    private static final String ENTRIES = "entries";
    private static final String DEAD = "dead";
    private static final String OWED = "owed";
    private static final String ID = "id";
    private static final String ROOT = "root";
    private static final String PLAYER = "player";
    private static final String FREED_FROM = "freed_from";
    private static final String FREED_KIND = "freed_kind";

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> byRoot = new HashMap<>();
    private final Set<UUID> dead = new LinkedHashSet<>();
    private final Map<UUID, UUID> owed = new LinkedHashMap<>();

    LineIndex() {
    }

    /** One index per world, in the overworld's storage. */
    public static LineIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LineIndex::new, LineIndex::load, null), NAME);
    }

    public static LineIndex load(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        LineIndex idx = new LineIndex();
        for (Tag t : tag.getList(ENTRIES, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            if (!e.contains(ID) || !e.contains(ROOT)) {
                continue;
            }
            Freed freed = null;
            if (e.contains(FREED_FROM) && e.contains(FREED_KIND)) {
                try {
                    freed = new Freed(NbtUtils.loadUUID(e.get(FREED_FROM)), Kind.valueOf(e.getString(FREED_KIND)));
                } catch (IllegalArgumentException ignored) {
                    // an unknown kind loads as not freed
                }
            }
            idx.put(NbtUtils.loadUUID(e.get(ID)), new Entry(NbtUtils.loadUUID(e.get(ROOT)), e.getBoolean(PLAYER), freed));
        }
        for (Tag t : tag.getList(DEAD, Tag.TAG_INT_ARRAY)) {
            idx.dead.add(NbtUtils.loadUUID(t));
        }
        for (Tag t : tag.getList(OWED, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            if (e.contains(ID) && e.contains(ROOT)) {
                idx.owed.put(NbtUtils.loadUUID(e.get(ID)), NbtUtils.loadUUID(e.get(ROOT)));
            }
        }
        return idx;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((id, en) -> {
            CompoundTag e = new CompoundTag();
            e.put(ID, NbtUtils.createUUID(id));
            e.put(ROOT, NbtUtils.createUUID(en.root()));
            e.putBoolean(PLAYER, en.player());
            if (en.freed() != null) {
                e.put(FREED_FROM, NbtUtils.createUUID(en.freed().from()));
                e.putString(FREED_KIND, en.freed().kind().name());
            }
            list.add(e);
        });
        tag.put(ENTRIES, list);
        ListTag deadList = new ListTag();
        dead.forEach(id -> deadList.add(NbtUtils.createUUID(id)));
        tag.put(DEAD, deadList);
        ListTag owedList = new ListTag();
        owed.forEach((id, root) -> {
            CompoundTag e = new CompoundTag();
            e.put(ID, NbtUtils.createUUID(id));
            e.put(ROOT, NbtUtils.createUUID(root));
            owedList.add(e);
        });
        tag.put(OWED, owedList);
        return tag;
    }

    private void put(UUID id, Entry entry) {
        Entry old = entries.put(id, entry);
        if (old != null) {
            unlist(old.root(), id);
        }
        byRoot.computeIfAbsent(entry.root(), k -> new LinkedHashSet<>()).add(id);
    }

    private void unlist(UUID root, UUID id) {
        Set<UUID> members = byRoot.get(root);
        if (members != null) {
            members.remove(id);
            if (members.isEmpty()) {
                byRoot.remove(root);
            }
        }
    }

    public Optional<UUID> rootOf(UUID member) {
        Entry e = entries.get(member);
        return e == null ? Optional.empty() : Optional.of(e.root());
    }

    public Optional<Freed> freedOf(UUID member) {
        Entry e = entries.get(member);
        return e == null ? Optional.empty() : Optional.ofNullable(e.freed());
    }

    /** Whether the index holds this id at all (a member, a root or a freed head). */
    public boolean has(UUID id) {
        return entries.containsKey(id);
    }

    /** Puts {@code member} in {@code root}'s line (clearing any freed record). */
    public void setRoot(UUID member, UUID root, boolean player) {
        put(member, new Entry(root, player, null));
        setDirty();
    }

    /** The member becomes their own root and remembers whose line they left. */
    public void free(UUID member, UUID from, Kind kind) {
        Entry e = entries.get(member);
        put(member, new Entry(member, e != null && e.player(), new Freed(from, kind)));
        setDirty();
    }

    public void remove(UUID member) {
        Entry old = entries.remove(member);
        if (old != null) {
            unlist(old.root(), member);
            setDirty();
        }
    }

    /** The line's members in the order they joined; never the root itself. */
    public List<Member> membersOf(UUID root) {
        List<Member> out = new ArrayList<>();
        for (UUID id : byRoot.getOrDefault(root, new LinkedHashSet<>())) {
            if (!id.equals(root)) {
                out.add(new Member(id, entries.get(id).player()));
            }
        }
        return out;
    }

    public void markDead(UUID root) {
        if (dead.add(root)) {
            setDirty();
        }
    }

    public boolean isDead(UUID root) {
        return dead.contains(root);
    }

    public Set<NpcOriginal> deadNpcRoots() {
        Set<NpcOriginal> out = EnumSet.noneOf(NpcOriginal.class);
        for (UUID id : dead) {
            NpcOriginal.byUuid(id).ifPresent(out::add);
        }
        return out;
    }

    public void addOwed(UUID member, UUID root) {
        if (!root.equals(owed.put(member, root))) {
            setDirty();
        }
    }

    /** The root whose death this member owes, once; then it is paid. */
    public Optional<UUID> takeOwed(UUID member) {
        UUID root = owed.remove(member);
        if (root != null) {
            setDirty();
        }
        return Optional.ofNullable(root);
    }
}
