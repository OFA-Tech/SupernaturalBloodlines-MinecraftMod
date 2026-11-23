package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import org.jetbrains.annotations.VisibleForTesting;

/** One boss bar per boss, shown to the players within {@link #RANGE} blocks (the way {@code AlphaTrialBoss} does it). Gone with the entity. */
public final class BossBars {
    public static final double RANGE = 64.0;
    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    /** What each bar last told its viewers, so nothing is sent twice. */
    private static final Map<UUID, BossBarInfoPayload> SENT = new ConcurrentHashMap<>();

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
        boolean fallen = boss instanceof NpcOriginalBoss n && n.isDesiccated();
        bar.setName(title(boss, fallen));
        bar.setColor(fallen ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE);
        bar.setProgress(Math.max(0f, Math.min(1f, boss.getHealth() / boss.getMaxHealth())));
        BossBarInfoPayload info = info(boss, bar.getId(), fallen);
        boolean changed = !info.equals(SENT.put(boss.getUUID(), info));
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(boss) > RANGE * RANGE) {
                bar.removePlayer(p);
                continue;
            }
            boolean viewing = bar.getPlayers().contains(p);
            if (mustSend(viewing, changed)) {
                // the card's facts go first, so the client never draws a vanilla bar for a boss it knows nothing about
                PacketDistributor.sendToPlayer(p, info);
            }
            if (!viewing) {
                bar.addPlayer(p);
            }
        }
    }

    /** A viewer who is just coming into range always gets the facts; one who is already watching only when they changed. */
    static boolean mustSend(boolean alreadyViewing, boolean changed) {
        return !alreadyViewing || changed;
    }

    @VisibleForTesting
    public static BossBarInfoPayload sentOf(UUID boss) {
        return SENT.get(boss);
    }

    /** What a card shows that the vanilla packet cannot say: the boss, the chips, the health in numbers. */
    public static BossBarInfoPayload info(BossNpc boss, UUID bar, boolean fallen) {
        return BossBarInfoPayload.of(bar, boss.boss(), boss.barLook(), fallen, Math.round(boss.getHealth()), Math.round(boss.getMaxHealth()));
    }

    /** The bar's name: the boss's own, and "(desiccated)" while an Original lies open to white oak. */
    public static Component title(BossNpc boss, boolean fallen) {
        return fallen ? Component.translatable("supernaturalbloodlines.boss.bar.desiccated", boss.getDisplayName()) : boss.getDisplayName();
    }

    @VisibleForTesting
    public static ServerBossEvent barOf(UUID boss) {
        return BARS.get(boss);
    }

    public static void remove(UUID boss) {
        SENT.remove(boss);
        ServerBossEvent bar = BARS.remove(boss);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    public static void reset() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        SENT.clear();
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
