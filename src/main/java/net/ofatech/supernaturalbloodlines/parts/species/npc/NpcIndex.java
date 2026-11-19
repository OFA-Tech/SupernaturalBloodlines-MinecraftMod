package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source;

/**
 * The ledger of the pack's NPC members (R148): for each one a short token the hub and the commands use (never the entity's UUID), whose it is, how it
 * came, and what was last seen of it (its name, kind, place and time), so the Pack page can show a member that is in an unloaded chunk or another
 * dimension. The bond on the entity is the truth for what it does; this is the truth for who has which, and it outlives the chunk. A bad entry costs the
 * entry, not the file. One per world (the overworld's storage).
 */
public final class NpcIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_npc_index";

    /** @param token the short id of the hub and the commands; @param type the entity type id; @param dim the dimension id; @param seen the game time it was last seen */
    public record Entry(int token, UUID npc, UUID owner, Source source, String name, String type, String dim, int x, int y, int z, long seen) {
    }

    private final Map<UUID, Entry> byNpc = new LinkedHashMap<>();
    private final Map<Integer, UUID> byToken = new HashMap<>();
    private int nextToken = 1;
    /** Wolves released while their chunk was not loaded: the bond still sits on the entity, so it is taken off when the wolf next joins a level. Capped, oldest first out. */
    private final java.util.Set<UUID> released = new java.util.LinkedHashSet<>();
    public static final int MAX_RELEASED = 256;

    public static NpcIndex get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(NpcIndex::new, NpcIndex::load, null), NAME);
    }

    public Optional<Entry> of(UUID npc) {
        return Optional.ofNullable(byNpc.get(npc));
    }

    public Optional<Entry> byToken(int token) {
        UUID id = byToken.get(token);
        return id == null ? Optional.empty() : of(id);
    }

    public List<Entry> ownedBy(UUID owner) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byNpc.values()) {
            if (e.owner().equals(owner)) {
                out.add(e);
            }
        }
        return out;
    }

    public int count(UUID owner, Source source) {
        int n = 0;
        for (Entry e : byNpc.values()) {
            if (e.owner().equals(owner) && e.source() == source) {
                n++;
            }
        }
        return n;
    }

    public List<Entry> all() {
        return new ArrayList<>(byNpc.values());
    }

    /** Records a member (or updates the one already recorded): the token stays the same for the same mob. */
    public Entry put(UUID npc, UUID owner, Source source, String name, String type, String dim, int x, int y, int z, long now) {
        Entry old = byNpc.get(npc);
        int token = old != null ? old.token() : nextToken++;
        Entry e = new Entry(token, npc, owner, source, name, type, dim, x, y, z, now);
        if (!e.equals(old)) {
            byNpc.put(npc, e);
            byToken.put(token, npc);
            setDirty();
        }
        return e;
    }

    /** Remembers that this wolf was freed while unloaded, so the bond that is still on it is dropped when it loads. */
    public void markReleased(UUID npc) {
        released.add(npc);
        while (released.size() > MAX_RELEASED) {
            released.remove(released.iterator().next());
        }
        setDirty();
    }

    /** Whether this wolf was freed while unloaded; forgets it when it was (the bond is about to be taken off). */
    public boolean takeReleased(UUID npc) {
        boolean was = released.remove(npc);
        if (was) {
            setDirty();
        }
        return was;
    }

    public int releasedCount() {
        return released.size();
    }

    /** A wolf that was replaced by another entity (a Werewolves transformation): the same member under a new uuid, with the same token. */
    public Optional<Entry> rekey(UUID old, UUID now) {
        Entry e = byNpc.remove(old);
        if (e == null) {
            return Optional.empty();
        }
        Entry moved = new Entry(e.token(), now, e.owner(), e.source(), e.name(), e.type(), e.dim(), e.x(), e.y(), e.z(), e.seen());
        byNpc.put(now, moved);
        byToken.put(moved.token(), now);
        setDirty();
        return Optional.of(moved);
    }

    public boolean remove(UUID npc) {
        Entry e = byNpc.remove(npc);
        if (e == null) {
            return false;
        }
        byToken.remove(e.token());
        setDirty();
        return true;
    }

    public static NpcIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcIndex n = new NpcIndex();
        n.nextToken = Math.max(1, tag.getInt("next"));
        ListTag list = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            try {
                Entry e = new Entry(c.getInt("token"), UUID.fromString(c.getString("npc")), UUID.fromString(c.getString("owner")),
                        Source.valueOf(c.getString("source")), c.getString("name"), c.getString("type"), c.getString("dim"), c.getInt("x"), c.getInt("y"),
                        c.getInt("z"), c.getLong("seen"));
                if (e.token() > 0 && !n.byToken.containsKey(e.token())) {
                    n.byNpc.put(e.npc(), e);
                    n.byToken.put(e.token(), e.npc());
                    n.nextToken = Math.max(n.nextToken, e.token() + 1);
                }
            } catch (IllegalArgumentException bad) {
                // a bad entry costs that entry only
            }
        }
        ListTag gone = tag.getList("released", Tag.TAG_STRING);
        for (int i = 0; i < gone.size(); i++) {
            try {
                n.released.add(UUID.fromString(gone.getString(i)));
            } catch (IllegalArgumentException bad) {
                // a bad entry costs that entry only
            }
        }
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : byNpc.values()) {
            CompoundTag c = new CompoundTag();
            c.putInt("token", e.token());
            c.putString("npc", e.npc().toString());
            c.putString("owner", e.owner().toString());
            c.putString("source", e.source().name());
            c.putString("name", e.name());
            c.putString("type", e.type());
            c.putString("dim", e.dim());
            c.putInt("x", e.x());
            c.putInt("y", e.y());
            c.putInt("z", e.z());
            c.putLong("seen", e.seen());
            list.add(c);
        }
        ListTag gone = new ListTag();
        released.forEach(u -> gone.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
        tag.put("released", gone);
        tag.put("members", list);
        tag.putInt("next", nextToken);
        return tag;
    }
}
