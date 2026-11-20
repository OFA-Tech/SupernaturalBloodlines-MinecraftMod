package net.ofatech.supernaturalbloodlines.parts.species.line;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.RingFill;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * What the Rite of the Imprisoned Original reads from the world: whether a mountain ash circle closes around the rite's centre, and
 * which Original stands trapped on it. The species part finds the ring block by its registry id (it belongs to the artifacts part).
 */
public final class Imprisonment {
    public static final ResourceLocation DESICCATED = ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "desiccated");
    public static final ResourceLocation RING_BLOCK = ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "mountain_ash_line");

    private Imprisonment() {
    }

    /** Mountain ash on the centre's level, or one above or below it, closing within {@code maxRadius} blocks. A world without the block is never closed. */
    public static boolean ringClosed(ServerLevel level, BlockPos centre, int maxRadius) {
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(RING_BLOCK);
        if (block.isEmpty()) {
            return false;
        }
        return RingFill.enclosed((dx, dz) -> {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos at = centre.offset(dx, dy, dz);
                if (level.isLoaded(at) && level.getBlockState(at).is(block.get())) {
                    return true;
                }
            }
            return false;
        }, 0, 0, maxRadius);
    }

    /** The Original on the centre block: a player Original or an NPC Original entity; the nearest to the centre wins. */
    public static Optional<LivingEntity> trapped(ServerLevel level, BlockPos centre) {
        Vec3 middle = Vec3.atBottomCenterOf(centre);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(centre).expandTowards(0, 1, 0), LivingEntity::isAlive).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(middle)))
                .filter(e -> originalRoot(e).isPresent()).findFirst();
    }

    /** The root of the Original on the centre block: a player Original (their own UUID) or an NPC Original entity. */
    public static Optional<UUID> trappedRoot(ServerLevel level, BlockPos centre) {
        return trapped(level, centre).flatMap(Imprisonment::originalRoot);
    }

    /** Whether the Original is brought down: downed, in Vampirism's dying state, or desiccated. Standing in the ring is not enough. */
    public static boolean isDown(LivingEntity e) {
        if (e instanceof ServerPlayer p && (Downed.isDowned(p) || VampirismAPI.vampirePlayer(p).isDBNO())) {
            return true;
        }
        if (e.hasData(SpeciesRegistries.NPC_OPEN) && e.getData(SpeciesRegistries.NPC_OPEN)) {
            return true; // an NPC Original (plan 17) that is desiccated is down without the effect
        }
        return BuiltInRegistries.MOB_EFFECT.getHolder(DESICCATED).map(e::hasEffect).orElse(false);
    }

    private static Optional<UUID> originalRoot(LivingEntity e) {
        if (e instanceof ServerPlayer p) {
            return OriginalRules.isOriginal(BloodlineAccess.get(p).species()) ? Optional.of(p.getUUID()) : Optional.empty();
        }
        return e.hasData(SpeciesRegistries.ORIGINAL_ROOT) ? NpcOriginal.byId(e.getData(SpeciesRegistries.ORIGINAL_ROOT)).map(NpcOriginal::uuid) : Optional.empty();
    }
}
