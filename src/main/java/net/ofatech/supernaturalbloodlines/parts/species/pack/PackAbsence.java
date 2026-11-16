package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * A leader who stays away too long loses the lead (plan 14, owner): offline for {@code absent_leader_days} real days (7 by
 * default, 0 = off), the strongest second Alpha leads the pack the next time this runs (when any player logs in, and every
 * 10 seconds). The old leader keeps their spark and stays a second Alpha; they can challenge for the lead when they are back.
 *
 * <p>The successor must be here: online, or seen within the same window. If every other Alpha is away too nothing changes, so
 * two absent Alphas never pass the lead back and forth. A pack with an absent leader and no second Alpha keeps its leader.
 *
 * <p>Time is the wall clock the index kept: at each logout, and renewed every hour for every pack member who is online (a crash
 * leaves no logout behind). A server that was off is not an absence, and a restart does not erase one: the index also keeps the
 * last time it knew the server was running (at most a minute old, and at a clean stop), and at the start every recorded time moves
 * forward by exactly the downtime. A world that never recorded it is not shifted.
 */
public final class PackAbsence {
    public static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    /** An online pack member's stored time is renewed when it is older than this. */
    public static final long REFRESH_MILLIS = 60L * 60 * 1000;
    /** The "server is running" time is written at most this often. */
    public static final long ALIVE_EVERY_MILLIS = 60_000L;
    private static final int PERIOD = 200;
    private static final String KEY = "supernaturalbloodlines.pack.";

    private PackAbsence() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> onStarted(e.getServer(), PackIndex.now()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> PackIndex.get(e.getServer().overworld()).setAliveAt(PackIndex.now()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity().getServer() != null) {
                check(e.getEntity().getServer(), PackIndex.now());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % PERIOD == 0) {
                check(e.getServer(), PackIndex.now());
            }
        });
    }

    /** The server started: the time it was off is not an absence, so every recorded time moves forward by it (none when it was never recorded). */
    public static void onStarted(MinecraftServer server, long nowMillis) {
        PackIndex index = PackIndex.get(server.overworld());
        long was = index.aliveAt();
        if (was > 0 && nowMillis > was) {
            index.shiftLastSeen(nowMillis - was);
        }
        index.setAliveAt(nowMillis);
    }

    /** @return how many packs changed hands */
    public static int check(MinecraftServer server, long nowMillis) {
        int days = SpeciesConfig.absentLeaderDays();
        if (days <= 0) {
            return 0;
        }
        long limit = days * DAY_MILLIS;
        PackIndex index = PackIndex.get(server.overworld());
        // the server is running: say so (at most once a minute), so a crash costs the leader no more than that
        if (nowMillis - index.aliveAt() >= ALIVE_EVERY_MILLIS) {
            index.setAliveAt(nowMillis);
        }
        // a crash leaves no logout: whoever of a pack is online is seen now, renewed every hour
        for (UUID leader : List.copyOf(index.leaders())) {
            for (UUID id : index.pack(leader)) {
                if (SireCall.player(server, id) != null && index.lastSeen(id).map(t -> nowMillis - t > REFRESH_MILLIS).orElse(true)) {
                    index.setLastSeen(id, nowMillis);
                }
            }
        }
        int changed = 0;
        for (UUID leader : List.copyOf(index.leaders())) {
            Long seen = index.lastSeen(leader).orElse(null);
            if (SireCall.player(server, leader) != null) {
                continue;
            }
            if (seen == null) {
                index.setLastSeen(leader, nowMillis); // from now on, not from some day the index never saw
                continue;
            }
            if (nowMillis - seen < limit) {
                continue;
            }
            UUID next = SparkHandler.coAlphaHeir(index, leader, id -> here(server, index, id, nowMillis, limit));
            if (next != null) {
                depose(server, index, leader, next);
                changed++;
            }
        }
        return changed;
    }

    /** Online, or seen within the window. Somebody the index has no time for is not known to be here. */
    private static boolean here(MinecraftServer server, PackIndex index, UUID id, long nowMillis, long limit) {
        return SireCall.player(server, id) != null || index.lastSeen(id).map(t -> nowMillis - t < limit).orElse(false);
    }

    private static void depose(MinecraftServer server, PackIndex index, UUID old, UUID next) {
        List<UUID> members = index.betasOf(old);
        index.promote(old, next);
        for (UUID id : members) {
            if (!id.equals(next)) {
                SparkHandler.repoint(server, id, next);
            }
        }
        String name = PackAccess.nameOf(server, next);
        String shown = name != null ? name : next.toString().substring(0, 8);
        for (UUID id : members) {
            if (id.equals(next)) {
                PackMembers.tell(server, id, KEY + "absent_lead_now", "");
            } else {
                PackMembers.tell(server, id, KEY + "absent_leader", shown);
            }
        }
        PackMembers.tell(server, old, KEY + "absent_you", shown);
        PartyLink.leadershipChanged(server, next);
    }
}