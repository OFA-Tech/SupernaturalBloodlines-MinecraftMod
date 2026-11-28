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
import net.ofatech.supernaturalbloodlines.core.group.MultiTypes;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
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
    private static final String CHANNEL = "channel";
    private static final String CONSENT = "consent";
    private static final String AT = "at";
    private static final String VOTES = "votes";
    private static final String BANS = "bans";
    private static final String COOLDOWNS = "cooldowns";
    private static final String CHARTERS = "charters";
    private static final String CONTESTS = "contests";
    private static final String MARKS = "marks";
    private static final String CURSES = "curses";
    private static final String BREAKERS = "code_breakers";
    private static final String MULTI_TYPES = "multi_types";
    private static final String TYPE_STAMPS = "type_stamps";
    private static final String MAIN_TYPE = "main_type";
    private static final Set<String> KNOWN = Set.of(VERSION, IDS, TYPES, TYPE_SINCE, RANKS, CHANNEL, CONSENT, VOTES, BANS, COOLDOWNS, CHARTERS, CONTESTS, MARKS, CURSES, BREAKERS, MULTI_TYPES, TYPE_STAMPS, MAIN_TYPE);
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String TYPE = "type";

    private final Map<UUID, UUID> ids = new HashMap<>();
    // raw strings: a type this build does not know is kept and written back
    private final Map<UUID, String> types = new HashMap<>();
    private final Map<UUID, Long> typeSince = new HashMap<>();
    // member -> (type id -> rank id): one rank per member per type, so a rank earned in another type is never overwritten by this one
    private final Map<UUID, Map<String, String>> ranks = new HashMap<>();
    // member -> the channel meter as it stood at a wall-clock time; a member with a full, idle meter has no row
    private final Map<UUID, ChannelRow> channel = new HashMap<>();
    // member -> consent id; only a level that is not the default
    private final Map<UUID, String> consent = new HashMap<>();
    // one open vote per kind per group (the group id, never the leader: the id follows the lead)
    private final Map<String, Vote> votes = new java.util.LinkedHashMap<>();
    // scope ("type:coven", or a group id for an exile) -> player -> until (wall-clock millis)
    private final Map<String, Map<UUID, Long>> bans = new HashMap<>();
    // group -> key ("election", a grand rite) -> until
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    // group -> its Charter (plan 20c); kept while the group is another type, dropped with the group
    private final Map<UUID, CharterRow> charters = new HashMap<>();
    // group -> its open challenge or running contest (plan 20c)
    private final Map<UUID, ContestRow> contests = new HashMap<>();
    // player rows of the brotherhood (plan 20d), not group rows: they outlive a lead change, a dissolved group and a type switch
    // plan 20e: multi-type group rows (set of active types, per-type stamps, computed main type)
    private final Map<UUID, Set<String>> multiTypes = new HashMap<>();
    private final Map<UUID, Map<String, Long>> typeStamps = new HashMap<>();
    private final Map<UUID, String> mainType = new HashMap<>();
    private final Map<UUID, Integer> marks = new HashMap<>();
    private final Map<UUID, Long> curses = new HashMap<>();
    private final Map<UUID, BreakerRow> breakers = new HashMap<>();
    private CompoundTag extras = new CompoundTag();
    private int dataVersion;

    private static volatile GroupIndex live;
    private static volatile PackIndex livePacks;
    private static boolean registered;

    /** A Code-breaker until {@code until} (wall-clock millis), with the wall-clock stamps of the breaks that still count towards an expulsion. */
    public record BreakerRow(long until, List<Long> strikes) {
        public BreakerRow {
            strikes = List.copyOf(strikes);
        }
    }

    /** What the channel meter held at {@code at} (wall-clock millis); it refills lazily from there ({@code ChannelMeter.valueAt}). */
    public record ChannelRow(int value, long at) {
    }

    /**
     * A clan's Charter as the server knows it; the item in a stack is only a token. {@code holder} and {@code entity} are empty when nobody or nothing is on
     * record. {@code byHead}: it is {@code STORED} because the clan's head was its last holder (the forfeit of a contest, whenever one comes).
     */
    public record CharterRow(UUID id, Optional<UUID> holder, CharterState state, long forgedAt, long seenAt, Optional<UUID> entity, long lostAt, boolean byHead) {
        public CharterRow(UUID id, Optional<UUID> holder, CharterState state, long forgedAt, long seenAt, Optional<UUID> entity, long lostAt) {
            this(id, holder, state, forgedAt, seenAt, entity, lostAt, false);
        }
    }

    /** The clan's one contest: a challenge waiting for the leader, or the Game of Kings running until its midnight. */
    public record ContestRow(UUID challenger, Phase phase, long openedAt, long acceptBy, long startedAt, long startedDayTime, boolean headStored) {
        public enum Phase { PENDING, RUNNING }

        public ContestRow(UUID challenger, Phase phase, long openedAt, long acceptBy, long startedAt, long startedDayTime) {
            this(challenger, phase, openedAt, acceptBy, startedAt, startedDayTime, false);
        }

        public ContestRow withHeadStored(boolean stored) {
            return new ContestRow(challenger, phase, openedAt, acceptBy, startedAt, startedDayTime, stored);
        }
    }

    /** A coven vote. Ballots are who voted what; a cancelled vote is kept and ignored. */
    public static final class Vote {
        public final UUID id;
        public final UUID group;
        public final VoteKind kind;
        public final UUID subject;
        public final UUID caller;
        public final long opened;
        public final long covenSince;
        public final long subjectSince;
        public final Map<UUID, Boolean> ballots = new java.util.LinkedHashMap<>();
        public boolean cancelled;

        private Vote(UUID id, UUID group, VoteKind kind, UUID subject, UUID caller, long opened, long covenSince, long subjectSince) {
            this.id = id;
            this.group = group;
            this.kind = kind;
            this.subject = subject;
            this.caller = caller;
            this.opened = opened;
            this.covenSince = covenSince;
            this.subjectSince = subjectSince;
        }

        public int yes() {
            return (int) ballots.values().stream().filter(b -> b).count();
        }

        public int no() {
            return (int) ballots.values().stream().filter(b -> !b).count();
        }
    }

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

    /** A type this build does not know answers {@code PACK} and stays stored as it was. Backward compat: returns main type. */
    public GroupType typeOf(UUID group) {
        Set<String> mt = multiTypes.get(group);
        if (mt != null && !mt.isEmpty()) {
            return MultiTypes.mainType(mt.stream().map(id -> GroupType.parse(id).orElse(null)).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()));
        }
        return GroupType.parse(types.getOrDefault(group, GroupType.PACK.id())).orElse(GroupType.PACK);
    }

    /** Backward compat: writes a singleton set alongside the old single-type keys. */
    public void setType(UUID group, GroupType type, long now) {
        types.put(group, type.id());
        typeSince.put(group, now);
        setTypeSet(group, Set.of(type), now);
    }

    public long typeSince(UUID group) {
        Map<String, Long> stamps = typeStamps.get(group);
        if (stamps != null) {
            String main = mainTypeOf(group).id();
            Long s = stamps.get(main);
            if (s != null) {
                return s;
            }
        }
        return typeSince.getOrDefault(group, 0L);
    }

    /** The active type set for a group; empty when no row exists. */
    public Set<GroupType> typesOf(UUID group) {
        Set<String> raw = multiTypes.get(group);
        if (raw == null || raw.isEmpty()) {
            String single = types.get(group);
            if (single != null) {
                return GroupType.parse(single).map(Set::of).orElse(Set.of());
            }
            return Set.of();
        }
        return raw.stream().map(id -> GroupType.parse(id).orElse(null)).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Writes the full type set with per-type stamps and computed main type; also populates old single-type keys for backward compat. */
    public void setTypeSet(UUID group, Set<GroupType> typeSet, long now) {
        Set<String> ids = typeSet.stream().map(GroupType::id).sorted().collect(java.util.stream.Collectors.toUnmodifiableSet());
        multiTypes.put(group, ids);
        Map<String, Long> stamps = typeStamps.computeIfAbsent(group, k -> new HashMap<>());
        for (String id : ids) {
            stamps.putIfAbsent(id, now);
        }
        stamps.keySet().retainAll(ids);
        GroupType main = MultiTypes.mainType(typeSet);
        mainType.put(group, main.id());
        types.put(group, main.id());
        typeSince.put(group, stamps.getOrDefault(main.id(), now));
        setDirty();
    }

    /** The main type of a group (first in canonical order); PACK when no row exists. */
    public GroupType mainTypeOf(UUID group) {
        String m = mainType.get(group);
        if (m != null) {
            return GroupType.parse(m).orElse(GroupType.PACK);
        }
        return typeOf(group);
    }

    /** The rank the member holds in a group of {@code type}; a rank of another type is dormant (kept, never shown), so it answers {@code NONE} here. */
    public GroupRank rankOf(UUID member, GroupType type) {
        Map<String, String> byType = ranks.get(member);
        GroupRank r = byType == null ? GroupRank.NONE : GroupRank.parse(byType.getOrDefault(type.id(), GroupRank.NONE.id()));
        return r == GroupRank.NONE || GroupSwitch.dormant(type, r.type()) ? GroupRank.NONE : r;
    }

    /** Stores {@code rank} under the type it belongs to; the member's ranks in the other types are left as they are. */
    public void setRank(UUID member, GroupRank rank) {
        if (rank == GroupRank.NONE) {
            throw new IllegalArgumentException("use clearRank(member, type) to take a rank away");
        }
        if (!rank.id().equals(ranks.computeIfAbsent(member, k -> new HashMap<>()).put(rank.type().id(), rank.id()))) {
            setDirty();
        }
    }

    /** Takes the member's rank in one type away (a new leader holds none of their group's type); the other types' ranks stay dormant. */
    public void clearRank(UUID member, GroupType type) {
        Map<String, String> byType = ranks.get(member);
        if (byType != null && byType.remove(type.id()) != null) {
            if (byType.isEmpty()) {
                ranks.remove(member);
            }
            setDirty();
        }
    }

    /** Takes every rank of the member away: they are in no group (left, kicked, or merged into another). */
    public void clearRanks(UUID member) {
        if (ranks.remove(member) != null) {
            setDirty();
        }
    }

    /** The stored meter of a member, or null: a full and idle meter is not stored. */
    @org.jetbrains.annotations.Nullable
    public ChannelRow channelOf(UUID member) {
        return channel.get(member);
    }

    /** Stores the meter; a meter that is back at {@code cap} stores nothing. */
    public void setChannel(UUID member, int value, long atMillis, int cap) {
        if (value >= cap) {
            if (channel.remove(member) != null) {
                setDirty();
            }
            return;
        }
        ChannelRow row = new ChannelRow(Math.max(0, value), atMillis);
        if (!row.equals(channel.put(member, row))) {
            setDirty();
        }
    }

    public Consent consentOf(UUID member) {
        return Consent.parse(consent.getOrDefault(member, Consent.NONE.id()));
    }

    /** Only a level that is not {@code NONE} is stored. */
    public void setConsent(UUID member, Consent level) {
        if (level == Consent.NONE ? consent.remove(member) != null : !level.id().equals(consent.put(member, level.id()))) {
            setDirty();
        }
    }

    private static String voteKey(UUID group, VoteKind kind) {
        return group + ":" + kind.id();
    }

    /** The open vote of this kind in the group, if any (a cancelled one is not open). */
    public Optional<Vote> openVote(UUID group, VoteKind kind) {
        return Optional.ofNullable(votes.get(voteKey(group, kind))).filter(v -> !v.cancelled);
    }

    /** Every open vote of the group, oldest first. */
    public List<Vote> openVotes(UUID group) {
        return votes.values().stream().filter(v -> v.group.equals(group) && !v.cancelled).sorted(java.util.Comparator.comparingLong((Vote v) -> v.opened)).toList();
    }

    public List<Vote> allVotes() {
        return List.copyOf(votes.values());
    }

    /** Opens a vote; one per kind per group, a cancelled row of the same kind is replaced. */
    public Vote openVote(UUID group, VoteKind kind, UUID subject, UUID caller, long now, long subjectSince) {
        Vote v = new Vote(UUID.randomUUID(), group, kind, subject, caller, now, typeSince(group), subjectSince);
        votes.put(voteKey(group, kind), v);
        setDirty();
        return v;
    }

    public void closeVote(Vote v) {
        if (votes.remove(voteKey(v.group, v.kind), v)) {
            setDirty();
        }
    }

    public void cancelVote(Vote v) {
        if (!v.cancelled) {
            v.cancelled = true;
            setDirty();
        }
    }

    /** A ballot is stored once; asking again changes nothing. */
    public boolean ballot(Vote v, UUID elector, boolean yes) {
        if (v.ballots.putIfAbsent(elector, yes) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean forgetBallot(Vote v, UUID elector) {
        if (v.ballots.remove(elector) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    /** The leader of the group with this id, if it has one. */
    public Optional<UUID> leaderOfGroup(UUID group) {
        return ids.entrySet().stream().filter(e -> e.getValue().equals(group)).map(Map.Entry::getKey).findFirst();
    }

    public void ban(String scope, UUID player, long until) {
        bans.computeIfAbsent(scope, k -> new HashMap<>()).put(player, until);
        setDirty();
    }

    /** 0 when there is no ban. */
    public long banUntil(String scope, UUID player) {
        Map<UUID, Long> m = bans.get(scope);
        return m == null ? 0 : m.getOrDefault(player, 0L);
    }

    /** The last recorded mark of a member (0 to 20), empty when none was recorded. */
    public java.util.OptionalInt markOf(UUID member) {
        Integer m = marks.get(member);
        return m == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(m);
    }

    public void setMark(UUID member, int mark) {
        Integer old = marks.put(member, mark);
        if (old == null || old != mark) {
            setDirty();
        }
    }

    public void clearMark(UUID member) {
        if (marks.remove(member) != null) {
            setDirty();
        }
    }

    public Map<UUID, Integer> marks() {
        return Map.copyOf(marks);
    }

    /** The wall-clock millis a timed curse ends at, empty when the player has none. */
    public java.util.OptionalLong curseUntil(UUID player) {
        Long u = curses.get(player);
        return u == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(u);
    }

    public void setCurse(UUID player, long until) {
        Long old = curses.put(player, until);
        if (old == null || old != until) {
            setDirty();
        }
    }

    public void clearCurse(UUID player) {
        if (curses.remove(player) != null) {
            setDirty();
        }
    }

    public Map<UUID, Long> curses() {
        return Map.copyOf(curses);
    }

    public Optional<BreakerRow> breakerOf(UUID player) {
        return Optional.ofNullable(breakers.get(player));
    }

    public void setBreaker(UUID player, BreakerRow row) {
        if (!row.equals(breakers.put(player, row))) {
            setDirty();
        }
    }

    public void clearBreaker(UUID player) {
        if (breakers.remove(player) != null) {
            setDirty();
        }
    }

    public Map<UUID, BreakerRow> breakers() {
        return Map.copyOf(breakers);
    }

    /** Drops the strikes the window has passed (a stamp from the future stays), and a row with no strikes whose breaker day is over. */
    public void pruneCode(long now, long windowMillis) {
        boolean[] changed = {false};
        for (UUID player : List.copyOf(breakers.keySet())) {
            BreakerRow row = breakers.get(player);
            List<Long> kept = net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules.strikesIn(row.strikes(), now, windowMillis);
            if (kept.isEmpty() && row.until() <= now) {
                breakers.remove(player);
                changed[0] = true;
            } else if (kept.size() != row.strikes().size()) {
                breakers.put(player, new BreakerRow(row.until(), kept));
                changed[0] = true;
            }
        }
        if (changed[0]) {
            setDirty();
        }
    }

    public void setCooldown(UUID group, String key, long until) {
        cooldowns.computeIfAbsent(group, k -> new HashMap<>()).put(key, until);
        setDirty();
    }

    /** Takes one cooldown away (a rank mark that no longer applies). */
    public void clearCooldown(UUID group, String key) {
        Map<String, Long> m = cooldowns.get(group);
        if (m != null && m.remove(key) != null) {
            if (m.isEmpty()) {
                cooldowns.remove(group);
            }
            setDirty();
        }
    }

    /** 0 when there is none. */
    public long cooldownUntil(UUID group, String key) {
        Map<String, Long> m = cooldowns.get(group);
        return m == null ? 0 : m.getOrDefault(key, 0L);
    }

    public Optional<CharterRow> charterOf(UUID group) {
        return Optional.ofNullable(charters.get(group));
    }

    public void setCharter(UUID group, CharterRow row) {
        if (!row.equals(charters.put(group, row))) {
            setDirty();
        }
    }

    public void clearCharter(UUID group) {
        if (charters.remove(group) != null) {
            setDirty();
        }
    }

    public Map<UUID, CharterRow> charters() {
        return Map.copyOf(charters);
    }

    public Optional<ContestRow> contestOf(UUID group) {
        return Optional.ofNullable(contests.get(group));
    }

    public void setContest(UUID group, ContestRow row) {
        if (!row.equals(contests.put(group, row))) {
            setDirty();
        }
    }

    public void clearContest(UUID group) {
        if (contests.remove(group) != null) {
            setDirty();
        }
    }

    public Map<UUID, ContestRow> contests() {
        return Map.copyOf(contests);
    }

    /** Forgets what is over: bans that ended, votes older than {@code maxAgeMillis}. */
    public void prune(long now, long maxAgeMillis) {
        boolean changed = pruneBansQuietly(now);
        changed |= votes.values().removeIf(v -> now - v.opened > maxAgeMillis);
        if (changed) {
            setDirty();
        }
    }

    /** Forgets only the bans (exiles and shunnings) that are over: a clan's sweep needs no coven to do it. */
    public void pruneBans(long now) {
        if (pruneBansQuietly(now)) {
            setDirty();
        }
    }

    private boolean pruneBansQuietly(long now) {
        boolean changed = false;
        for (Map<UUID, Long> m : bans.values()) {
            changed |= m.values().removeIf(until -> until <= now);
        }
        return changed | bans.values().removeIf(Map::isEmpty);
    }

    /** The id goes to the new leader; when that leader already has a group, the old leader's group is gone (its members joined theirs). */
    public void follow(UUID oldLeader, UUID newLeader) {
        UUID group = ids.remove(oldLeader);
        if (group == null) {
            return;
        }
        if (ids.containsKey(newLeader)) {
            retire(group);
        } else {
            ids.put(newLeader, group);
            // "the head stored it" is about the head who did: a new head inherits a stored Charter, not the old head's forfeit
            charters.computeIfPresent(group, (g, r) -> r.byHead() ? new CharterRow(r.id(), r.holder(), r.state(), r.forgedAt(), r.seenAt(), r.entity(), r.lostAt(), false) : r);
        }
        setDirty();
    }

    /** A dissolved group: its id and type rows. The ranks of members who are no longer in any group go with {@link #prune}. */
    public void drop(UUID leader) {
        UUID group = ids.remove(leader);
        if (group != null) {
            retire(group);
            setDirty();
        }
    }

    private void retire(UUID group) {
        types.remove(group);
        typeSince.remove(group);
        multiTypes.remove(group);
        typeStamps.remove(group);
        mainType.remove(group);
        votes.values().removeIf(v -> v.group.equals(group));
        cooldowns.remove(group);
        charters.remove(group);
        contests.remove(group);
        // an exile bars one clan by its group id: it ends with the clan
        bans.remove(group.toString());
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
        // a mark is the last one seen of somebody in a group; a group of another type keeps it (it sleeps), nobody in no group does
        changed[0] |= marks.keySet().removeIf(member -> !packs.inPack(member));
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
        for (Tag t : tag.getList(RANKS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                String type = e.getString(TYPE);
                if (!e.contains(TYPE) || type.isEmpty()) {
                    throw new IllegalArgumentException("a rank row with no type");
                }
                i.ranks.computeIfAbsent(NbtUtils.loadUUID(e.get(KEY)), k -> new HashMap<>()).put(type, e.get(VALUE).getAsString());
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged rank row: {}", bad.toString());
            }
        }
        for (Tag t : tag.getList(CHANNEL, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.channel.put(NbtUtils.loadUUID(e.get(KEY)), new ChannelRow(e.getInt(VALUE), e.getLong(AT)));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged channel row: {}", bad.toString());
            }
        }
        readPairs(tag, CONSENT, (k, v) -> i.consent.put(NbtUtils.loadUUID(k), v.getAsString()));
        for (Tag t : tag.getList(VOTES, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                VoteKind kind = VoteKind.parse(e.getString(TYPE)).orElseThrow(() -> new IllegalArgumentException("an unknown vote kind"));
                UUID id = e.contains("id") ? NbtUtils.loadUUID(e.get("id")) : UUID.randomUUID();
                UUID group = NbtUtils.loadUUID(e.get(KEY));
                long opened = e.getLong("opened");
                long currentTerm = i.typeSince(group);
                long covenSince = e.contains("coven_since") ? e.getLong("coven_since") : currentTerm;
                if (!i.ids.containsValue(group) || i.typeOf(group) != GroupType.COVEN || covenSince != currentTerm
                        || (!e.contains("coven_since") && opened < currentTerm)) {
                    i.setDirty();
                    continue;
                }
                long subjectSince = e.contains("subject_since") ? e.getLong("subject_since") : Long.MIN_VALUE;
                Vote v = new Vote(id, group, kind, NbtUtils.loadUUID(e.get("subject")), NbtUtils.loadUUID(e.get("caller")), opened, covenSince, subjectSince);
                if (!e.contains("id") || !e.contains("coven_since")) {
                    i.setDirty();
                }
                v.cancelled = e.getBoolean("cancelled");
                for (Tag b : e.getList("ballots", Tag.TAG_COMPOUND)) {
                    CompoundTag ballot = (CompoundTag) b;
                    v.ballots.put(NbtUtils.loadUUID(ballot.get(KEY)), ballot.getBoolean(VALUE));
                }
                i.votes.put(voteKey(v.group, v.kind), v);
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged vote row: {}", bad.toString());
            }
        }
        for (Tag t : tag.getList(BANS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.bans.computeIfAbsent(e.getString("scope"), k -> new HashMap<>()).put(NbtUtils.loadUUID(e.get("player")), e.getLong("until"));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged ban row: {}", bad.toString());
            }
        }
        for (Tag t : tag.getList(COOLDOWNS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.cooldowns.computeIfAbsent(NbtUtils.loadUUID(e.get(KEY)), k -> new HashMap<>()).put(e.getString(TYPE), e.getLong("until"));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged cooldown row: {}", bad.toString());
            }
        }
        int orphans = 0;
        for (Tag t : tag.getList(CHARTERS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                UUID group = NbtUtils.loadUUID(e.get(KEY));
                if (!i.ids.containsValue(group)) {
                    orphans++;
                    i.setDirty();
                    continue;
                }
                CharterState state = CharterState.parse(e.getString("state"));
                if (!e.contains("state")) {
                    throw new IllegalArgumentException("a charter row with no state");
                }
                i.charters.put(group, new CharterRow(NbtUtils.loadUUID(e.get("id")), e.contains("holder") ? Optional.of(NbtUtils.loadUUID(e.get("holder"))) : Optional.empty(),
                        state, e.getLong("forged_at"), e.getLong("seen_at"), e.contains("entity") ? Optional.of(NbtUtils.loadUUID(e.get("entity"))) : Optional.empty(), e.getLong("lost_at"), e.getBoolean("by_head")));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged charter row: {}", bad.toString());
            }
        }
        for (Tag t : tag.getList(CONTESTS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                UUID group = NbtUtils.loadUUID(e.get(KEY));
                if (!i.ids.containsValue(group)) {
                    orphans++;
                    i.setDirty();
                    continue;
                }
                ContestRow.Phase phase = ContestRow.Phase.valueOf(e.getString("phase"));
                i.contests.put(group, new ContestRow(NbtUtils.loadUUID(e.get("challenger")), phase, e.getLong("opened_at"), e.getLong("accept_by"), e.getLong("started_at"), e.getLong("started_day_time"), e.getBoolean("head_stored")));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged contest row: {}", bad.toString());
            }
        }
        readPairs(tag, MARKS, (k, v) -> i.marks.put(NbtUtils.loadUUID(k), ((net.minecraft.nbt.NumericTag) v).getAsInt()));
        readPairs(tag, CURSES, (k, v) -> i.curses.put(NbtUtils.loadUUID(k), ((net.minecraft.nbt.NumericTag) v).getAsLong()));
        for (Tag t : tag.getList(BREAKERS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                UUID player = NbtUtils.loadUUID(e.get(KEY));
                if (!(e.get("strikes") instanceof net.minecraft.nbt.LongArrayTag strikes) || !(e.get("until") instanceof net.minecraft.nbt.NumericTag)) {
                    throw new IllegalArgumentException("a code-breaker row with no strikes or no end");
                }
                List<Long> times = new java.util.ArrayList<>();
                for (long s : strikes.getAsLongArray()) {
                    times.add(s);
                }
                i.breakers.put(player, new BreakerRow(e.getLong("until"), times));
            } catch (RuntimeException bad) {
                net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index skipped a damaged code-breaker row: {}", bad.toString());
            }
        }
        if (orphans > 0) {
            net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The group index dropped {} charter or contest row(s) of groups that no longer exist", orphans);
        }
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
        ranks.forEach((member, byType) -> byType.forEach((type, rank) -> {
            CompoundTag e = row(member);
            e.putString(TYPE, type);
            e.putString(VALUE, rank);
            rankList.add(e);
        }));
        tag.put(RANKS, rankList);
        ListTag channelList = new ListTag();
        channel.forEach((member, row) -> {
            CompoundTag e = row(member);
            e.putInt(VALUE, row.value());
            e.putLong(AT, row.at());
            channelList.add(e);
        });
        tag.put(CHANNEL, channelList);
        ListTag consentList = new ListTag();
        consent.forEach((member, level) -> {
            CompoundTag e = row(member);
            e.putString(VALUE, level);
            consentList.add(e);
        });
        tag.put(CONSENT, consentList);
        ListTag voteList = new ListTag();
        votes.values().forEach(v -> {
            CompoundTag e = row(v.group);
            e.put("id", NbtUtils.createUUID(v.id));
            e.putString(TYPE, v.kind.id());
            e.put("subject", NbtUtils.createUUID(v.subject));
            e.put("caller", NbtUtils.createUUID(v.caller));
            e.putLong("opened", v.opened);
            e.putLong("coven_since", v.covenSince);
            e.putLong("subject_since", v.subjectSince);
            e.putBoolean("cancelled", v.cancelled);
            ListTag ballots = new ListTag();
            v.ballots.forEach((elector, yes) -> {
                CompoundTag b = row(elector);
                b.putBoolean(VALUE, yes);
                ballots.add(b);
            });
            e.put("ballots", ballots);
            voteList.add(e);
        });
        tag.put(VOTES, voteList);
        ListTag banList = new ListTag();
        bans.forEach((scope, players) -> players.forEach((player, until) -> {
            CompoundTag e = new CompoundTag();
            e.putString("scope", scope);
            e.put("player", NbtUtils.createUUID(player));
            e.putLong("until", until);
            banList.add(e);
        }));
        tag.put(BANS, banList);
        ListTag cooldownList = new ListTag();
        cooldowns.forEach((group, keys) -> keys.forEach((key, until) -> {
            CompoundTag e = row(group);
            e.putString(TYPE, key);
            e.putLong("until", until);
            cooldownList.add(e);
        }));
        tag.put(COOLDOWNS, cooldownList);
        ListTag charterList = new ListTag();
        charters.forEach((group, c) -> {
            CompoundTag e = row(group);
            e.put("id", NbtUtils.createUUID(c.id()));
            c.holder().ifPresent(h -> e.put("holder", NbtUtils.createUUID(h)));
            e.putString("state", c.state().id());
            e.putLong("forged_at", c.forgedAt());
            e.putLong("seen_at", c.seenAt());
            c.entity().ifPresent(en -> e.put("entity", NbtUtils.createUUID(en)));
            e.putLong("lost_at", c.lostAt());
            if (c.byHead()) {
                e.putBoolean("by_head", true);
            }
            charterList.add(e);
        });
        tag.put(CHARTERS, charterList);
        ListTag contestList = new ListTag();
        contests.forEach((group, c) -> {
            CompoundTag e = row(group);
            e.put("challenger", NbtUtils.createUUID(c.challenger()));
            e.putString("phase", c.phase().name());
            e.putLong("opened_at", c.openedAt());
            e.putLong("accept_by", c.acceptBy());
            e.putLong("started_at", c.startedAt());
            e.putLong("started_day_time", c.startedDayTime());
            if (c.headStored()) {
                e.putBoolean("head_stored", true);
            }
            contestList.add(e);
        });
        tag.put(CONTESTS, contestList);
        ListTag markList = new ListTag();
        marks.forEach((member, mark) -> {
            CompoundTag e = row(member);
            e.putInt(VALUE, mark);
            markList.add(e);
        });
        tag.put(MARKS, markList);
        ListTag curseList = new ListTag();
        curses.forEach((player, until) -> {
            CompoundTag e = row(player);
            e.putLong(VALUE, until);
            curseList.add(e);
        });
        tag.put(CURSES, curseList);
        ListTag breakerList = new ListTag();
        breakers.forEach((player, b) -> {
            CompoundTag e = row(player);
            e.putLong("until", b.until());
            e.put("strikes", new net.minecraft.nbt.LongArrayTag(b.strikes().stream().mapToLong(Long::longValue).toArray()));
            breakerList.add(e);
        });
        tag.put(BREAKERS, breakerList);
        return tag;
    }

    static Set<String> knownKeys() {
        return KNOWN;
    }

    int dataVersion() {
        return dataVersion;
    }

    Set<UUID> groupIds() {
        return new HashSet<>(ids.values());
    }
}
