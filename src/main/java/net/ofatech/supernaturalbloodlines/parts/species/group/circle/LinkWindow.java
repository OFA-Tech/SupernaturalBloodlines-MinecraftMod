package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteManager;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenText;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * The live link (R256): from the moment a linked rite has started, the anchor's death hurts the members who paid, for {@code link_window_seconds} or while
 * the rite that started is still the one running at the chalk. In memory: a restart owes nothing. A rite still running is stopped through a queue drained
 * in {@code ServerTickEvent.Post}, never inside the death event or inside {@code RiteManager.tick}.
 */
public final class LinkWindow {
    /** The members are those who paid; {@code rite} is the rite running at the chalk right after it started, null for an instant one. */
    public record Live(UUID anchor, List<UUID> members, ResourceKey<Level> dimension, BlockPos chalk, long startedMillis, WeakReference<Rite> rite) {
    }

    private static final Map<UUID, Live> LIVE = new ConcurrentHashMap<>();
    private static final Queue<Rite> STOPS = new ConcurrentLinkedQueue<>();
    private static boolean registered;

    private LinkWindow() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, LinkWindow::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> drain());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LIVE.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> {
            LIVE.clear();
            STOPS.clear();
        });
    }

    /** Called when a linked rite has started (the hook's {@code commit}): the members who paid are linked to the anchor. */
    public static void open(ServerLevel level, CircleStage.Staged staged) {
        List<UUID> members = new ArrayList<>(staged.result().shares().keySet());
        Rite running = RiteManager.getRiteAt(level, staged.chalk());
        LIVE.put(staged.anchor(), new Live(staged.anchor(), members, staged.dimension(), staged.chalk(), PackIndex.now(), new WeakReference<>(running)));
    }

    /** A member who left the coven, was kicked or shunned owes nothing more: they leave every live link; the link of an anchor who left is over. */
    public static void drop(UUID member) {
        LIVE.remove(member);
        for (Live live : LIVE.values()) {
            live.members().remove(member);
        }
    }

    @Nullable
    public static Live liveOf(UUID anchor) {
        return LIVE.get(anchor);
    }

    public static int queuedStops() {
        return STOPS.size();
    }

    /** Test clean-up for these players only. */
    public static void forget(UUID... players) {
        for (UUID id : players) {
            LIVE.remove(id);
        }
    }

    private static boolean stillOpen(MinecraftServer server, Live live) {
        if (PackIndex.now() - live.startedMillis() <= GroupsConfig.linkWindowSeconds() * 1000L) {
            return true;
        }
        Rite rite = live.rite().get();
        ServerLevel level = server.getLevel(live.dimension());
        return rite != null && level != null && RiteManager.getRiteAt(level, live.chalk()) == rite;
    }

    @Nullable
    private static ServerPlayer find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer p) {
                return p;
            }
        }
        return null;
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer dead) || dead.level().isClientSide) {
            return;
        }
        MinecraftServer server = dead.server;
        UUID id = dead.getUUID();
        for (Live other : LIVE.values()) {
            other.members().remove(id);
        }
        Live live = LIVE.remove(id);
        if (live == null || !stillOpen(server, live)) {
            return;
        }
        long now = PackIndex.now();
        GroupIndex groups = GroupIndex.get(dead.serverLevel());
        UUID coven = CircleLink.covenOf(dead);
        for (UUID memberId : List.copyOf(live.members())) {
            ServerPlayer member = memberId.equals(id) ? null : find(server, memberId);
            // someone who left, was kicked or shunned since the rite started is no longer in the coven: the link is no longer theirs
            if (member == null || !member.isAlive() || coven == null || !coven.equals(CircleLink.covenOf(member))) {
                continue;
            }
            if (GroupsConfig.linkDeathLethal()) {
                member.hurt(member.damageSources().genericKill(), Float.MAX_VALUE);
            } else {
                member.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 60 * 20, 1));
                member.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 60 * 20, 0));
                Pain.hurt(member, member.getMaxHealth() * GroupsConfig.linkDeathDamagePercent() / 100.0f);
            }
            groups.setChannel(memberId, 0, now, CircleCost.capacityOf(member));
            if (GroupsConfig.linkShakenMinutes() > 0) {
                CircleLink.shake(memberId, now + GroupsConfig.linkShakenMinutes() * 60_000L);
            }
            member.sendSystemMessage(CovenText.of(GroupsConfig.linkDeathLethal() ? "link_death_lethal" : "link_death_hurt"));
        }
        Rite rite = live.rite().get();
        ServerLevel level = server.getLevel(live.dimension());
        if (rite != null && level != null && RiteManager.getRiteAt(level, live.chalk()) == rite) {
            STOPS.add(rite);
        }
    }

    private static void drain() {
        Rite rite;
        while ((rite = STOPS.poll()) != null) {
            if (RiteManager.getRiteAt(rite.getLevel(), rite.getPos()) == rite) {
                rite.stop();
                RiteManager.removeRite(rite.getLevel(), rite);
            }
        }
    }
}