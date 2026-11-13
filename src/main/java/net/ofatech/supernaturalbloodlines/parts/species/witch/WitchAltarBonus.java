package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.common.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Emissary;

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

    /** R94: an Emissary near their pack pays a further x0.9 for rites (after the witch discount, if they have it). */
    public static final double EMISSARY_MULTIPLIER = 0.9;

    /** Rounded up, like {@link #scaledCost}. */
    public static int scaledEmissaryCost(int base) {
        return base <= 0 ? 0 : (int) Math.ceil(base * EMISSARY_MULTIPLIER);
    }

    /** True when {@code caster} is a pack Emissary with a pack member within 16 blocks. */
    public static boolean emissaryApplies(Level level, UUID caster) {
        return level instanceof ServerLevel sl && Emissary.nearPack(sl, caster);
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

    /**
     * True when the caster, or the player standing on the circle's centre (the chalk's 1x2x1 column, where the rites look
     * for their target), has the bonus and stands in range of one of the chalk's altars.
     */
    public static boolean appliesTo(Level level, UUID caster, BlockPos chalk, List<BlockPos> altars) {
        if (level == null || !SpeciesConfig.witchBonusEnabled() || altars.isEmpty()) {
            return false;
        }
        int range = ServerConfig.INSTANCE.altarRange.get();
        List<Player> who = new ArrayList<>(level.getEntitiesOfClass(Player.class, new AABB(chalk).expandTowards(0, 1, 0)));
        Player c = caster == null ? null : level.getPlayerByUUID(caster);
        if (c != null) {
            who.add(c);
        }
        return who.stream().anyMatch(p -> hasBonus(BloodlineAccess.get(p).species())
                && altars.stream().anyMatch(a -> isNear(p, a, range)));
    }
}
