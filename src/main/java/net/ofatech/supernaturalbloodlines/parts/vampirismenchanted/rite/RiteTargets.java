package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import java.util.Comparator;
import java.util.Optional;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiteTargets {
    private RiteTargets() {
    }

    /**
     * The player standing on the circle's centre block, else the caster (if online). {@code params} is needed because
     * Enchanted keeps the caster id private to the rite and only hands it to {@code onStart}.
     */
    public static Optional<ServerPlayer> centrePlayerOrCaster(Rite rite, Rite.RiteParams params, ServerLevel level) {
        Vec3 centre = Vec3.atBottomCenterOf(rite.getPos());
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(rite.getPos()).expandTowards(0, 1, 0),
                        p -> p.isAlive() && !p.isSpectator()).stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(centre)))
                .or(() -> Optional.ofNullable(level.getPlayerByUUID(params.caster)).filter(ServerPlayer.class::isInstance)
                        .map(ServerPlayer.class::cast));
    }
}
