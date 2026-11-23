package net.ofatech.supernaturalbloodlines.parts.hud.client;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The creature the crosshair is on, as far as a Hybrid Leap may reach. The game's own pick stops at the interaction range (3 blocks), far short of a leap, so
 * this casts the look ray itself: it ends at the first block, and takes the nearest living creature on it. The server checks the same things again (in reach, in sight,
 * in front) and decides.
 */
public final class LeapAim {
    /** The farthest the ray goes: the config range of the leap tops out here. */
    public static final double MAX_RANGE = net.ofatech.supernaturalbloodlines.core.hud.LeapMath.MAX_RANGE;

    private LeapAim() {
    }

    @Nullable
    public static Entity pick(LocalPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1.0F).scale(MAX_RANGE));
        HitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB sweep = p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, sweep, e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), MAX_RANGE * MAX_RANGE);
        return hit == null ? null : hit.getEntity();
    }
}
