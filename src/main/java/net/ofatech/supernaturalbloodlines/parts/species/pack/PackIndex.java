package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackMoonRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * Who is in whose pack, online or not (a pack survives its Alpha logging out), plus each Alpha's emissary and each
 * werewolf's anchor. The pack of a Beta also lives in {@code WolfState.packAlpha}; a {@link BloodlineAccess} listener keeps
 * this index in step with it, so the rules' own writes are covered.
 */
public final class PackIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_packs";
    private static final String MEMBERS = "members";
    private static final String EMISSARIES = "emissaries";
    private static final String ANCHORS = "anchors";
    private static final String FORMER = "former_alphas";
    private static final String DISSOLVED = "dissolved";
    private static final String STRENGTHS = "strengths";
    private static final String ALLIES = "allies";
    private static final String MIRRORS = "party_mirrors";
    private static final String PARTIES = "party_ids";
    private static final String ENDED = "party_ended";
    private static final String NOTICES = "notices";
    private static final String SPARKS = "sparks";
    private static final String LAST_SEEN = "last_seen";
    private static final String JOINED_AT = "joined_at";
    private static final String ALIVE_AT = "alive_at";
    private static final String HYBRID_LIKE = "hybrid_like";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    // beta -> alpha, insertion order is the order they joined
    private final Map<UUID, UUID> alphaOf = new LinkedHashMap<>();
    private final Map<UUID, UUID> emissaryOf = new HashMap<>();
    private final Map<UUID, UUID> anchorOf = new HashMap<>();
    // players who lost the spark (stolen, passed on, given up): an offline Alpha is still an Alpha unless they are here
    private final Set<UUID> formerAlphas = new HashSet<>();
    // Betas of a pack that dissolved while they were away: they become Omegas at their next login
    private final Set<UUID> dissolved = new HashSet<>();
    // the packed strength (PackLeadership.strength) each member last had while online: an offline Alpha still counts
    private final Map<UUID, Integer> strengths = new HashMap<>();
    // leader -> the leaders of the packs allied with it (kept both ways)
    private final Map<UUID, Set<UUID>> allies = new HashMap<>();
    // member -> the owner of the Open Parties and Claims party we put them in (see PartyLink)
    private final Map<UUID, UUID> mirrors = new HashMap<>();
    // owner -> id of the party the pack made for them: the only party a pack ever touches
    private final Map<UUID, UUID> parties = new HashMap<>();
    // owners whose recorded party vanished: nothing is made again until the roster changes
    private final Set<UUID> ended = new HashSet<>();
    // what to tell a player who was away when their pack changed: reason key and one argument
    private final Map<UUID, List<String[]>> notices = new HashMap<>();
    // the spark count each Alpha last had while online: a pack's offline Alphas are shown with it, and the strongest is picked by it
    private final Map<UUID, Integer> sparks = new HashMap<>();
    // when (wall clock, milliseconds) a member of a pack was last seen: logged out, or renewed every hour while online (only members are recorded)
    private final Map<UUID, Long> lastSeen = new HashMap<>();
    // when a member joined the pack they are in now (wall-clock millis, {@link #now}); the hub shows it as days in the pack
    private final Map<UUID, Long> joinedAt = new HashMap<>();
    // players who were hybrid-like (a hybrid type, curse not bound) the last time they were seen online: an offline leader still gives the pack gift (R96)
    private final Set<UUID> hybridLike = new HashSet<>();
    // the last wall-clock time the server was known to be running (0: never recorded): the gap to the next start is downtime, not an absence
    private long aliveAt;

    /** The wall clock the grace windows and last-seen times use; a test pins it. */
    private static volatile java.util.function.LongSupplier clock = System::currentTimeMillis;

    public static void overrideClock(java.util.function.LongSupplier c) {
        clock = c == null ? System::currentTimeMillis : c;
    }

    public static long now() {
        return clock.getAsLong();
    }

    /** Who a player left and when (wall clock). In memory only: a restart forgets it, which only shortens the grace. */
    private record Left(UUID leader, long at) {
    }

    private final Map<UUID, Left> leftAt = new HashMap<>();
    /** The notice an absent leader gets is never dropped to make room for another one. */
    public static final String KEPT_NOTICE = "supernaturalbloodlines.pack.absent_you";
    /** A short list: the newest few are kept for a player who stays away. */
    public static final int MAX_NOTICES = 8;
    // members the party link already told that they could not be added (in memory: told again once after a restart, not once per retry)
    private final Set<UUID> partyRefused = new HashSet<>();

    /** A party the pack made: its owner and its id. */
    public record Recorded(UUID owner, UUID id) {
    }
    public PackIndex() {
    }

    /** One index per world: always the overworld's storage, whichever level is passed. */
    public static PackIndex get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(PackIndex::new, PackIndex::load, null), NAME);
    }

    /** Set while a Beta is made a second Alpha of the pack they are in (the Alpha Trial): they stay in it. */
    static final ThreadLocal<Boolean> KEEP_PACK = ThreadLocal.withInitial(() -> false);

    /**
     * Keeps the index in step with {@code WolfState.packAlpha}: on every change, and at login (heals a lost file). A cured
     * werewolf and a Beta who becomes an Alpha of their own (not through the trial) leave the pack they were in; a Packmate
     * (not a werewolf) and a co-Alpha live in the index only.
     */
    public static void register() {
        registerWolf();
        registerSession();
    }

    /** The wolf-state sync: Werewolves worlds only. */
    public static void registerWolf() {
        BloodlineAccess.addListener((p, change) -> {
            WolfState before = change.before().wolf();
            WolfState after = change.after().wolf();
            boolean ownSpark = before.rank() == Rank.BETA && PackAccess.isAlpha(after.rank()) && !KEEP_PACK.get();
            PackIndex index = get(p.serverLevel());
            if (ownSpark) {
                index.leave(p.getUUID());
            } else if (before.lineage() == Lineage.NONE && after.lineage() != Lineage.NONE && after.packAlpha() == null && !PackAccess.isAlpha(after.rank())
                    && index.alphaOf(p.getUUID()).isPresent()) {
                // a Packmate who becomes a werewolf is a Beta of the pack they are in (a cured Beta, the other way, stays as a Packmate)
                UUID leader = index.alphaOf(p.getUUID()).get();
                BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(after.toBuilder().rank(Rank.BETA).packAlpha(leader).betaSince(p.level().getGameTime()).build()).build());
            } else if (!Objects.equals(before.packAlpha(), after.packAlpha())) {
                follow(p);
            }
            // before the strength is refreshed: whatever reads the pack from there must see the new flag
            index.setHybridLike(p.getUUID(), PackMoonRules.hybridLike(change.after().species(), change.after().hybridCurseBound()));
            if (before.rank() != after.rank() || before.lineage() != after.lineage() || before.sparks() != after.sparks()) {
                PackLeader.refresh(p, after);
            }
            // a dormant True Alpha (outside the faction) keeps its tombstone until it rejoins
            if (PackAccess.isAlpha(change.after().wolf().rank()) && VampirismSupernatural.isWerewolf(p)) {
                get(p.serverLevel()).clearFormerAlpha(p.getUUID());
            }
        });
    }

    /** Login (notices, last seen, dissolved) and logout (last seen): every group type needs them, with or without Werewolves. */
    public static void registerSession() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                login(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !p.level().isClientSide) {
                PackIndex index = get(p.serverLevel());
                if (index.inPack(p.getUUID())) {
                    index.setLastSeen(p.getUUID(), now());
                }
            }
        });
    }

    /**
     * Login: a Beta whose pack dissolved while they were away is an Omega now, and one whose Alpha lost the spark to
     * someone else (the index moved the pack, their own state still names the old Alpha) follows the index.
     */
    public static void login(ServerPlayer p) {
        PackIndex index = get(p.serverLevel());
        if (index.inPack(p.getUUID())) {
            index.setLastSeen(p.getUUID(), now());
        }
        index.takeNotices(p.getUUID()).forEach(n -> p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(n[0], n[1])));
        if (index.takeDissolved(p.getUUID())) {
            PackAccess.applyPack(p, PackEvent.LEFT_PACK, PackContext.defaults());
        }
        WolfState w = PackAccess.wolf(p);
        // shortcut: only an ex-Alpha the index still has a tombstone for is marked as having held a spark; one who lost it before this version without a tombstone is not caught and counts as never having held one, upgrade: none possible, the history was never recorded
        if (index.isFormerAlpha(p.getUUID()) && w.rank() != WolfState.Rank.TRUE_ALPHA && !w.heldSpark()) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().heldSpark(true).build()).build());
            w = PackAccess.wolf(p);
        }
        UUID indexed = index.alphaOf(p.getUUID()).orElse(null);
        if (w.packAlpha() != null && indexed != null && !indexed.equals(w.packAlpha())
                && (index.isFormerAlpha(w.packAlpha()) || indexed.equals(index.leaderOf(w.packAlpha())))) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().packAlpha(indexed).build()).build());
        }
        follow(p);
        BloodlineState own = BloodlineAccess.get(p);
        index.setHybridLike(p.getUUID(), PackMoonRules.hybridLike(own.species(), own.hybridCurseBound()));
        PackLeader.refresh(p);
    }

    private static void follow(ServerPlayer p) {
        PackIndex index = get(p.serverLevel());
        WolfState w = PackAccess.wolf(p);
        UUID alpha = w.packAlpha();
        // a co-Alpha and a Packmate (no werewolf state, no Alpha of their own named) live in the index only
        if (PackAccess.isAlpha(w.rank()) || (alpha == null && w.lineage() == Lineage.NONE)) {
            return;
        }
        if (!Objects.equals(index.alphaOf(p.getUUID()).orElse(null), alpha)) {
            index.leave(p.getUUID());
            if (alpha != null) {
                index.join(alpha, p.getUUID());
            }
        }
    }

    /** The leader of the pack {@code p} is in, or {@code p} itself when they lead (or are alone). */
    public UUID leaderOf(UUID p) {
        return alphaOf.getOrDefault(p, p);
    }

    /** Whether {@code p} is in a pack of anyone else: a member, or a leader with members. */
    public boolean inPack(UUID p) {
        return alphaOf.containsKey(p) || alphaOf.containsValue(p);
    }

    /** The whole pack of {@code p}: the leader first, then every member in the order they joined. */
    public List<UUID> pack(UUID p) {
        UUID leader = leaderOf(p);
        List<UUID> out = new ArrayList<>();
        out.add(leader);
        out.addAll(betasOf(leader));
        return out;
    }

    /** Whether {@code id} was hybrid-like (a hybrid type with the curse not bound) when they were last seen: the leader's half of the pack gift (R96). */
    public boolean isHybridLike(UUID id) {
        return hybridLike.contains(id);
    }

    public void setHybridLike(UUID id, boolean value) {
        if (value ? hybridLike.add(id) : hybridLike.remove(id)) {
            setDirty();
        }
    }

    /** Whether {@code p} leads the pack they are in (a lone player leads their own). */
    public boolean isLeader(UUID p) {
        return !alphaOf.containsKey(p);
    }

    /** Both are in the same pack (not the same player). */
    public boolean samePackNow(UUID a, UUID b) {
        return !a.equals(b) && inPack(a) && leaderOf(a).equals(leaderOf(b));
    }

    public Optional<Recorded> recordedParty(List<UUID> pack) {
        for (UUID owner : pack) {
            UUID id = parties.get(owner);
            if (id != null) {
                return Optional.of(new Recorded(owner, id));
            }
        }
        return Optional.empty();
    }

    public void recordParty(UUID owner, UUID id) {
        parties.put(owner, id);
        setDirty();
    }

    /** Forgets the party and everyone mirrored into it (the party itself is not touched). */
    public void forgetParty(UUID owner) {
        boolean was = parties.remove(owner) != null;
        was |= mirrors.values().removeIf(owner::equals);
        if (was) {
            setDirty();
        }
    }

    public boolean partyEnded(List<UUID> pack) {
        return pack.stream().anyMatch(ended::contains);
    }

    public void setPartyEnded(UUID owner, boolean value) {
        if (value ? ended.add(owner) : ended.remove(owner)) {
            setDirty();
        }
    }

    public void addNotice(UUID player, String key, String arg) {
        List<String[]> list = notices.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(new String[] {key, arg});
        while (list.size() > MAX_NOTICES) {
            // the oldest goes, but never the one that says you lost the lead
            int drop = 0;
            while (drop < list.size() - 1 && list.get(drop)[0].equals(KEPT_NOTICE)) {
                drop++;
            }
            list.remove(drop);
        }
        setDirty();
    }

    /** Everything that waits for {@code player}, oldest first. */
    public List<String[]> takeNotices(UUID player) {
        List<String[]> n = notices.remove(player);
        if (n != null) {
            setDirty();
        }
        return n == null ? List.of() : n;
    }

    /** The oldest notice that waits for {@code player}. */
    public Optional<String[]> takeNotice(UUID player) {
        List<String[]> n = notices.get(player);
        if (n == null || n.isEmpty()) {
            return Optional.empty();
        }
        String[] first = n.remove(0);
        if (n.isEmpty()) {
            notices.remove(player);
        }
        setDirty();
        return Optional.of(first);
    }

    public int noticeCount(UUID player) {
        return notices.getOrDefault(player, List.of()).size();
    }

    /** @return true the first time (until {@link #clearPartyRefused}): the player should be told once, not at every retry */
    public boolean markPartyRefused(UUID member) {
        return partyRefused.add(member);
    }

    public void clearPartyRefused(UUID member) {
        partyRefused.remove(member);
    }

    /** The owners of the parties the packs made (see {@code PartyLink}). */
    public Set<UUID> partyOwners() {
        return new HashSet<>(parties.keySet());
    }

    /** Every leader that has members. */
    public Set<UUID> leaders() {
        return new HashSet<>(alphaOf.values());
    }

    /** The members of {@code pack} (and anyone else still marked as put in a party by an owner in it) we mirrored. */
    public Set<UUID> mirrorsOf(List<UUID> pack) {
        Set<UUID> out = new HashSet<>();
        mirrors.forEach((member, owner) -> {
            if (pack.contains(owner)) {
                out.add(member);
            }
        });
        return out;
    }

    public Optional<UUID> mirrorOwner(UUID member) {
        return Optional.ofNullable(mirrors.get(member));
    }

    public void setMirror(UUID member, @Nullable UUID owner) {
        put(mirrors, member, owner);
    }

    public int sparksOf(UUID id) {
        return sparks.getOrDefault(id, 1);
    }

    public void setSparks(UUID id, int count) {
        Integer old = count <= 1 ? sparks.remove(id) : sparks.put(id, count);
        if ((count <= 1 && old != null) || (count > 1 && (old == null || old != count))) {
            setDirty();
        }
    }

    /**
     * Since when {@code id} has been in their pack: a member's own join, a leader's the first join of any member they lead (the pack's age).
     * Empty for someone in no pack, or a leader none of whose members has a recorded join (a pack from before joins were recorded is stamped when the index loads).
     */
    public Optional<Long> inPackSince(UUID id) {
        if (!inPack(id) && !isLeader(id)) {
            return Optional.empty();
        }
        Long own = joinedAt.get(id);
        if (own != null && alphaOf.containsKey(id)) {
            return Optional.of(own);
        }
        return alphaOf.entrySet().stream().filter(e -> e.getValue().equals(id)).map(e -> joinedAt.get(e.getKey())).filter(java.util.Objects::nonNull).min(Long::compare);
    }

    /** When {@code id} last left (wall clock milliseconds), if the index ever saw it. */
    public Optional<Long> lastSeen(UUID id) {
        return Optional.ofNullable(lastSeen.get(id));
    }

    /** The last time the server was known to be running, or 0 for a world that never recorded it. */
    public long aliveAt() {
        return aliveAt;
    }

    public void setAliveAt(long millis) {
        if (millis != aliveAt) {
            aliveAt = millis;
            setDirty();
        }
    }

    /**
     * Every recorded time moves forward by {@code millis}: the server was off for that long, and nobody was away in it. A time never
     * moves past {@code cap} (the present): a clock that ran backwards must not leave a time in the future.
     */
    public void shiftLastSeen(long millis, long cap) {
        if (millis > 0 && !lastSeen.isEmpty()) {
            lastSeen.replaceAll((id, t) -> Math.max(t, Math.min(t + millis, cap)));
            setDirty();
        }
    }

    /**
     * Whether {@code a} and {@code b} count as one pack for what a kill does: their packs are the same now, or one of them left a
     * pack of the other's (by {@code /pack leave}, within the grace window) whatever pack either of them is in since. A kicked
     * member and a dissolved pack get no grace.
     */
    public boolean killsInPack(UUID a, UUID b, int graceMinutes) {
        if (a.equals(b)) {
            return false;
        }
        Set<UUID> kb = packKeys(b, graceMinutes);
        return packKeys(a, graceMinutes).stream().anyMatch(kb::contains);
    }

    /**
     * The packs {@code p} counts as being in: the one they are in now (a player of no pack is their own key, so a leader left alone
     * still matches who left them), and the one they left a moment ago, found again through its old leader so that a change of
     * leader or of pack since does not lose it.
     */
    private Set<UUID> packKeys(UUID p, int graceMinutes) {
        Set<UUID> keys = new HashSet<>();
        keys.add(inPack(p) ? leaderOf(p) : p);
        Left l = leftAt.get(p);
        if (l != null) {
            if (graceMinutes > 0 && now() - l.at < graceMinutes * 60_000L) {
                keys.add(leaderOf(l.leader));
            }
        }
        return keys;
    }

    /** True when the grace (not the pack they are in now) is what makes {@code a} and {@code b} count as one pack. */
    public boolean onlyByGrace(UUID a, UUID b, int graceMinutes) {
        return killsInPack(a, b, graceMinutes) && !killsInPack(a, b, 0);
    }

    /** {@link #killsInPack} for two players in a world, with the configured grace: what a kill between them does. */
    public static boolean samePack(net.minecraft.world.entity.Entity a, net.minecraft.world.entity.Entity b) {
        return a instanceof ServerPlayer x && b instanceof ServerPlayer y
                && get(x.serverLevel()).killsInPack(x.getUUID(), y.getUUID(), net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.leftPackGraceMinutes());
    }

    public void setLastSeen(UUID id, long millis) {
        Long old = lastSeen.put(id, millis);
        if (old == null || old != millis) {
            setDirty();
        }
    }

    public int strengthOf(UUID id) {
        return strengths.getOrDefault(id, 0);
    }

    public void setStrength(UUID id, int strength) {
        Integer old = strengths.put(id, strength);
        if (old == null || old != strength) {
            setDirty();
        }
    }

    public Optional<UUID> alphaOf(UUID beta) {
        return Optional.ofNullable(alphaOf.get(beta));
    }

    /** Every member of the pack led by {@code alpha}: Betas, Packmates and co-Alphas, in the order they joined (not the leader). */
    public List<UUID> betasOf(UUID alpha) {
        List<UUID> out = new ArrayList<>();
        alphaOf.forEach((beta, a) -> {
            if (a.equals(alpha)) {
                out.add(beta);
            }
        });
        return out;
    }

    /**
     * @return false when the beta already has an Alpha (a pack is never switched silently), is the Alpha itself, or {@code alpha}
     *         is not a leader. A leader who joins another pack brings their own members along.
     */
    public boolean join(UUID alpha, UUID beta) {
        if (alpha.equals(beta) || alphaOf.containsKey(beta) || alphaOf.containsKey(alpha)) {
            return false;
        }
        long at = now();
        boolean wasLeader = !betasOf(beta).isEmpty();
        for (UUID own : betasOf(beta)) {
            alphaOf.put(own, alpha);
        }
        alphaOf.put(beta, alpha);
        joinedAt.put(beta, at);
        if (wasLeader) {
            // a leader who joins another pack brings their members and, with them, what their pack has discovered
            LEADERSHIP_MOVED.forEach(l -> l.accept(beta, alpha));
        }
        emissaryOf.remove(beta); // a leader who joins another pack no longer has an Emissary of their own
        allies.remove(beta);
        allies.values().forEach(s -> s.remove(beta));
        setDirty();
        return true;
    }

    /** {@code next} (a member) leads, {@code old} stays in the pack as a member; allies and the emissary go with the leadership. */
    public void promote(UUID old, UUID next) {
        long age = inPackSince(old).orElse(now()); // the old leader has been in this pack since it began, not since now
        alphaOf.remove(next);
        alphaOf.replaceAll((member, a) -> a.equals(old) ? next : a);
        alphaOf.put(old, next);
        joinedAt.put(old, age);
        joinedAt.remove(next);
        moveLeadership(old, next);
        setDirty();
    }

    private static final java.util.List<java.util.function.BiConsumer<UUID, UUID>> LEADERSHIP_MOVED = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.List<java.util.function.Consumer<UUID>> DISSOLVE_LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Told (old leader, new leader) whenever a pack changes hands, by a duel, an absence, a spark handover or a succession: whatever is kept per leader follows. */
    public static void onLeadershipMoved(java.util.function.BiConsumer<UUID, UUID> listener) {
        LEADERSHIP_MOVED.add(listener);
    }

    /** Told the leader of a pack that dissolved. */
    public static void onDissolved(java.util.function.Consumer<UUID> listener) {
        DISSOLVE_LISTENERS.add(listener);
    }

    private void moveLeadership(UUID old, UUID next) {
        LEADERSHIP_MOVED.forEach(l -> l.accept(old, next));
        UUID emissary = emissaryOf.remove(old);
        if (emissary != null && !emissaryOf.containsKey(next)) {
            emissaryOf.put(next, emissary);
        }
        Set<UUID> mine = allies.remove(old);
        if (mine != null) {
            mine.remove(next);
            allies.computeIfAbsent(next, k -> new HashSet<>()).addAll(mine);
        }
        allies.values().forEach(s -> {
            if (s.remove(old)) {
                s.add(next);
            }
        });
        allies.values().removeIf(Set::isEmpty);
    }

    /** The leader removes a member (online or not): a werewolf Beta becomes an Omega at their next login. @return false if they were no member */
    public boolean kick(UUID member) {
        return kick(member, false);
    }

    /** {@link #kick(UUID)}; {@code voluntary} is a member who left the party of the pack: they chose it, so the kill grace holds. */
    public boolean kick(UUID member, boolean voluntary) {
        if (voluntary) {
            rememberLeft(member);
        }
        UUID was = alphaOf.remove(member);
        if (was == null) {
            return false;
        }
        joinedAt.remove(member);
        emissaryOf.values().removeIf(member::equals);
        dissolved.add(member);
        setDirty();
        return true;
    }

    public Set<UUID> alliesOf(UUID leader) {
        return Set.copyOf(allies.getOrDefault(leader, Set.of()));
    }

    /** Two leaders ally (both ways). @return false for the same leader, someone who is not a leader or an alliance that exists */
    public boolean ally(UUID a, UUID b) {
        if (a.equals(b) || alphaOf.containsKey(a) || alphaOf.containsKey(b) || !allies.computeIfAbsent(a, k -> new HashSet<>()).add(b)) {
            allies.values().removeIf(Set::isEmpty);
            return false;
        }
        allies.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        setDirty();
        return true;
    }

    public boolean unally(UUID a, UUID b) {
        boolean was = allies.getOrDefault(a, new HashSet<>()).remove(b) | allies.getOrDefault(b, new HashSet<>()).remove(a);
        allies.values().removeIf(Set::isEmpty);
        if (was) {
            setDirty();
        }
        return was;
    }

    /** Whether {@code a} and {@code b} are in packs that are allied (not the same pack). */
    public boolean alliedPlayers(UUID a, UUID b) {
        UUID la = leaderOf(a);
        UUID lb = leaderOf(b);
        return !la.equals(lb) && allies.getOrDefault(la, Set.of()).contains(lb);
    }

    public void leave(UUID beta) {
        if (alphaOf.remove(beta) != null) {
            joinedAt.remove(beta);
            emissaryOf.values().removeIf(beta::equals);
            setDirty();
        }
    }

    /**
     * A member leaves by their own choice ({@code /pack leave}): like {@link #leave}, and the pack they left is remembered for the
     * grace window of what a kill does. Being kicked, or the pack dissolving, gets no grace: those are outsiders at once.
     */
    public void leaveVoluntarily(UUID member) {
        rememberLeft(member);
        leave(member);
    }

    /**
     * Notes that {@code member} is leaving their pack by their own choice, so the grace holds. It has to run BEFORE anything that
     * can take them out of the index (a Beta's state change does, through {@code follow}): afterwards there is no pack to remember.
     */
    public void rememberLeft(UUID member) {
        UUID was = alphaOf.get(member);
        if (was != null) {
            leftAt.put(member, new Left(was, now()));
        }
    }

    /** Whether {@code p} left a pack by choice within the grace window (nothing is changed by asking). */
    public boolean leftRecently(UUID p, int graceMinutes) {
        Left l = leftAt.get(p);
        return graceMinutes > 0 && l != null && now() - l.at < graceMinutes * 60_000L;
    }

    /** Whether a leave is on record for {@code p} (any age): for the tests of the pruning. */
    public boolean hasLeft(UUID p) {
        return leftAt.containsKey(p);
    }

    /** Forgets the leaves older than a day: they could never count for anything again. Called from the tick, never from a query. */
    public void pruneLeft() {
        leftAt.values().removeIf(l -> now() - l.at >= PackAbsence.DAY_MILLIS);
    }

    /**
     * The new Alpha takes every member of the old one (and stops being a member); the old Alpha's Emissary and allies go with the
     * leadership (the Emissary only if the new Alpha has none), and the old Alpha is remembered as having lost the spark.
     */
    public void transfer(UUID oldAlpha, UUID newAlpha) {
        alphaOf.remove(newAlpha);
        alphaOf.replaceAll((beta, a) -> a.equals(oldAlpha) ? newAlpha : a);
        alphaOf.remove(oldAlpha);
        joinedAt.remove(newAlpha);
        moveLeadership(oldAlpha, newAlpha);
        // somebody who left the old pack a moment ago left the pack that is now the new Alpha's
        leftAt.replaceAll((id, l) -> l.leader.equals(oldAlpha) ? new Left(newAlpha, l.at) : l);
        formerAlphas.add(oldAlpha);
        formerAlphas.remove(newAlpha);
        setDirty();
    }

    /** Nobody can take the pack over (no Beta is around): its Betas become Omegas when they next log in. */
    public void dissolve(UUID alpha) {
        for (UUID beta : betasOf(alpha)) {
            alphaOf.remove(beta);
            joinedAt.remove(beta);
            dissolved.add(beta);
        }
        DISSOLVE_LISTENERS.forEach(l -> l.accept(alpha));
        emissaryOf.remove(alpha);
        allies.remove(alpha);
        allies.values().forEach(s -> s.remove(alpha));
        allies.values().removeIf(Set::isEmpty);
        formerAlphas.add(alpha);
        setDirty();
    }

    /** True once, for a Beta whose pack dissolved while they were away. */
    public boolean takeDissolved(UUID beta) {
        boolean was = dissolved.remove(beta);
        if (was) {
            setDirty();
        }
        return was;
    }

    public boolean isFormerAlpha(UUID id) {
        return formerAlphas.contains(id);
    }

    public void markFormerAlpha(UUID id) {
        if (formerAlphas.add(id)) {
            setDirty();
        }
    }

    public void clearFormerAlpha(UUID id) {
        if (formerAlphas.remove(id)) {
            setDirty();
        }
    }

    public Optional<UUID> emissaryOf(UUID alpha) {
        return Optional.ofNullable(emissaryOf.get(alpha));
    }

    /** The Alpha who named {@code emissary}, if anyone did. */
    public Optional<UUID> emissaryAlpha(UUID emissary) {
        return emissaryOf.entrySet().stream().filter(e -> e.getValue().equals(emissary)).map(Map.Entry::getKey).findFirst();
    }

    public void setEmissary(UUID alpha, @Nullable UUID emissary) {
        put(emissaryOf, alpha, emissary);
    }

    public Optional<UUID> anchorOf(UUID member) {
        return Optional.ofNullable(anchorOf.get(member));
    }

    public void setAnchor(UUID member, @Nullable UUID anchor) {
        put(anchorOf, member, anchor);
    }

    private void put(Map<UUID, UUID> map, UUID key, @Nullable UUID value) {
        if (value == null ? map.remove(key) != null : !value.equals(map.put(key, value))) {
            setDirty();
        }
    }

    public static PackIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        PackIndex i = new PackIndex();
        read(tag, MEMBERS, i.alphaOf);
        read(tag, EMISSARIES, i.emissaryOf);
        read(tag, ANCHORS, i.anchorOf);
        readIds(tag, FORMER, i.formerAlphas);
        readIds(tag, DISSOLVED, i.dissolved);
        readIds(tag, HYBRID_LIKE, i.hybridLike);
        read(tag, MIRRORS, i.mirrors);
        read(tag, PARTIES, i.parties);
        readIds(tag, ENDED, i.ended);
        for (Tag t : tag.getList(NOTICES, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.notices.computeIfAbsent(NbtUtils.loadUUID(e.get(KEY)), k -> new ArrayList<>()).add(new String[] {e.getString("reason"), e.getString("arg")});
            } catch (RuntimeException bad) {
                skipped(NOTICES, bad);
            }
        }
        for (Tag t : tag.getList(SPARKS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.sparks.put(NbtUtils.loadUUID(e.get(KEY)), Math.max(1, e.getInt(VALUE)));
            } catch (RuntimeException bad) {
                skipped(SPARKS, bad);
            }
        }
        for (Tag t : tag.getList(LAST_SEEN, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.lastSeen.put(NbtUtils.loadUUID(e.get(KEY)), e.getLong(VALUE));
            } catch (RuntimeException bad) {
                skipped(LAST_SEEN, bad);
            }
        }
        for (Tag t : tag.getList(JOINED_AT, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.joinedAt.put(NbtUtils.loadUUID(e.get(KEY)), e.getLong(VALUE));
            } catch (RuntimeException bad) {
                skipped(JOINED_AT, bad);
            }
        }
        // members of a pack from before joins were recorded start counting now (days since this version), and the stamp is saved so it does not move
        long stamped = now();
        boolean backfilled = false;
        for (UUID member : i.alphaOf.keySet()) {
            backfilled |= i.joinedAt.putIfAbsent(member, stamped) == null;
        }
        if (backfilled) {
            i.setDirty();
        }
        i.aliveAt = tag.contains(ALIVE_AT, Tag.TAG_LONG) ? Math.max(0, tag.getLong(ALIVE_AT)) : 0;
        for (Tag t : tag.getList(STRENGTHS, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                i.strengths.put(NbtUtils.loadUUID(e.get(KEY)), e.getInt(VALUE));
            } catch (RuntimeException bad) {
                skipped(STRENGTHS, bad);
            }
        }
        for (Tag t : tag.getList(ALLIES, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                UUID a = NbtUtils.loadUUID(e.get(KEY));
                UUID b = NbtUtils.loadUUID(e.get(VALUE));
                i.allies.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                i.allies.computeIfAbsent(b, k -> new HashSet<>()).add(a);
            } catch (RuntimeException bad) {
                skipped(ALLIES, bad);
            }
        }
        return i;
    }

    /** One bad entry (a damaged tag) costs that entry, not the whole index. */
    private static void skipped(String list, RuntimeException bad) {
        net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.LOGGER.warn("The pack index skipped a damaged entry of '{}': {}", list, bad.toString());
    }

    private static void readIds(CompoundTag tag, String name, Set<UUID> into) {
        for (Tag t : tag.getList(name, Tag.TAG_INT_ARRAY)) {
            try {
                into.add(NbtUtils.loadUUID(t));
            } catch (RuntimeException bad) {
                skipped(name, bad);
            }
        }
    }

    private static void writeIds(CompoundTag tag, String name, Set<UUID> from) {
        ListTag list = new ListTag();
        from.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put(name, list);
    }

    private static void read(CompoundTag tag, String name, Map<UUID, UUID> into) {
        for (Tag t : tag.getList(name, Tag.TAG_COMPOUND)) {
            try {
                CompoundTag e = (CompoundTag) t;
                into.put(NbtUtils.loadUUID(e.get(KEY)), NbtUtils.loadUUID(e.get(VALUE)));
            } catch (RuntimeException bad) {
                skipped(name, bad);
            }
        }
    }

    private static void write(CompoundTag tag, String name, Map<UUID, UUID> from) {
        ListTag list = new ListTag();
        from.forEach((k, v) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(k));
            e.put(VALUE, NbtUtils.createUUID(v));
            list.add(e);
        });
        tag.put(name, list);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, MEMBERS, alphaOf);
        write(tag, EMISSARIES, emissaryOf);
        write(tag, ANCHORS, anchorOf);
        writeIds(tag, FORMER, formerAlphas);
        writeIds(tag, DISSOLVED, dissolved);
        writeIds(tag, HYBRID_LIKE, hybridLike);
        write(tag, MIRRORS, mirrors);
        write(tag, PARTIES, parties);
        writeIds(tag, ENDED, ended);
        ListTag notes = new ListTag();
        notices.forEach((id, list) -> list.forEach(n -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(id));
            e.putString("reason", n[0]);
            e.putString("arg", n[1]);
            notes.add(e);
        }));
        tag.put(NOTICES, notes);
        ListTag sp = new ListTag();
        sparks.forEach((id, n) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(id));
            e.putInt(VALUE, n);
            sp.add(e);
        });
        tag.put(SPARKS, sp);
        ListTag seen = new ListTag();
        lastSeen.forEach((id, t) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(id));
            e.putLong(VALUE, t);
            seen.add(e);
        });
        tag.put(LAST_SEEN, seen);
        ListTag joined = new ListTag();
        joinedAt.forEach((id, t) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(id));
            e.putLong(VALUE, t);
            joined.add(e);
        });
        tag.put(JOINED_AT, joined);
        tag.putLong(ALIVE_AT, aliveAt);
        ListTag strong = new ListTag();
        strengths.forEach((id, s) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(id));
            e.putInt(VALUE, s);
            strong.add(e);
        });
        tag.put(STRENGTHS, strong);
        ListTag pairs = new ListTag();
        allies.forEach((a, set) -> set.forEach(b -> {
            if (a.compareTo(b) < 0) {
                CompoundTag e = new CompoundTag();
                e.put(KEY, NbtUtils.createUUID(a));
                e.put(VALUE, NbtUtils.createUUID(b));
                pairs.add(e);
            }
        }));
        tag.put(ALLIES, pairs);
        return tag;
    }
}
