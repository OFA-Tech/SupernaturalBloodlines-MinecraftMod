package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.locator.LocatorRules;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.shared.species.LocatorTargets;

/**
 * Where the needle of an attuned Vampire Compass points (R316, R325). Every reason the one looked for cannot be found (the switch, offline, dead,
 * another dimension, out of range, cloaked, creative or spectator, the attunement spent, a seal that does not open) is the same answer, empty: the needle
 * spins, and nothing can be read from it. When they can be found it is the middle of a square around them, never their position, and the distance is in
 * hundreds of blocks.
 */
public final class LocatorNeedle {
    /** Where to point, and how far it says: -1 nothing, 0 very near, else blocks to the nearest hundred. */
    public record Aim(GlobalPos pos, int near) {}

    private LocatorNeedle() {
    }

    public static Optional<Aim> aim(ItemStack stack, ServerLevel level, Entity holder) {
        String seal = stack.get(ArtifactItems.LOCATOR_SEAL.get());
        Optional<LocatorTargets.Settings> settings = LocatorTargets.settings();
        Optional<UUID> target = seal == null ? Optional.empty() : LocatorTargets.open(seal);
        if (settings.isEmpty() || target.isEmpty()) {
            return Optional.empty();
        }
        Optional<LocatorTargets.Located> found = LocatorTargets.find(level.getServer(), target.get());
        LocatorTargets.Located at = found.orElse(null);
        double dx = at == null ? 0 : at.position().x - holder.getX();
        double dz = at == null ? 0 : at.position().z - holder.getZ();
        boolean ok = LocatorRules.findable(settings.get().enabled(), at != null && at.online(), at != null && at.alive(),
                at != null && at.dimension().equals(level.dimension().location()), dx * dx + dz * dz, settings.get().range(),
                at != null && at.cloaked(), at != null && at.spectator(), level.getGameTime(), stack.getOrDefault(ArtifactItems.LOCATOR_UNTIL.get(), 0L));
        if (!ok) {
            return Optional.empty();
        }
        long seed = target.get().getMostSignificantBits() ^ target.get().getLeastSignificantBits() ^ (level.getDayTime() / 24_000L);
        LocatorRules.Pt blurred = LocatorRules.blur(at.position().x, at.position().z, seed, settings.get().blur());
        LocatorRules.Distance distance = LocatorRules.distance((int) Math.hypot(blurred.x() - holder.getX(), blurred.z() - holder.getZ()));
        GlobalPos pos = GlobalPos.of(level.dimension(), BlockPos.containing(blurred.x(), holder.getY(), blurred.z()));
        return Optional.of(new Aim(pos, distance.veryNear() ? 0 : distance.blocks()));
    }
}
