package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.common.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * R61: a witch (and the heretic and tribrid, who keep the magic) widens an altar's range x1.5 and pays x0.75 for rites.
 * An altar has no owner, so "a witch is at the altar" means a witch within the altar's normal range, and the rite side
 * uses the chalk's caster. The mixins call in here; off (config) or no witch means the multipliers are 1.
 */
public final class WitchAltarBonus {
    public static final double RANGE_MULTIPLIER = 1.5;
    public static final double COST_MULTIPLIER = 0.75;

    private WitchAltarBonus() {}

    public static int scaledRange(int base) {
        return (int) Math.round(base * RANGE_MULTIPLIER);
    }

    /** Rounded up, so a rite that costs power never becomes free. */
    public static int scaledCost(int base) {
        return base <= 0 ? 0 : (int) Math.ceil(base * COST_MULTIPLIER);
    }

    public static boolean hasBonus(Species s) {
        return s == Species.WITCH || s == Species.HERETIC || s == Species.TRIBRID;
    }

    public static boolean isNear(Player p, BlockPos altar, int range) {
        return p.distanceToSqr(altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5) <= (double) range * range;
    }

    /** The altar's range with the bonus: {@code base} unless a bonus species stands within {@code base} blocks of it. */
    public static int rangeFor(Level level, BlockPos altar, int base) {
        if (level == null || level.isClientSide || !SpeciesConfig.witchBonusEnabled()) {
            return base;
        }
        for (Player p : level.players()) {
            if (hasBonus(BloodlineAccess.get(p).species()) && isNear(p, altar, base)) {
                return scaledRange(base);
            }
        }
        return base;
    }

    /** True when {@code caster} is online in {@code level}, has the bonus and stands in range of one of the chalk's altars. */
    public static boolean appliesToCaster(Level level, UUID caster, List<BlockPos> altars) {
        if (level == null || caster == null || !SpeciesConfig.witchBonusEnabled()) {
            return false;
        }
        Player p = level.getPlayerByUUID(caster);
        if (p == null || !hasBonus(BloodlineAccess.get(p).species())) {
            return false;
        }
        int range = ServerConfig.INSTANCE.altarRange.get();
        return altars.stream().anyMatch(a -> isNear(p, a, range));
    }
}
