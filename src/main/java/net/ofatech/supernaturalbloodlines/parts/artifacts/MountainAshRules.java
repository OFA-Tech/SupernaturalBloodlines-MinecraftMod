package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/** What the line does besides standing in the way (the shape and the path type are in the block). */
final class MountainAshRules {
    private MountainAshRules() {
    }

    static final int ASHWARDEN_CHECK_TICKS = 5;
    static final int ASHWARDEN_WEAKNESS_TICKS = 5 * 20;
    private static final double ASHWARDEN_PUSH = 0.6;

    /**
     * Ashwarden (S-WIT-3): a vampire or werewolf type that touches a line whose placer is online and owns the skill is pushed back
     * and gets Weakness I for 5 seconds. "Touch" is the body within a tenth of a block of the line's cell, checked every
     * {@value #ASHWARDEN_CHECK_TICKS} ticks and only for prey. A line whose placer is offline, never recorded or without the
     * skill (dormant included) only blocks, as before. A line never pushes its own placer (a tribrid witch is prey to their own line). (The skill reads the placer, so it follows them: no skill, no effect.)
     */
    // tests run side by side, so this is a set of players, not one lookup that the next test would replace
    private static final java.util.Map<java.util.UUID, Player> PINNED_OWNERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** For tests: lets a player the player list does not have (a fake player) own lines; {@code null} as the player removes the id. */
    static void pinOwner(java.util.UUID id, Player player) {
        if (player == null) {
            PINNED_OWNERS.remove(id);
        } else {
            PINNED_OWNERS.put(id, player);
        }
    }

    /** The line's owner when online anywhere on the server (the player list, not just this dimension). */
    private static Player owner(Level level, java.util.UUID id) {
        Player p = PINNED_OWNERS.get(id);
        return p != null || level.getServer() == null ? p : level.getServer().getPlayerList().getPlayer(id);
    }

    static void ashwarden(LivingEntity entity) {
        Level level = entity.level();
        if (level.isClientSide || entity.tickCount % ASHWARDEN_CHECK_TICKS != 0 || !Supernatural.isPrey(entity)) {
            return;
        }
        AABB box = entity.getBoundingBox().inflate(0.1, -0.05, 0.1);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.minY, box.maxZ))) {
            if (level.getBlockState(pos).is(MountainAsh.LINE.get()) && level.getBlockEntity(pos) instanceof MountainAshLineBlockEntity line
                    && line.owner() != null && !line.owner().equals(entity.getUUID())
                    && owner(level, line.owner()) instanceof Player owner && LadderQuery.owns(owner, "S-WIT-3")) {
                Vec3 away = entity.position().subtract(Vec3.atCenterOf(pos)).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : away.normalize();
                entity.setDeltaMovement(away.x * ASHWARDEN_PUSH, 0.2, away.z * ASHWARDEN_PUSH);
                entity.hurtMarked = true;
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ASHWARDEN_WEAKNESS_TICKS, 0));
                return;
            }
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof LivingEntity living) {
                ashwarden(living);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.BreakSpeed.class, e -> {
            // speed 0: never finishes. Instant mining skips the speed, so the break itself is refused below
            if (e.getState().is(MountainAsh.LINE.get()) && Supernatural.isPrey(e.getEntity())) {
                e.setNewSpeed(0.0f);
            }
        });
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, e -> {
            if (e.getState().is(MountainAsh.LINE.get()) && !e.getPlayer().isCreative() && Supernatural.isPrey(e.getPlayer())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(ProjectileImpactEvent.class, e -> {
            Entity owner = e.getProjectile().getOwner();
            if (owner != null && e.getRayTraceResult() instanceof BlockHitResult hit
                    && e.getProjectile().level().getBlockState(hit.getBlockPos()).is(MountainAsh.LINE.get()) && Supernatural.isPrey(owner)) {
                // arrows and tridents stick into the wall shape like into any block; the rest (fireballs, potions...) just vanish
                if (!(e.getProjectile() instanceof AbstractArrow)) {
                    e.setCanceled(true);
                    e.getProjectile().discard();
                }
            }
        });
    }
}
