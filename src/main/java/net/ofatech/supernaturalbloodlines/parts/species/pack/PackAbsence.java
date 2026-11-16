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
 * <p>Time is the wall clock the index kept at each logout. A server that was off is not an absence: at every start every
 * recorded time begins again, and an online leader's time is refreshed (a crash leaves no logout behind).
 */
public final class PackAbsence {
    public static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    /** An online leader's stored time is renewed when it is older than this. */
    public static final long REFRESH_MILLIS = 60L * 60 * 1000;
    private static final int PERIOD = 200;
    private static final String KEY = "supernaturalbloodlines.pack.";

    private PackAbsence() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> PackIndex.get(e.getServer().overworld()).resetLastSeen(PackIndex.now()));
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

    /** @return how many packs changed hands */
    public static int check(MinecraftServer server, long nowMillis) {
        int days = SpeciesConfig.absentLeaderDays();
        if (days <= 0) {
            return 0;
        }
        long limit = days * DAY_MILLIS;
        PackIndex index = PackIndex.get(server.overworld());
        int changed = 0;
        for (UUID leader : List.copyOf(index.leaders())) {
            Long seen = index.lastSeen(leader).orElse(null);
            if (SireCall.player(server, leader) != null) {
                if (seen == null || nowMillis - seen > REFRESH_MILLIS) {
                    index.setLastSeen(leader, nowMillis); // a crash leaves no logout: an online leader is seen now
                }
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