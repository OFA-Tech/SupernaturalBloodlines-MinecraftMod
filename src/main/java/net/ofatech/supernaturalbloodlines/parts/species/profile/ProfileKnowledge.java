package net.ofatech.supernaturalbloodlines.parts.species.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.KnownFacts;

/**
 * What each player has discovered about other people, and the pool of what each pack has discovered. A discovery is per observer and stays
 * (leaving a pack keeps it); a discovery made by anyone in a pack goes to every current member at once and to the pack's pool for whoever
 * joins next. It never invents a fact: only {@link #discover} writes. Sizes are capped ({@link KnownFacts#MAX_SUBJECTS} subjects each,
 * {@value #MAX_OWNERS} owners), and a save that is damaged or from another version loads what it can instead of failing.
 */
public final class ProfileKnowledge extends SavedData {
    public static final String NAME = "supernaturalbloodlines_profile_knowledge";
    public static final int MAX_OWNERS = 1024;

    private final LinkedHashMap<UUID, KnownFacts> observers = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, KnownFacts> pools = new LinkedHashMap<>();

    public static ProfileKnowledge get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(ProfileKnowledge::new, ProfileKnowledge::load, null), NAME);
    }

    /** What {@code observer} knows of {@code subject}: their own discoveries and, while they are in a pack, the pack's pool (kept once seen). */
    public Set<Fact> known(PackIndex packs, UUID observer, UUID subject) {
        // a read never creates an entry: a profile asked for by someone nobody has discovered anything for must not push a real observer out of the capped map
        Set<Fact> out = EnumSet.copyOf(peek(observers, observer).facts(subject));
        if (packs != null && packs.inPack(observer)) {
            Set<Fact> pooled = peek(pools, packs.leaderOf(observer)).facts(subject);
            if (!out.containsAll(pooled)) {
                out.addAll(pooled);
                owned(observers, observer).add(subject, pooled);
                setDirty();
            }
        }
        return out;
    }

    /** {@code observer} learned {@code facts} of {@code subject}. A pack-mate learns it too, at once, and so does the pack's pool. */
    public void discover(PackIndex packs, UUID observer, UUID subject, Collection<Fact> facts) {
        if (facts.isEmpty()) {
            return;
        }
        owned(observers, observer).add(subject, facts);
        if (packs != null && packs.inPack(observer)) {
            owned(pools, packs.leaderOf(observer)).add(subject, facts);
            for (UUID member : packs.pack(observer)) {
                owned(observers, member).add(subject, facts);
            }
        }
        setDirty();
    }

    /** The pack changed hands: its pool goes to the new leader (joined with any the new leader already had). */
    public void movePool(UUID oldLeader, UUID newLeader) {
        KnownFacts pool = pools.remove(oldLeader);
        if (pool != null && !oldLeader.equals(newLeader)) {
            owned(pools, newLeader).mergeFrom(pool);
            setDirty();
        }
    }

    /** The pack dissolved: nobody leads it, so its pool is dropped (what members already learned stays with them). */
    public void dropPool(UUID leader) {
        if (pools.remove(leader) != null) {
            setDirty();
        }
    }

    /** Whether the pack led by {@code leader} has a pool (for tests). */
    public boolean hasPool(UUID leader) {
        return pools.containsKey(leader);
    }

    private static final KnownFacts NOTHING = new KnownFacts();
    private static int evicted;

    /** How many owners the cap has pushed out since the server started (for tests); the first is logged. */
    public static int evictedOwners() {
        return evicted;
    }

    public int observerCount() {
        return observers.size();
    }

    private static KnownFacts peek(LinkedHashMap<UUID, KnownFacts> map, UUID owner) {
        return map.getOrDefault(owner, NOTHING);
    }

    private static KnownFacts owned(LinkedHashMap<UUID, KnownFacts> map, UUID owner) {
        KnownFacts k = map.remove(owner);
        if (k == null) {
            k = new KnownFacts();
        }
        map.put(owner, k);
        while (map.size() > MAX_OWNERS) {
            map.remove(map.keySet().iterator().next());
            if (evicted++ == 0) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("Profile knowledge is full ({} owners): the least recently active player's discoveries are being dropped", MAX_OWNERS);
            }
        }
        return k;
    }

    // ---- storage ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("observers", write(observers));
        tag.put("pools", write(pools));
        return tag;
    }

    public static ProfileKnowledge load(CompoundTag tag, HolderLookup.Provider registries) {
        ProfileKnowledge k = new ProfileKnowledge();
        read(tag, "observers", k.observers);
        read(tag, "pools", k.pools);
        return k;
    }

    private static ListTag write(Map<UUID, KnownFacts> map) {
        ListTag out = new ListTag();
        map.forEach((owner, known) -> {
            if (known.isEmpty()) {
                return;
            }
            CompoundTag e = new CompoundTag();
            e.putString("owner", owner.toString());
            ListTag subjects = new ListTag();
            known.toStrings().forEach((subject, facts) -> {
                CompoundTag s = new CompoundTag();
                s.putString("subject", subject);
                ListTag names = new ListTag();
                facts.forEach(f -> names.add(StringTag.valueOf(f)));
                s.put("facts", names);
                subjects.add(s);
            });
            e.put("known", subjects);
            out.add(e);
        });
        return out;
    }

    /** Lenient: a wrong tag type, a bad id or a fact this version does not know skips that entry only; more owners than the cap keep the newest. */
    private static void read(CompoundTag tag, String key, LinkedHashMap<UUID, KnownFacts> into) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        for (Tag t : tag.getList(key, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                UUID owner = UUID.fromString(e.getString("owner"));
                Map<String, List<String>> raw = new LinkedHashMap<>();
                for (Tag st : e.getList("known", Tag.TAG_COMPOUND)) {
                    CompoundTag s = (CompoundTag) st;
                    List<String> names = new ArrayList<>();
                    for (Tag n : s.getList("facts", Tag.TAG_STRING)) {
                        names.add(n.getAsString());
                    }
                    raw.put(s.getString("subject"), names);
                }
                owned(into, owner).mergeFrom(KnownFacts.fromStrings(raw));
            } catch (RuntimeException damaged) {
                // this entry is lost, the rest of the save is not
            }
        }
    }
}

