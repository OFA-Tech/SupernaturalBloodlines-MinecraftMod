package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Who sired whom, online or not, players and creatures. Every node keeps its last known name, so a tree can still show a
 * sire who is offline or gone. A child has one sire; a sire has any number of children.
 */
public final class SirelineIndex extends SavedData {
    public record Node(UUID id, String entityType, String name) {
    }

    private static final String NAME = "supernaturalbloodlines_sirelines";
    private static final String LINKS = "links";
    private static final String NAMES = "names";
    private static final String CHILD = "child";
    private static final String SIRE = "sire";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String LAST_NAME = "name";
    private static final String PENDING_MOBS = "pendingMobs";
    private static final String PENDING_STAMPS = "pendingMobStamps";
    private static final String AT = "at";
    /** The key of the old sireline death's offline players: read so their debt is not lost (see {@code LineDeath.onLogin}). */
    private static final String LEGACY_PLAYERS = "pendingDeaths";
    private static final String PENDING_TICKS = "pendingMobTicks";
    /** A creature owed a death that never loads again is forgotten after this much game time (30 days of 20 ticks a second). */
    public static final long PENDING_MAX_AGE_TICKS = 51_840_000L;
    /** The stamp of a debt saved before debts had game time: it gets the time of the first prune. */
    private static final long UNSTAMPED = -1L;

    private record Link(UUID sire, String entityType) {
    }

    // child -> its sire; insertion order is the order the tree lists them in
    private final Map<UUID, Link> links = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> children = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    // line death's debts of creatures that were not loaded when their line died (players owe it through LineIndex), with when they were added
    private final Map<UUID, Long> pendingMobs = new LinkedHashMap<>();
    // players the old sireline death owed a death while offline: paid or dropped at their next login
    private final Set<UUID> legacyPlayers = new LinkedHashSet<>();

    SirelineIndex() {
    }

    /** One index per world: always read from the overworld's storage, whichever level is passed. */
    public static SirelineIndex get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(SirelineIndex::new, SirelineIndex::load, null), NAME);
    }

    static SirelineIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        SirelineIndex idx = new SirelineIndex();
        for (Tag t : tag.getList(NAMES, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            idx.names.put(NbtUtils.loadUUID(e.get(ID)), e.getString(LAST_NAME));
        }
        for (Tag t : tag.getList(LINKS, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            idx.link(NbtUtils.loadUUID(e.get(SIRE)), NbtUtils.loadUUID(e.get(CHILD)), e.getString(TYPE));
        }
        // nothing is pruned here: there is no game time yet. The plain list and the first stamped format (wall clock) have none.
        for (Tag t : tag.getList(PENDING_MOBS, Tag.TAG_INT_ARRAY)) {
            idx.pendingMobs.putIfAbsent(NbtUtils.loadUUID(t), UNSTAMPED);
        }
        for (Tag t : tag.getList(PENDING_STAMPS, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            if (e.contains(ID)) {
                idx.pendingMobs.putIfAbsent(NbtUtils.loadUUID(e.get(ID)), UNSTAMPED);
            }
        }
        for (Tag t : tag.getList(PENDING_TICKS, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            if (e.contains(ID) && e.contains(AT)) {
                idx.pendingMobs.put(NbtUtils.loadUUID(e.get(ID)), e.getLong(AT));
            }
        }
        for (Tag t : tag.getList(LEGACY_PLAYERS, Tag.TAG_INT_ARRAY)) {
            idx.legacyPlayers.add(NbtUtils.loadUUID(t));
        }
        return idx;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        links.forEach((child, link) -> {
            CompoundTag e = new CompoundTag();
            e.put(CHILD, NbtUtils.createUUID(child));
            e.put(SIRE, NbtUtils.createUUID(link.sire()));
            e.putString(TYPE, link.entityType());
            list.add(e);
        });
        tag.put(LINKS, list);
        ListTag known = new ListTag();
        names.forEach((id, name) -> {
            CompoundTag e = new CompoundTag();
            e.put(ID, NbtUtils.createUUID(id));
            e.putString(LAST_NAME, name);
            known.add(e);
        });
        tag.put(NAMES, known);
        ListTag stamps = new ListTag();
        pendingMobs.forEach((id, at) -> {
            CompoundTag e = new CompoundTag();
            e.put(ID, NbtUtils.createUUID(id));
            e.putLong(AT, at);
            stamps.add(e);
        });
        tag.put(PENDING_TICKS, stamps);
        // shortcut: the plain list is kept for one release so that going back to the previous version keeps its debts; drop it after that
        tag.put(PENDING_MOBS, uuids(pendingMobs.keySet()));
        tag.put(LEGACY_PLAYERS, uuids(legacyPlayers));
        return tag;
    }

    private static ListTag uuids(Set<UUID> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(NbtUtils.createUUID(id)));
        return list;
    }

    private void link(UUID sire, UUID child, String entityType) {
        Link old = links.put(child, new Link(sire, entityType));
        if (old != null) {
            unlinkChild(old.sire(), child);
        }
        children.computeIfAbsent(sire, k -> new LinkedHashSet<>()).add(child);
    }

    private void unlinkChild(UUID sire, UUID child) {
        Set<UUID> kids = children.get(sire);
        if (kids != null) {
            kids.remove(child);
            if (kids.isEmpty()) {
                children.remove(sire);
            }
        }
    }

    /** Sets the child's sire (moving it if it had another). Nobody is their own sire. */
    public void add(UUID sire, UUID child, String entityType, String name) {
        if (sire.equals(child)) {
            return;
        }
        link(sire, child, entityType);
        names.put(child, name);
        setDirty();
    }

    /** The child has no sire any more. Its own children stay with it, and its name is kept while it is still a sire. */
    public void remove(UUID child) {
        Link old = links.remove(child);
        if (old == null) {
            return;
        }
        unlinkChild(old.sire(), child);
        if (!children.containsKey(child)) {
            names.remove(child);
        }
        setDirty();
    }

    /** Only for ids the index already knows (a sire or a child); anyone else is not worth a record. */
    public void setName(UUID id, String name) {
        if ((links.containsKey(id) || children.containsKey(id) || names.containsKey(id)) && !name.equals(names.put(id, name))) {
            setDirty();
        }
    }

    public Optional<String> nameOf(UUID id) {
        return Optional.ofNullable(names.get(id));
    }

    public List<Node> childrenOf(UUID sire) {
        List<Node> out = new ArrayList<>();
        for (UUID child : children.getOrDefault(sire, Set.of())) {
            out.add(new Node(child, links.get(child).entityType(), names.getOrDefault(child, child.toString())));
        }
        return out;
    }

    /** The entity type the child was recorded with ({@code minecraft:player} for a player); empty for an id that is only a sire. */
    /** Recorded as a creature, or one of the six NPC Originals. The top of a chain has no link of its own and no recorded type: it is taken for a player. */
    public boolean isCreature(UUID id) {
        return net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.byUuid(id).isPresent()
                || entityTypeOf(id).map(t -> !"minecraft:player".equals(t)).orElse(false);
    }

    public Optional<String> entityTypeOf(UUID child) {
        Link l = links.get(child);
        return l == null ? Optional.empty() : Optional.of(l.entityType());
    }

    public Optional<UUID> sireOf(UUID child) {
        Link l = links.get(child);
        return l == null ? Optional.empty() : Optional.of(l.sire());
    }

    /** Whether {@code ancestor} is somewhere above {@code id} (a sire, a sire's sire, ...). Nobody is their own descendant; a cycle ends the climb. */
    public boolean isDescendant(UUID ancestor, UUID id) {
        Set<UUID> seen = new HashSet<>();
        for (Link l = links.get(id); l != null && seen.add(l.sire()); l = links.get(l.sire())) {
            if (l.sire().equals(ancestor)) {
                return !ancestor.equals(id);
            }
        }
        return false;
    }

    /** @param gameTime the overworld's game time now */
    public void addPendingMob(UUID id, long gameTime) {
        prunePending(gameTime);
        if (pendingMobs.putIfAbsent(id, gameTime) == null) {
            setDirty();
        }
    }

    /** Stamps the debts that have no game time yet with {@code now}, and forgets those owed for more than {@link #PENDING_MAX_AGE_TICKS}. */
    public void prunePending(long now) {
        boolean changed = false;
        for (var it = pendingMobs.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            if (entry.getValue() == UNSTAMPED) {
                entry.setValue(now);
                changed = true;
            } else if (now - entry.getValue() > PENDING_MAX_AGE_TICKS) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public boolean isPendingMob(UUID id) {
        return pendingMobs.containsKey(id);
    }

    public boolean takePendingMob(UUID id) {
        boolean owed = pendingMobs.remove(id) != null;
        if (owed) {
            setDirty();
        }
        return owed;
    }

    /** For tests: a player the old sireline death owed. */
    public void addLegacyPendingPlayer(UUID id) {
        if (legacyPlayers.add(id)) {
            setDirty();
        }
    }

    /** @return true once: the old sireline death owed this player a death; the caller resolves their line */
    public boolean takeLegacyPendingPlayer(UUID id) {
        boolean owed = legacyPlayers.remove(id);
        if (owed) {
            setDirty();
        }
        return owed;
    }
}
