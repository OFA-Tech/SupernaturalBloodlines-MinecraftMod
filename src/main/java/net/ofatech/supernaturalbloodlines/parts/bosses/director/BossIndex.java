package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import org.jetbrains.annotations.Nullable;

/**
 * Once per world, written down (plan 17, decision 5): for each boss its state, the entity that is it, where it was last, when a desiccated Original
 * returns, whether the reward was paid. Not in the entity, so a lost or forgotten entity does not forget the story. A bad entry is skipped, not the file.
 */
public final class BossIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_bosses";
    private static final String BOSSES = "bosses";

    /** One boss's record. */
    public static final class Entry {
        public BossState state = BossState.UNSEEN;
        @Nullable
        public UUID entity;
        public String dimension = "";
        public BlockPos pos = BlockPos.ZERO;
        public long returnDay;
        public boolean rewarded;
        public int appearances;
        /** The first day on which a desiccated Original may drop their blood again. */
        public long bloodDay;
        /** The last night (day number) on which the director rolled for this boss; -1 for never. */
        public long lastRollDay = -1;
        /** The player a hunting boss came for. */
        @Nullable
        public UUID target;
        /** Game time (ticks) at which a boss that was announced arrives; -1 when none waits (Klaus warns a hunted player first). */
        public long arriveAt = -1;
        /** The seconds of the last countdown reminder shown to the hunted player (0 for none). */
        public int reminded;
        /** Elijah: an oath was broken, so he fights everybody for good and makes no more deals (R167). */
        public boolean hostile;
        /** The Beast of Gevaudan's encounter (R184): where it is, how many Hosts were made, whether the Spirit already came back, and the day the Host was made or the Spirit got away. */
        public net.ofatech.supernaturalbloodlines.core.boss.BeastPhase phase = net.ofatech.supernaturalbloodlines.core.boss.BeastPhase.NONE;
        public int hostsMade;
        public boolean returned;
        public long phaseDay;
        /** The Spirit entity and the game time at which it escapes. */
        @Nullable
        public UUID spirit;
        public long spiritUntil;
        /** Who did what to the Beast across the whole encounter (the Beast is replaced by the Host when it collapses, so the count cannot live on the entity). */
        public final net.ofatech.supernaturalbloodlines.core.boss.DamageTally tally = new net.ofatech.supernaturalbloodlines.core.boss.DamageTally();
        /** The players who were near when the Beast collapsed: told how it ends even if they have gone since. */
        public final java.util.Set<UUID> witnesses = new java.util.LinkedHashSet<>();
        /** Who the Mantle of the Beast is kept for: the one who did the most, who was not here when it was paid. It is handed over when they log in. */
        public final java.util.Set<UUID> owed = new java.util.LinkedHashSet<>();

        CompoundTag save(BossId id) {
            CompoundTag t = new CompoundTag();
            t.putString("boss", id.id());
            t.putString("state", state.name());
            if (entity != null) {
                t.putUUID("entity", entity);
            }
            t.putString("dim", dimension);
            t.put("pos", NbtUtils.writeBlockPos(pos));
            t.putLong("return_day", returnDay);
            t.putBoolean("rewarded", rewarded);
            t.putInt("appearances", appearances);
            t.putLong("blood_day", bloodDay);
            t.putLong("last_roll_day", lastRollDay);
            if (target != null) {
                t.putUUID("target", target);
            }
            t.putLong("arrive_at", arriveAt);
            t.putInt("reminded", reminded);
            t.putBoolean("hostile", hostile);
            t.putString("phase", phase.name());
            t.putInt("hosts_made", hostsMade);
            t.putBoolean("returned", returned);
            t.putLong("phase_day", phaseDay);
            if (spirit != null) {
                t.putUUID("spirit", spirit);
            }
            t.putLong("spirit_until", spiritUntil);
            ListTag hits = new ListTag();
            tally.entries().forEach((who, hit) -> {
                CompoundTag h = new CompoundTag();
                h.putUUID("who", who);
                h.putFloat("amount", hit.amount());
                h.putLong("seq", hit.seq());
                hits.add(h);
            });
            t.put("tally", hits);
            t.put("witnesses", uuids(witnesses));
            t.put("owed", uuids(owed));
            return t;
        }

        private static ListTag uuids(java.util.Set<UUID> set) {
            ListTag list = new ListTag();
            for (UUID id : set) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", id);
                list.add(c);
            }
            return list;
        }

        private static void readUuids(CompoundTag t, String key, java.util.Set<UUID> into) {
            for (Tag h : t.getList(key, Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) h;
                if (c.hasUUID("id")) {
                    into.add(c.getUUID("id"));
                }
            }
        }

        static Entry load(CompoundTag t) {
            Entry e = new Entry();
            e.state = BossState.valueOf(t.getString("state"));
            e.entity = t.hasUUID("entity") ? t.getUUID("entity") : null;
            e.dimension = t.getString("dim");
            e.pos = NbtUtils.readBlockPos(t, "pos").orElse(BlockPos.ZERO);
            e.returnDay = t.getLong("return_day");
            e.rewarded = t.getBoolean("rewarded");
            e.appearances = t.getInt("appearances");
            e.bloodDay = t.getLong("blood_day");
            e.lastRollDay = t.contains("last_roll_day") ? t.getLong("last_roll_day") : -1;
            e.target = t.hasUUID("target") ? t.getUUID("target") : null;
            e.arriveAt = t.contains("arrive_at") ? t.getLong("arrive_at") : -1;
            e.reminded = t.getInt("reminded");
            e.hostile = t.getBoolean("hostile");
            e.phase = net.ofatech.supernaturalbloodlines.core.boss.BeastPhase.of(t.getString("phase"));
            e.hostsMade = t.getInt("hosts_made");
            e.returned = t.getBoolean("returned");
            e.phaseDay = t.getLong("phase_day");
            e.spirit = t.hasUUID("spirit") ? t.getUUID("spirit") : null;
            e.spiritUntil = t.getLong("spirit_until");
            readUuids(t, "witnesses", e.witnesses);
            readUuids(t, "owed", e.owed);
            for (Tag h : t.getList("tally", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) h;
                if (c.hasUUID("who")) {
                    e.tally.put(c.getUUID("who"), c.getFloat("amount"), c.getLong("seq"));
                }
            }
            return e;
        }
    }

    private final Map<BossId, Entry> entries = new EnumMap<>(BossId.class);
    /** Elijah's deals: each player to the last day of their oath ({@link net.ofatech.supernaturalbloodlines.core.boss.DealRules#BROKEN} once they broke it). Absent: never dealt. */
    private final Map<UUID, Long> oaths = new java.util.HashMap<>();

    public static BossIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BossIndex::new, BossIndex::load, null), NAME);
    }

    public static BossIndex load(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        BossIndex index = new BossIndex();
        ListTag list = tag.getList(BOSSES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                BossId id = BossId.byId(t.getString("boss")).orElseThrow();
                index.entries.put(id, Entry.load(t));
            } catch (RuntimeException bad) {
                SupernaturalBloodlines.LOGGER.warn("Skipping a bad entry of the boss index: {}", bad.toString());
            }
        }
        ListTag oathList = tag.getList("oaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < oathList.size(); i++) {
            CompoundTag t = oathList.getCompound(i);
            if (t.hasUUID("player")) {
                index.oaths.put(t.getUUID("player"), t.getLong("until"));
            }
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((id, e) -> list.add(e.save(id)));
        tag.put(BOSSES, list);
        ListTag oathList = new ListTag();
        oaths.forEach((player, until) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("player", player);
            t.putLong("until", until);
            oathList.add(t);
        });
        tag.put("oaths", oathList);
        return tag;
    }

    /** The last day of this player's oath, {@link net.ofatech.supernaturalbloodlines.core.boss.DealRules#NEVER} when they never dealt with Elijah. */
    public long oathOf(UUID player) {
        return oaths.getOrDefault(player, net.ofatech.supernaturalbloodlines.core.boss.DealRules.NEVER);
    }

    public void setOath(UUID player, long until) {
        oaths.put(player, until);
        setDirty();
    }

    /** The record of a boss, made on first look. Callers that change it call {@link #setDirty()}. */
    public Entry of(BossId boss) {
        return entries.computeIfAbsent(boss, b -> new Entry());
    }

    public BossState state(BossId boss) {
        Entry e = entries.get(boss);
        return e == null ? BossState.UNSEEN : e.state;
    }

    /** Applies an event to the state by {@link BossRules}; returns the new state. */
    public BossState apply(BossId boss, BossRules.Event event) {
        Entry e = of(boss);
        e.state = BossRules.next(e.state, event);
        setDirty();
        return e.state;
    }

    /**
     * Where the boss is now: the entity that is it and its last place. A boss written down as lost that shows itself again is found again
     * (standing, or open when it is desiccated). Returns false for a second entity of a boss that is out there already (the old one that
     * came back after a new one was made): the caller discards it.
     */
    public boolean seen(BossId boss, UUID entity, String dimension, BlockPos pos, boolean desiccated) {
        Entry e = of(boss);
        if ((e.state == BossState.ACTIVE || e.state == BossState.OPEN) && e.entity != null && !entity.equals(e.entity)) {
            return false;
        }
        if (e.state == BossState.LOST) {
            e.state = BossRules.next(e.state, desiccated ? BossRules.Event.FOUND_OPEN : BossRules.Event.FOUND);
            setDirty();
        }
        adopt(boss, entity, dimension, pos);
        return true;
    }

    /** Writes down that this entity is the boss, wherever the index thought it was. */
    public void adopt(BossId boss, UUID entity, String dimension, BlockPos pos) {
        Entry e = of(boss);
        if (!entity.equals(e.entity) || !dimension.equals(e.dimension) || !pos.equals(e.pos)) {
            e.entity = entity;
            e.dimension = dimension;
            e.pos = pos.immutable();
            setDirty();
        }
    }

    /** Writes the index to disk now (the reward was just paid, and a crash must not pay it twice). */
    public static void flush(MinecraftServer server) {
        server.overworld().getDataStorage().save();
    }

    public Optional<BossId> bossOf(UUID entity) {
        for (Map.Entry<BossId, Entry> en : entries.entrySet()) {
            if (entity.equals(en.getValue().entity)) {
                return Optional.of(en.getKey());
            }
        }
        return Optional.empty();
    }

    /** Forgets everything about a boss (the operator's reset). */
    public void forget(BossId boss) {
        entries.remove(boss);
        if (boss == BossId.ELIJAH) {
            oaths.clear(); // a world that has never seen him has no deals either
        }
        setDirty();
    }
}
