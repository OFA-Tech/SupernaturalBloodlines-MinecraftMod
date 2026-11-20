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

    /** What the caster and the one on the circle are told for a refusal that is about that one; {@code target} is null when nobody else is told. */
    public record Told(String caster, String target) {
    }

    /**
     * A refusal that says something about the target (their gene, that they are an Original, a hybrid) goes to the target only: a caster who is not the
     * target is told the rite would change nothing, so a refusal cannot be used to ask a stranger on the circle what they are.
     */
    public static Told refusalAbout(boolean casterIsTarget, String key) {
        return casterIsTarget ? new Told(key, null) : new Told(FactionRiteFactory.NO_EFFECT, key);
    }

    public static void refuseAbout(net.minecraft.server.level.ServerPlayer caster, net.minecraft.server.level.ServerPlayer target, String key) {
        Told told = refusalAbout(caster == target, key);
        caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.caster()), true);
        if (told.target() != null) {
            target.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.target()), true);
        }
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
