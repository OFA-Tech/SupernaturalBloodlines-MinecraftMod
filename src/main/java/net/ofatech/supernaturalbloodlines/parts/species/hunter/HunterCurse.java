package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/**
 * Hunter's Curse (R83): prey that killed a marked hunter hallucinates, one random {@link HallucinationPayload} every
 * 600 +- 200 ticks, until a potential completes the Five. The flag is the saved {@code hunterCurse}, so a second curse
 * changes nothing. The timers are in memory only: dropped on logout, started again on login.
 */
public final class HunterCurse {
    static final int INTERVAL = 600;
    static final int JITTER = 200;

    // player -> server tick of the next hallucination; server thread only
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private HunterCurse() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer(), e.getServer().getTickCount()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogout(p);
            }
        });
    }

    /** A prey killer of a marked hunter: the rules decide whether they are prey. */
    public static void curse(ServerPlayer killer) {
        BloodlineAccess.applyEvent(killer, BloodlineEvent.KILLED_MARKED_HUNTER, SpeciesContext.of(killer, null));
        if (BloodlineAccess.get(killer).hunterCurse()) {
            FiveRoster.get(killer.serverLevel()).setCursed(killer.getUUID(), true);
            schedule(killer); // putIfAbsent: the second curse keeps the first timer
        }
    }

    /** The potential's first kill: frees everyone online now, and marks everyone else to be freed at login. */
    public static void liftAll(MinecraftServer server) {
        FiveRoster roster = FiveRoster.get(server.overworld());
        for (ServerPlayer p : HunterEvents.online(server)) {
            if (BloodlineAccess.get(p).hunterCurse() || roster.cursed().contains(p.getUUID())) {
                lift(p, roster);
            }
        }
        for (UUID offline : Set.copyOf(roster.cursed())) {
            roster.setLiftedCurse(offline, true);
            roster.setCursed(offline, false);
        }
    }

    private static void lift(ServerPlayer p, FiveRoster roster) {
        BloodlineAccess.applyEvent(p, BloodlineEvent.HUNTER_CURSE_LIFTED, SpeciesContext.of(p, null));
        roster.setCursed(p.getUUID(), false);
        roster.setLiftedCurse(p.getUUID(), false);
        NEXT.remove(p.getUUID());
    }

    public static void onLogin(ServerPlayer p) {
        FiveRoster roster = FiveRoster.get(p.serverLevel());
        if (roster.liftedCurses().contains(p.getUUID())) {
            lift(p, roster);
        }
        if (BloodlineAccess.get(p).hunterCurse()) {
            schedule(p);
        }
    }

    public static void onLogout(ServerPlayer p) {
        NEXT.remove(p.getUUID());
    }

    /** The server tick of the next hallucination, if the player is cursed and online. */
    public static OptionalLong nextDue(ServerPlayer p) {
        Long due = NEXT.get(p.getUUID());
        return due == null ? OptionalLong.empty() : OptionalLong.of(due);
    }

    private static void schedule(ServerPlayer p) {
        NEXT.computeIfAbsent(p.getUUID(), id -> p.server.getTickCount() + interval(p.getRandom()));
    }

    private static long interval(RandomSource random) {
        return INTERVAL - JITTER + random.nextInt(2 * JITTER + 1);
    }

    /** @param now the server tick count */
    public static void tick(MinecraftServer server, long now) {
        if (NEXT.isEmpty()) {
            return;
        }
        List<ServerPlayer> online = HunterEvents.online(server);
        Iterator<Map.Entry<UUID, Long>> it = NEXT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now < entry.getValue()) {
                continue;
            }
            ServerPlayer p = online.stream().filter(o -> o.getUUID().equals(entry.getKey())).findFirst().orElse(null);
            if (p == null || !BloodlineAccess.get(p).hunterCurse()) {
                it.remove(); // gone, or freed some other way (the cure)
                continue;
            }
            HallucinationPayload.send(p, HallucinationPayload.Kind.random(p.getRandom()));
            entry.setValue(now + interval(p.getRandom()));
        }
    }
}
