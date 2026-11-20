package net.ofatech.supernaturalbloodlines.parts.bosses.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import org.jetbrains.annotations.Nullable;

/** The Linking Spells that are running (R177): who is linked to whom until when, and the damage a partner that was not loaded still owes. Survives a restart. */
public final class LinkIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_links";

    public record Link(UUID a, UUID b, long until) {
        public UUID other(UUID me) {
            return me.equals(a) ? b : a;
        }

        public boolean has(UUID e) {
            return a.equals(e) || b.equals(e);
        }
    }

    private final List<Link> links = new ArrayList<>();
    private final Map<UUID, Float> pending = new HashMap<>();

    public static LinkIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LinkIndex::new, LinkIndex::load, null), NAME);
    }

    public static LinkIndex load(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        LinkIndex index = new LinkIndex();
        ListTag list = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.hasUUID("a") && t.hasUUID("b")) {
                index.links.add(new Link(t.getUUID("a"), t.getUUID("b"), t.getLong("until")));
            }
        }
        ListTag owed = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < owed.size(); i++) {
            CompoundTag t = owed.getCompound(i);
            if (t.hasUUID("who")) {
                index.pending.put(t.getUUID("who"), t.getFloat("damage"));
            }
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Link l : links) {
            CompoundTag t = new CompoundTag();
            t.putUUID("a", l.a());
            t.putUUID("b", l.b());
            t.putLong("until", l.until());
            list.add(t);
        }
        tag.put("links", list);
        ListTag owed = new ListTag();
        pending.forEach((who, damage) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("who", who);
            t.putFloat("damage", damage);
            owed.add(t);
        });
        tag.put("pending", owed);
        return tag;
    }

    public Optional<Link> of(UUID entity) {
        return links.stream().filter(l -> l.has(entity)).findFirst();
    }

    /** Whether this one is in a link that still runs at {@code now}. */
    public boolean linked(UUID entity, long now) {
        return of(entity).filter(l -> LinkRules.active(l.until(), now)).isPresent();
    }

    public void add(UUID a, UUID b, long until) {
        links.add(new Link(a, b, until));
        setDirty();
    }

    public void remove(Link link) {
        if (links.remove(link)) {
            pending.remove(link.a());
            pending.remove(link.b());
            setDirty();
        }
    }

    /** Takes away every link that ran out; the damage owed to the ones that were in them is forgotten with them. */
    public void expire(long now) {
        for (Link l : List.copyOf(links)) {
            if (!LinkRules.active(l.until(), now)) {
                remove(l);
            }
        }
    }

    public void owe(UUID who, float damage) {
        pending.merge(who, LinkRules.addPending(0F, damage), LinkRules::addPending);
        setDirty();
    }

    /** What this one owes now (and forgets it). */
    public float settle(UUID who) {
        Float owed = pending.remove(who);
        if (owed != null) {
            setDirty();
        }
        return owed == null ? 0F : owed;
    }

    public int count() {
        return links.size();
    }

    public void clear() {
        links.clear();
        pending.clear();
        setDirty();
    }
}
