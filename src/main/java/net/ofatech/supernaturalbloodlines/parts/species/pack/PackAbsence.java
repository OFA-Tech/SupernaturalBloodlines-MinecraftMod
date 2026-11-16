package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * A leader who stays away too long loses the lead (plan 14, owner): offline for {@code absent_leader_days} real days (7 by
 * default, 0 = off), the strongest second Alpha, online or not, leads the pack the next time this runs (when any player logs in,
 * and every 10 seconds). The old leader keeps their spark and stays a second Alpha; they can challenge for the lead when they
 * are back. The time a leader left is the index's own {@code lastSeen}, kept with the packs; a leader with no time on record
 * is not deposed, the time is only started.
 */
public final class PackAbsence {
    public static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final int PERIOD = 200;
    private static final String KEY = "supernaturalbloodlines.pack.";

    private PackAbsence() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity().getServer() != null) {
                check(e.getEntity().getServer(), System.currentTimeMillis());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % PERIOD == 0) {
                check(e.getServer(), System.currentTimeMillis());
            }
        });
    }

    /** @return how many packs changed hands */
    public static int check(MinecraftServer server, long nowMillis) {
        int days = SpeciesConfig.absentLeaderDays();
        if (days <= 0) {
            return 0;
        }
        PackIndex index = PackIndex.get(server.overworld());
        int changed = 0;
        for (UUID leader : List.copyOf(index.leaders())) {
            if (SireCall.player(server, leader) != null) {
                continue;
            }
            Long seen = index.lastSeen(leader).orElse(null);
            if (seen == null) {
                index.setLastSeen(leader, nowMillis); // from now on, not from some day the index never saw
                continue;
            }
            if (nowMillis - seen < days * DAY_MILLIS) {
                continue;
            }
            UUID next = SparkHandler.coAlphaHeir(index, leader);
            if (next != null) {
                depose(server, index, leader, next);
                changed++;
            }
        }
        return changed;
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
            PackMembers.tell(server, id, KEY + "absent_leader", shown);
        }
        PackMembers.tell(server, old, KEY + "absent_you", shown);
        PartyLink.leadershipChanged(server, next);
    }
}