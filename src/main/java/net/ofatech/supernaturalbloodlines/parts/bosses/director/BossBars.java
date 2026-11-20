package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import org.jetbrains.annotations.VisibleForTesting;

/** One boss bar per boss, shown to the players within {@link #RANGE} blocks (the way {@code AlphaTrialBoss} does it). Gone with the entity. */
public final class BossBars {
    public static final double RANGE = 64.0;
    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();

    private BossBars() {
    }

    public static void register() {
        // a boss that leaves the level any way (unloaded, killed, discarded, another dimension) takes its bar with it
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (e.getEntity() instanceof BossNpc && !e.getEntity().level().isClientSide) {
                remove(e.getEntity().getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    public static void update(BossNpc boss) {
        if (boss.level() instanceof ServerLevel level) {
            update(boss, level.players());
        }
    }

    /** The same with the players named (fake players are not in the level's list). */
    @VisibleForTesting
    public static void update(BossNpc boss, java.util.List<ServerPlayer> players) {
        if (!(boss.level() instanceof ServerLevel) || !boss.isAlive()) {
            return;
        }
        if (!BossesConfig.barsEnabled()) {
            remove(boss.getUUID());
            return;
        }
        ServerBossEvent bar = BARS.computeIfAbsent(boss.getUUID(),
                id -> new ServerBossEvent(boss.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(Math.max(0f, Math.min(1f, boss.getHealth() / boss.getMaxHealth())));
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(boss) <= RANGE * RANGE) {
                bar.addPlayer(p);
            } else {
                bar.removePlayer(p);
            }
        }
    }

    public static void remove(UUID boss) {
        ServerBossEvent bar = BARS.remove(boss);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    public static void reset() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
    }

    @VisibleForTesting
    public static boolean has(UUID boss) {
        return BARS.containsKey(boss);
    }

    @VisibleForTesting
    public static int viewers(Entity boss) {
        ServerBossEvent bar = BARS.get(boss.getUUID());
        return bar == null ? 0 : bar.getPlayers().size();
    }
}
