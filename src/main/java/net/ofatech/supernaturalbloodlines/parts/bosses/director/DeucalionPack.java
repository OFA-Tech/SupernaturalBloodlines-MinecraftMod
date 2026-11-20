package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import org.jetbrains.annotations.Nullable;

/** Deucalion's Alpha pack. Links the Werewolves mod (through the species part): touched only behind {@code ModList.isLoaded("werewolves")}. */
final class DeucalionPack {
    private DeucalionPack() {
    }

    /** {@code count} Alphas round {@code spot}, marked as his, set on {@code target}; their ids go into {@code into}. */
    static void summon(ServerLevel level, BlockPos spot, int count, @Nullable LivingEntity target, List<UUID> into) {
        for (Mob alpha : NpcPackSpawner.spawnBossAlphas(level, spot, count, level.getDayTime() / 24000L)) {
            into.add(alpha.getUUID());
            if (target != null) {
                alpha.setTarget(target);
            }
        }
    }
}
