package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupText;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenText;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/**
 * The open circles, in memory (a restart owes nothing): one per {@code (dimension, chalk)}, owned by its anchor, with the members in the order they joined
 * (the anchor first, at most 13). A call is a one-minute, single-use token like a pack invitation, kept in a second {@link CallRequests}.
 */
public final class CircleLink {
    public static final int MAX_MEMBERS = 13;
    public static final double CALL_RANGE = 32;
    public static final long IDLE_MILLIS = 5 * 60_000L;

    public record Key(ResourceKey<Level> dimension, BlockPos chalk) {
    }

    public static final class Session {
        private final UUID anchor;
        private final Key key;
        private final List<UUID> members = new ArrayList<>();
        private long lastActive;

        private Session(UUID anchor, Key key, long now) {
            this.anchor = anchor;
            this.key = key;
            this.lastActive = now;
            members.add(anchor);
        }

        public UUID anchor() {
            return anchor;
        }

        public Key key() {
            return key;
        }

        /** In join order, the anchor first. */
        public List<UUID> members() {
            return List.copyOf(members);
        }
    }

    private static final CallRequests CALLS = new CallRequests();
    private static final Map<Key, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> SHAKEN = new HashMap<>();

    private CircleLink() {
    }

    public static CallRequests calls() {
        return CALLS;
    }

    /** Test clean-up and server stop: no circle, no call, nobody shaken. */
    public static void reset() {
        SESSIONS.clear();
        SHAKEN.clear();
        CALLS.clear();
    }

    @Nullable
    public static UUID covenOf(ServerPlayer p) {
        PackIndex packs = PackIndex.get(p.serverLevel());
        if (!packs.inPack(p.getUUID())) {
            return null;
        }
        UUID leader = packs.leaderOf(p.getUUID());
        return Groups.typeOfGroup(p.serverLevel(), leader) == GroupType.COVEN ? leader : null;
    }

    public static Component notACoven(ServerPlayer p) {
        if (!GroupsConfig.covensEnabled()) {
            return CovenText.error("covens_off");
        }
        PackIndex packs = PackIndex.get(p.serverLevel());
        return GroupText.error(Groups.typeOf(p), packs.inPack(p.getUUID()) ? "wrong_type" : "no_group");
    }

    public static void shake(UUID member, long untilMillis) {
        SHAKEN.put(member, untilMillis);
    }

    public static boolean shaken(UUID member) {
        Long until = SHAKEN.get(member);
        if (until != null && until <= PackIndex.now()) {
            SHAKEN.remove(member);
            return false;
        }
        return until != null;
    }

    /** The session {@code member} is in, anchor or not. */
    @Nullable
    public static Session of(UUID member) {
        for (Session s : SESSIONS.values()) {
            if (s.members.contains(member)) {
                return s;
            }
        }
        return null;
    }

    /** The circle at this chalk, only for its own anchor: another caster at the same chalk, or the anchor at another chalk, has none. */
    @Nullable
    public static Session current(ServerLevel level, BlockPos chalk, @Nullable UUID caster) {
        Session s = SESSIONS.get(new Key(level.dimension(), chalk));
        if (s == null || caster == null || !s.anchor.equals(caster)) {
            return null;
        }
        return prune(level.getServer(), s) ? s : null;
    }

    /** The members of the circle open at the chalk, whoever its anchor is. */
    public static int sizeAt(ServerLevel level, BlockPos chalk) {
        Session s = SESSIONS.get(new Key(level.dimension(), chalk));
        return s == null ? 0 : s.members.size();
    }

    public static int sessions() {
        return SESSIONS.size();
    }

    /** A button pressed: the anchor opens a circle at the chalk they stand at and calls the coven's members within 32 blocks. */
    @Nullable
    public static Component open(ServerPlayer anchor) {
        UUID coven = GroupsConfig.covensEnabled() ? covenOf(anchor) : null;
        if (coven == null) {
            return notACoven(anchor);
        }
        if (!GroupsConfig.linkEnabled()) {
            return CovenText.error("link_off");
        }
        if (of(anchor.getUUID()) != null) {
            return CovenText.error("in_circle");
        }
        if (shaken(anchor.getUUID())) {
            return CovenText.error("shaken");
        }
        ServerLevel level = anchor.serverLevel();
        var chalk = CircleSites.chalkNear(level, anchor.blockPosition());
        if (chalk == null) {
            return CovenText.error("no_chalk");
        }
        Key key = new Key(level.dimension(), chalk.getBlockPos().immutable());
        if (SESSIONS.containsKey(key) && prune(level.getServer(), SESSIONS.get(key))) {
            return CovenText.error("circle_taken");
        }
        Session s = new Session(anchor.getUUID(), key, PackIndex.now());
        SESSIONS.put(key, s);
        long now = level.getGameTime();
        PackIndex packs = PackIndex.get(level);
        List<UUID> everyone = new ArrayList<>(packs.betasOf(coven));
        everyone.add(coven);
        for (UUID id : everyone) {
            ServerPlayer other = SireCall.player(level.getServer(), id);
            if (other == null || id.equals(anchor.getUUID()) || other.level() != level || of(id) != null || shaken(id)
                    || other.distanceToSqr(anchor) > CALL_RANGE * CALL_RANGE) {
                continue;
            }
            String token = CALLS.open(anchor.getUUID(), id, now);
            other.sendSystemMessage(callText(anchor.getGameProfile().getName(), token));
            HubRequests.changed(other);
        }
        anchor.sendSystemMessage(CovenText.of("link_opened"));
        HubRequests.changed(anchor);
        return null;
    }

    private static Component callText(String anchorName, String token) {
        Component join = CovenText.of("link_join_btn").copy().withStyle(st -> st.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/group link join " + token)));
        return CovenText.of("link_call", anchorName).copy().append(" ").append(join);
    }

    @Nullable
    public static Component close(ServerPlayer anchor) {
        Session s = of(anchor.getUUID());
        if (s == null || !s.anchor.equals(anchor.getUUID())) {
            return CovenText.error("no_circle");
        }
        closeSession(anchor.server, s, true);
        return null;
    }

    /** {@code token} is what the call card sent; null answers the newest call. */
    @Nullable
    public static Component join(ServerPlayer p, @Nullable String token) {
        UUID coven = GroupsConfig.covensEnabled() ? covenOf(p) : null;
        if (coven == null) {
            return notACoven(p);
        }
        long now = p.serverLevel().getGameTime();
        String use = token != null ? token : CALLS.latestFor(p.getUUID(), now).orElse(null);
        CallRequests.Request r = use == null ? null : CALLS.peek(use, p.getUUID(), now).orElse(null);
        if (r == null) {
            HubRequests.changed(p);
            return CovenText.error("call_expired");
        }
        Session s = of(r.caller());
        ServerPlayer anchor = SireCall.player(p.server, r.caller());
        if (s == null || !s.anchor.equals(r.caller()) || anchor == null || !prune(p.server, s)) {
            CALLS.take(use, p.getUUID(), now);
            HubRequests.changed(p);
            return CovenText.error("circle_gone");
        }
        if (!coven.equals(covenOf(anchor))) {
            return CovenText.error("circle_gone");
        }
        if (of(p.getUUID()) != null) {
            return CovenText.error("in_circle");
        }
        if (shaken(p.getUUID())) {
            return CovenText.error("shaken");
        }
        if (s.members.size() >= MAX_MEMBERS) {
            return CovenText.error("circle_full");
        }
        if (p.level().dimension() != s.key.dimension() || p.blockPosition().distSqr(s.key.chalk()) > CALL_RANGE * CALL_RANGE) {
            return CovenText.error("too_far");
        }
        if (CALLS.take(use, p.getUUID(), now).isEmpty()) {
            HubRequests.changed(p);
            return CovenText.error("call_expired");
        }
        s.members.add(p.getUUID());
        s.lastActive = PackIndex.now();
        p.sendSystemMessage(CovenText.of("link_joined"));
        anchor.sendSystemMessage(CovenText.of("link_joined_anchor", p.getGameProfile().getName()));
        changed(p.server, s);
        return null;
    }

    /** The anchor pressing Leave closes the circle; anyone else only leaves it. */
    @Nullable
    public static Component leave(ServerPlayer p) {
        Session s = of(p.getUUID());
        if (s == null) {
            return CovenText.error("not_in_circle");
        }
        if (s.anchor.equals(p.getUUID())) {
            closeSession(p.server, s, true);
        } else {
            dropMember(p.server, p.getUUID(), true);
        }
        return null;
    }

    /** One member goes (left the coven, was kicked or shunned, logged out); the circle stays. The anchor going closes it. */
    public static void dropMember(MinecraftServer server, UUID member, boolean tell) {
        LinkWindow.drop(member);
        Session s = of(member);
        if (s == null) {
            return;
        }
        if (s.anchor.equals(member)) {
            closeSession(server, s, tell);
            return;
        }
        s.members.remove(member);
        ServerPlayer gone = SireCall.player(server, member);
        if (gone != null) {
            if (tell) {
                gone.sendSystemMessage(CovenText.of("link_left"));
            }
            HubRequests.changed(gone);
        }
        ServerPlayer anchor = SireCall.player(server, s.anchor);
        if (tell && anchor != null) {
            anchor.sendSystemMessage(CovenText.of("link_member_left", gone != null ? gone.getGameProfile().getName() : "?"));
        }
        changed(server, s);
    }

    /** The anchor left the dimension (or the world): their circle closes. A member who moves is only dropped when a rite is planned. */
    public static void anchorLeft(MinecraftServer server, UUID player) {
        Session s = of(player);
        if (s != null && s.anchor.equals(player)) {
            closeSession(server, s, true);
        }
    }

    /** Test clean-up for these players only (the others' circles are not touched): their circles, calls and shaking are gone, silently. */
    public static void forget(UUID... players) {
        for (UUID id : players) {
            Session s = of(id);
            if (s != null) {
                SESSIONS.remove(s.key);
            }
            CALLS.forget(id);
            CALLS.forgetCalledBy(id);
        }
    }

    /** Every circle owned or joined by a member of the group led by {@code leader} (a type switch or a dissolved group). */
    public static void closeGroup(MinecraftServer server, UUID leader) {
        PackIndex packs = PackIndex.get(server.overworld());
        for (Session s : List.copyOf(SESSIONS.values())) {
            if (s.anchor.equals(leader) || leader.equals(packs.leaderOf(s.anchor))) {
                closeSession(server, s, true);
            }
        }
    }

    private static void closeSession(MinecraftServer server, Session s, boolean tell) {
        SESSIONS.remove(s.key);
        for (UUID id : s.members) {
            ServerPlayer p = SireCall.player(server, id);
            if (p != null) {
                if (tell) {
                    p.sendSystemMessage(CovenText.of("link_closed"));
                }
                HubRequests.changed(p);
            }
        }
        for (UUID asked : CALLS.forgetCalledBy(s.anchor)) {
            ServerPlayer p = SireCall.player(server, asked);
            if (p != null) {
                HubRequests.changed(p);
            }
        }
    }

    private static void changed(MinecraftServer server, Session s) {
        for (UUID id : s.members) {
            ServerPlayer p = SireCall.player(server, id);
            if (p != null) {
                HubRequests.changed(p);
            }
        }
    }

    /** Keeps the clock of a circle that is being used. */
    public static void touch(Session s) {
        s.lastActive = PackIndex.now();
    }

    /** Drops the members who are no longer in the anchor's coven; false (and the circle closed) when the anchor is not in one any more. */
    private static boolean prune(MinecraftServer server, Session s) {
        ServerPlayer anchor = SireCall.player(server, s.anchor);
        UUID coven = anchor == null ? null : covenOf(anchor);
        if (coven == null) {
            closeSession(server, s, false);
            return false;
        }
        PackIndex packs = PackIndex.get(server.overworld());
        for (Iterator<UUID> it = s.members.iterator(); it.hasNext();) {
            UUID id = it.next();
            if (!id.equals(s.anchor) && !coven.equals(packs.inPack(id) ? packs.leaderOf(id) : null)) {
                it.remove();
            }
        }
        return true;
    }

    /** Closes the circles that stood idle, and the ones whose anchor is gone; runs every 200 ticks. */
    public static void sweep(MinecraftServer server) {
        long now = PackIndex.now();
        for (Session s : List.copyOf(SESSIONS.values())) {
            if (now - s.lastActive > IDLE_MILLIS) {
                closeSession(server, s, true);
            } else {
                prune(server, s);
            }
        }
    }
}