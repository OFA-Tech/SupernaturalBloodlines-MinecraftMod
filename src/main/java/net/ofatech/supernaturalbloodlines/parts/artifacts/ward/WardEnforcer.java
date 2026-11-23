package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * What a standing ward does (R302, R303): every {@value #CHECK_TICKS} ticks an uninvited entity of the kinds it turns away that is inside its box
 * is pushed to the nearest face with Ashwarden's push, and after {@code ward_expel_seconds} inside is moved out (a closed room cannot be cleared by a push).
 * A door ward also keeps the door shut for them. The same one line for every cause; nothing is said about what the entity is, and the owner is never
 * told who was turned away (R325). Only wards of the entity's own level, read from the index, never from a chunk.
 */
public final class WardEnforcer {
    public static final String CANNOT_ENTER = "supernaturalbloodlines.ward.cannot_enter";
    public static final String TESTED = "supernaturalbloodlines.ward.tested";
    public static final int CHECK_TICKS = 5;
    private static final long TELL_EVERY = 40;
    private static final long ALERT_EVERY = 200;

    private static final Map<UUID, Integer> INSIDE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TOLD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ALERTED = new ConcurrentHashMap<>();
    private static final Map<String, Set<UUID>> TOLERATED = new ConcurrentHashMap<>();

    private WardEnforcer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof LivingEntity living && !living.level().isClientSide && living.tickCount % CHECK_TICKS == 0) {
                checkNow(living);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (e.getEntity() instanceof ServerPlayer player && e.getLevel() instanceof ServerLevel level && doorShut(level, e.getPos(), player)) {
                e.setCanceled(true);
                tell(player, level.getGameTime());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            INSIDE.remove(e.getEntity().getUUID());
            TOLD.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    @VisibleForTesting
    public static void reset() {
        INSIDE.clear();
        TOLD.clear();
        ALERTED.clear();
        TOLERATED.clear();
    }

    public static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    /** A ward that is standing for the rules: not fallen, the switch on, and for the door form its own switch on. */
    static boolean standing(WardTable.Ward ward) {
        return !ward.fallen() && ArtifactsConfig.wardEnabled() && (ward.kind() != WardRules.Kind.DOOR || ArtifactsConfig.wardDoorEnabled());
    }

    /** Whether {@code ward} turns this entity away. */
    static boolean blocks(WardTable.Ward ward, LivingEntity entity) {
        if (!(entity instanceof Player) && !ArtifactsConfig.wardAffectsMobs()) {
            return false;
        }
        boolean spectator = entity instanceof Player p && (p.isCreative() || p.isSpectator());
        boolean wolf = VampirismSupernatural.isWerewolf(entity) || Supernatural.isWerewolfLike(entity);
        UUID id = entity.getUUID();
        return WardRules.blocks(ArtifactsConfig.wardEnabled(), standing(ward), Supernatural.isVampiric(entity), wolf, ArtifactsConfig.wardBlocksWerewolves(),
                spectator, ward.owner().equals(id), ward.invited().contains(id));
    }

    /**
     * One check of one entity: the push and the timeout. The tick listener calls it, and the tests (a fake player does not tick).
     *
     * @return whether the entity was pushed
     */
    public static boolean checkNow(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        WardTable table = WardIndex.get(level).table();
        if (!TOLERATED.isEmpty()) {
            releaseOutside(table, entity);
        }
        List<WardTable.Ward> wards = table.standingIn(dimension(level));
        for (WardTable.Ward ward : wards) {
            if (!ward.box().contains(entity.getX(), entity.getY(), entity.getZ()) || !blocks(ward, entity) || tolerated(ward, entity)) {
                continue;
            }
            push(level, entity, ward);
            return true;
        }
        if (!INSIDE.isEmpty()) {
            INSIDE.remove(entity.getUUID());
        }
        return false;
    }

    /**
     * {@code ward_expel_on_revoke = false} (R308): somebody who was inside when their invitation was withdrawn may stay until they leave; the day they
     * come back in they are turned away like any other. In memory: a restart ends the grace.
     */
    static void tolerateInside(List<WardTable.Ward> wards, LivingEntity entity) {
        for (WardTable.Ward ward : wards) {
            if (ward.box().contains(entity.getX(), entity.getY(), entity.getZ())) {
                TOLERATED.computeIfAbsent(ward.id(), k -> ConcurrentHashMap.newKeySet()).add(entity.getUUID());
            }
        }
    }

    static void forgetWard(String id) {
        TOLERATED.remove(id);
    }

    private static boolean tolerated(WardTable.Ward ward, LivingEntity entity) {
        Set<UUID> inside = TOLERATED.get(ward.id());
        return inside != null && inside.contains(entity.getUUID());
    }

    private static void releaseOutside(WardTable table, LivingEntity entity) {
        TOLERATED.entrySet().removeIf(entry -> {
            if (entry.getValue().contains(entity.getUUID())) {
                boolean stillInside = table.get(entry.getKey()).filter(w -> w.box().contains(entity.getX(), entity.getY(), entity.getZ())).isPresent();
                if (!stillInside) {
                    entry.getValue().remove(entity.getUUID());
                }
            }
            return entry.getValue().isEmpty();
        });
    }

    private static void push(ServerLevel level, LivingEntity entity, WardTable.Ward ward) {
        WardRules.Box box = ward.box();
        WardRules.Dir way = WardRules.pushDirection(box, entity.getX(), entity.getZ());
        MountainAsh.pushBack(entity, new Vec3(way.x(), 0, way.z()));
        int ticks = INSIDE.merge(entity.getUUID(), CHECK_TICKS, Integer::sum);
        if (entity instanceof ServerPlayer player) {
            tell(player, level.getGameTime());
        }
        alert(level, ward);
        if (ticks >= ArtifactsConfig.wardExpelSeconds() * 20) {
            INSIDE.remove(entity.getUUID());
            moveOut(level, entity, box);
        }
    }

    private static void tell(ServerPlayer player, long now) {
        Long last = TOLD.get(player.getUUID());
        if (last == null || now - last >= TELL_EVERY || now < last) {
            TOLD.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable(CANNOT_ENTER), true);
        }
    }

    /** {@code ward_alert_owner}: "your ward was tested", no name and no kind (R325). */
    private static void alert(ServerLevel level, WardTable.Ward ward) {
        if (!ArtifactsConfig.wardAlertOwner()) {
            return;
        }
        ServerPlayer owner = WardService.find(level, ward.owner());
        long now = level.getGameTime();
        Long last = ALERTED.get(ward.owner());
        if (owner != null && (last == null || now - last >= ALERT_EVERY || now < last)) {
            ALERTED.put(ward.owner(), now);
            owner.displayClientMessage(Component.translatable(TESTED), true);
        }
    }

    /** The fallback of the push: to just outside the nearest face, at the first height with room, else where it stands. */
    private static void moveOut(ServerLevel level, LivingEntity entity, WardRules.Box box) {
        WardRules.Point out = WardRules.exitPoint(box, entity.getX(), entity.getZ());
        double dx = out.x() - entity.getX();
        double dz = out.z() - entity.getZ();
        for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
            AABB moved = entity.getBoundingBox().move(dx, dy, dz);
            if (level.noCollision(entity, moved)) {
                double y = entity.getY() + dy;
                if (entity instanceof ServerPlayer player) {
                    player.teleportTo(out.x(), y, out.z());
                } else {
                    entity.moveTo(out.x(), y, out.z(), entity.getYRot(), entity.getXRot());
                }
                return;
            }
        }
    }

    /** Whether a click on {@code pos} is stopped: the cell is inside a standing door ward that turns this player away. */
    static boolean doorShut(ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) {
        for (WardTable.Ward ward : WardIndex.get(level).table().standingIn(dimension(level))) {
            if (ward.kind() == WardRules.Kind.DOOR && ward.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) && blocks(ward, player)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public static boolean doorShutFor(ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) {
        return doorShut(level, pos, player);
    }
}
