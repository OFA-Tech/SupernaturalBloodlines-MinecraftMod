package net.ofatech.supernaturalbloodlines.parts.supernatural.task;

import java.util.Comparator;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;
import org.jetbrains.annotations.Nullable;

/**
 * Called by {@code RiteCompleteMixin} when a rite worked. The rite's id is the id of its datapack rite type
 * ({@code supernaturalbloodlines:ward_of_return}, {@code enchanted:...}); the performer is the caster, the target is the rite's
 * own target if it is a player online, else the player standing on the circle's centre, else the caster.
 */
public final class RiteSignal {
    private static boolean warned;

    private RiteSignal() {}

    public static void completed(Rite rite, Rite.RiteParams params) {
        if (!SupernaturalConfig.riteCompleteSignal()) {
            return;
        }
        try {
            ServerLevel level = rite.getLevel();
            ResourceLocation id = level.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY).getKey(rite.getType());
            if (id == null) {
                return;
            }
            Player performer = online(level, params.caster);
            Player target = online(level, params.target);
            if (target == null) {
                target = centrePlayer(level, rite);
            }
            TaskSignals.rite(performer, target != null ? target : performer, id.toString());
        } catch (RuntimeException e) {
            // never let the signal break a rite
            if (!warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("supernatural: could not signal a completed rite", e);
            }
        }
    }

    @Nullable
    private static Player online(ServerLevel level, @Nullable UUID id) {
        return id == null ? null : level.getPlayerByUUID(id);
    }

    @Nullable
    private static Player centrePlayer(ServerLevel level, Rite rite) {
        Vec3 centre = Vec3.atBottomCenterOf(rite.getPos());
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(rite.getPos()).expandTowards(0, 1, 0),
                        p -> p.isAlive() && !p.isSpectator()).stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(centre))).orElse(null);
    }
}
