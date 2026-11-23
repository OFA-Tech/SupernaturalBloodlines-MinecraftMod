package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.HashMap;
import java.util.HashSet;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupSwitch;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/**
 * What a group needs that the membership graph ({@link PackIndex}, never rewritten by this class) does not hold: a stable id
 * per leader, the type last derived and its stamp, and the stored ranks. Type-specific rows are kept when the type changes.
 */
public final class GroupIndex extends SavedData {
    public static final String NAME = "supernaturalbloodlines_groups";
    static final int DATA_VERSION = 1;
    private static final String VERSION = "data_version";
    private static final String IDS = "ids";
    private static final String TYPES = "types";
    private static final String TYPE_SINCE = "type_since";
    private static final String RANKS = "ranks";
    private static final Set<String> KNOWN = Set.of(VERSION, IDS, TYPES, TYPE_SINCE, RANKS);
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private final Map<UUID, UUID> ids = new HashMap<>();
    // raw strings: a type this build does not know is kept and written back
    private final Map<UUID, String> types = new HashMap<>();
    private final Map<UUID, Long> typeSince = new HashMap<>();
    private final Map<UUID, String> ranks = new HashMap<>();
    private CompoundTag extras = new CompoundTag();
    private int dataVersion;

    private static volatile GroupIndex live;
    private static volatile PackIndex livePacks;
    private static boolean registered;

    public GroupIndex() {
    }

    /** One index per world: always the overworld's storage. The first time it loads empty it is seeded from the packs. */
    public static GroupIndex get(ServerLevel level) {
        GroupIndex i = level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(GroupIndex::new, GroupIndex::load, null), NAME);
        PackIndex packs = PackIndex.get(level);
        live = i;
        livePacks = packs;
        if (i.dataVersion == 0) {
            i.seed(packs);
        }
        return i;
    }

    /** The follow listeners have no server in hand; they act on the index the last {@link #get} returned. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PackIndex.onLeadershipMoved((old, next) -> {
            GroupIndex i = live;
            if (i != null) {
                i.follow(old, next);
            }
        });
        PackIndex.onDissolved(leader -> {
            GroupIndex i = live;
            if (i != null) {
                i.drop(leader);
                PackIndex packs = livePacks;
                if (packs != null) {
                    i.prune(packs);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            live = null;
            livePacks = null;
        });
    }

    /** One {@code PACK} row per leader, once. A type is never derived here: an offline leader's facts cannot be read. */
    public void seed(PackIndex packs) {
        if (dataVersion != 0) {
            return;
        }
        long at = PackIndex.now();
        for (UUID leader : packs.leaders()) {
            if (!ids.containsKey(leader)) {
                UUID group = UUID.randomUUID();
                ids.put(leader, group);
                types.put(group, GroupType.PACK.id());
                typeSince.put(group, at);
            }
        }
        dataVersion = DATA_VERSION;
        setDirty();
    }

    public Optional<UUID> existingGroupOf(UUID leader) {
        return Optional.ofNullable(ids.get(leader));
    }

    /** The id of the leader's group, made (as a pack) when there is none. */
    public UUID groupOf(UUID leader) {
        return ensure(leader, GroupType.PACK, PackIndex.now());
    }

    /** The id of the leader's group; a new one is born with {@code type}. */
    public UUID ensure(UUID leader, GroupType type, long now) {
        UUID group = ids.get(leader);
        if (group == null) {
            group = UUID.randomUUID();
            ids.put(leader, group);
            types.put(group, type.id());
            typeSince.put(group, now);
            setDirty();
        }
        return group;
    }

    /** A type this build does not know answers {@code PACK} and stays stored as it was. */
    public GroupType typeOf(UUID group) {
        return GroupType.parse(types.getOrDefault(group, GroupType.PACK.id())).orElse(GroupType.PACK);
    }

    public void setType(UUID group, GroupType type, long now) {
        types.put(group, type.id());
        typeSince.put(group, now);
        setDirty();
    }

    public long typeSince(UUID group) {
        return typeSince.getOrDefault(group, 0L);
    }

    /** The stored rank, whatever the group's type is now (a row of another type is dormant, see {@link #activeRankOf}). */
    public GroupRank rankOf(UUID member) {
        return GroupRank.parse(ranks.getOrDefault(member, GroupRank.NONE.id()));
    }

    /** The rank as the group of type {@code active} sees it: a rank of another type is hidden, not deleted. */
    public GroupRank activeRankOf(UUID member, GroupType active) {
        GroupRank r = rankOf(member);
        return r == GroupRank.NONE || GroupSwitch.dormant(active, r.type()) ? GroupRank.NONE : r;
    }

    public void setRank(UUID member, GroupRank rank) {
        if (rank == GroupRank.NONE) {
            clearRanks(member);
        } else if (!rank.id().equals(ranks.put(member, rank.id()))) {
            setDirty();
        }
    }

    public void clearRanks(UUID member) {
        if (ranks.remove(member) != null) {
            setDirty();
        }
    }

    /** The id goes to the new leader; when that leader already has a group, the old leader's group is gone (its members joined theirs). */
    public void follow(UUID oldLeader, UUID newLeader) {
        UUID group = ids.remove(oldLeader);
        if (group == null) {
            return;
        }
        if (ids.containsKey(newLeader)) {
            types.remove(group);
            typeSince.remove(group);
        } else {
            ids.put(newLeader, group);
        }
        setDirty();
    }

    /** A dissolved group: its id and type rows. The ranks of members who are no longer in any group go with {@link #prune}. */
    public void drop(UUID leader) {
        UUID group = ids.remove(leader);
        if (group != null) {
            types.remove(group);
            typeSince.remove(group);
            setDirty();
        }
    }

    /** Forgets the ranks of people who are in no group. */
    public void prune(PackIndex packs) {
        boolean[] changed = {false};
        List.copyOf(ranks.keySet()).forEach(member -> {
            if (!packs.inPack(member)) {
                ranks.remove(member);
                changed[0] = true;
            }
        });
        if (changed[0]) {
            setDirty();
        }
    }

    public Set<UUID> leaders() {
        return Set.copyOf(ids.keySet());
    }

    public static GroupIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        GroupIndex i = new GroupIndex();
        i.dataVersion = tag.getInt(VERSION);
        readPairs(tag, IDS, (k, v) -> i.ids.put(NbtUtils.loadUUID(k), NbtUtils.loadUUID(v)));
        readPairs(tag, TYPES, (k, v) -> i.types.put(NbtUtils.loadUUID(k), v.getAsString()));
        readPairs(tag, TYPE_SINCE, (k, v) -> i.typeSince.put(NbtUtils.loadUUID(k), ((net.minecraft.nbt.NumericTag) v).getAsLong()));
        readPairs(tag, RANKS, (k, v) -> i.ranks.put(NbtUtils.loadUUID(k), v.getAsString()));
        for (String key : tag.getAllKeys()) {
            if (!KNOWN.contains(key)) {
                i.extras.put(key, tag.get(key).copy());
            }
        }
        return i;
    }

    private interface Row {
        void accept(Tag key, Tag value);
    }

    private static void readPairs(CompoundTag tag, String name, Row row) {
        for (Tag t : tag.getList(name, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                row.accept(e.get(KEY), e.get(VALUE));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged entry of '{}': {}", name, bad.toString());
            }
        }
    }

    private static CompoundTag row(UUID key) {
        CompoundTag e = new CompoundTag();
        e.put(KEY, NbtUtils.createUUID(key));
        return e;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (String key : extras.getAllKeys()) {
            tag.put(key, extras.get(key).copy());
        }
        tag.putInt(VERSION, dataVersion);
        ListTag idList = new ListTag();
        ids.forEach((k, v) -> {
            CompoundTag e = row(k);
            e.put(VALUE, NbtUtils.createUUID(v));
            idList.add(e);
        });
        tag.put(IDS, idList);
        ListTag typeList = new ListTag();
        types.forEach((k, v) -> {
            CompoundTag e = row(k);
            e.putString(VALUE, v);
            typeList.add(e);
        });
        tag.put(TYPES, typeList);
        ListTag sinceList = new ListTag();
        typeSince.forEach((k, v) -> {
            CompoundTag e = row(k);
            e.putLong(VALUE, v);
            sinceList.add(e);
        });
        tag.put(TYPE_SINCE, sinceList);
        ListTag rankList = new ListTag();
        ranks.forEach((k, v) -> {
            CompoundTag e = row(k);
            e.putString(VALUE, v);
            rankList.add(e);
        });
        tag.put(RANKS, rankList);
        return tag;
    }

    int dataVersion() {
        return dataVersion;
    }

    Set<UUID> groupIds() {
        return new HashSet<>(ids.values());
    }
}
