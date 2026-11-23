package net.ofatech.supernaturalbloodlines.parts.hud.action;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;

/** The creature a HUD action is aimed at. The client names one by id; the server decides whether it counts. */
public final class HudTargets {
    private HudTargets() {
    }

    /** Same level, alive, not the player, within {@code bite_reach}, in sight: exactly what a bite needs. */
    public static Optional<LivingEntity> resolve(ServerPlayer p, int entityId) {
        return resolve(p, entityId, 0.0);
    }

    /** The same, within {@code reach} blocks ({@code 0} or less: the bite reach). An action with a longer range names its target with this. */
    public static Optional<LivingEntity> resolve(ServerPlayer p, int entityId, double reach) {
        Entity found = entityId < 0 ? null : p.level().getEntity(entityId);
        double within = reach > 0.0 ? reach : HudConfig.biteReach();
        if (found instanceof LivingEntity living && living.isAlive() && living != p && p.distanceTo(living) <= within && p.hasLineOfSight(living)) {
            return Optional.of(living);
        }
        return Optional.empty();
    }
}
