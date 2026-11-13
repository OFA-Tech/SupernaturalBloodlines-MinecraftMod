package net.ofatech.supernaturalbloodlines.parts.artifacts;

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

    static void register() {
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
