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
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> forgetPlayer(e.getEntity().getUUID()));
        // the player who damages a boss is told, so the boss row makes that boss the full card (the one fact the client cannot know by itself)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e) -> {
            if (e.getEntity() instanceof BossNpc boss && e.getSource().getEntity() instanceof ServerPlayer hitter) {
                noteHit(hitter, boss, boss.level().getGameTime());
            }
        });
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
        // a viewer who changed dimension or logged out is not in this level's player list, so the loop below would never take the card from them
        for (ServerPlayer gone : java.util.List.copyOf(bar.getPlayers())) {
            if (!stillViewing(gone.level() == boss.level(), gone.isRemoved())) {
                bar.removePlayer(gone);
            }
        }
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(boss) > RANGE * RANGE) {
                bar.removePlayer(p);
                continue;
            }
            boolean viewing = bar.getPlayers().contains(p);
            if (mustSend(viewing, changed)) {
                // the card's facts go first, so the client never draws a vanilla bar for a boss it knows nothing about
                BossNet.send(p, info);
            }
            if (!viewing) {
                bar.addPlayer(p);
            }
        }
    }

    /** When each player last was told of a hit on each boss (game time), so a flurry is one message per {@link HitThrottle#MIN_GAP} ticks. */
    private static final Map<UUID, Map<UUID, Long>> HIT_SENT = new ConcurrentHashMap<>();

    /** Tells {@code hitter} that they have just hit {@code boss}, unless they were told a moment ago, or the boss has no bar they can see, or they cannot receive it. */
    public static boolean noteHit(ServerPlayer hitter, BossNpc boss, long now) {
        ServerBossEvent bar = BARS.get(boss.getUUID());
        if (bar == null || !bar.getPlayers().contains(hitter)) {
            return false;
        }
        Map<UUID, Long> mine = HIT_SENT.computeIfAbsent(hitter.getUUID(), id -> new ConcurrentHashMap<>());
        if (!HitThrottle.allow(mine.getOrDefault(boss.getUUID(), HitThrottle.NEVER), now)) {
            return false;
        }
        if (!BossNet.send(hitter, new BossHitPayload(bar.getId()))) {
            return false;
        }
        mine.put(boss.getUUID(), now);
        return true;
    }

    /** A player who logs out takes what was kept about their hits with them (it would otherwise stay, one entry per player who ever hit a boss). */
    public static void forgetPlayer(UUID player) {
        HIT_SENT.remove(player);
    }

    /** How many bosses this player is being throttled for: what is kept of them. */
    @VisibleForTesting
    public static int hitsKept(UUID player) {
        Map<UUID, Long> mine = HIT_SENT.get(player);
        return mine == null ? 0 : mine.size();
    }

    /** Whether a player who is watching a bar still may: in the boss's dimension, and still in the game. */
    static boolean stillViewing(boolean sameLevel, boolean removed) {
        return sameLevel && !removed;
    }

    /** The health as a card writes it: rounded up, so a boss that is alive never shows 0. */
    public static int shownHealth(float health) {
        return Math.max(0, (int) Math.ceil(health));
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
        return BossBarInfoPayload.of(bar, boss.boss(), boss.barLook(), fallen, shownHealth(boss.getHealth()), shownHealth(boss.getMaxHealth()), boss.getId());
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
        HIT_SENT.values().forEach(m -> m.remove(boss));
        ServerBossEvent bar = BARS.remove(boss);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    public static void reset() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        SENT.clear();
        HIT_SENT.clear();
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
