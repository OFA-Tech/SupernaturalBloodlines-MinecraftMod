package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.ArrayList;
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

    private record Link(UUID sire, String entityType) {
    }

    // child -> its sire; insertion order is the order the tree lists them in
    private final Map<UUID, Link> links = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> children = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

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
        return tag;
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

    public Optional<UUID> sireOf(UUID child) {
        Link l = links.get(child);
        return l == null ? Optional.empty() : Optional.of(l.sire());
    }
}
