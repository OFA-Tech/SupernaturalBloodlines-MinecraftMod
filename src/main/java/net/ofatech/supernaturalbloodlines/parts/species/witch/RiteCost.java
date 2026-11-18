package net.ofatech.supernaturalbloodlines.parts.species.witch;

import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import org.jetbrains.annotations.Nullable;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.favouriteless.enchanted.common.init.EData;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.api.altar.PowerConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;

/**
 * The one rite cost hook (called from the chalk mixin): the witch discount, then the siphoner's own power, then whatever
 * is left goes to the altar. Siphoned power pays the start cost only, and is only taken once the rite has started
 * without cancelling itself ({@link #commit()} after {@code Rite.start()}, {@link #discard()} from {@code Rite.cancel()}),
 * so an altar that cannot pay, or a rite that refuses its target, costs the siphoner nothing.
 */
public final class RiteCost {
    /** What the altar is asked for and how much siphoned power pays the rest. */
    public record Split(int altarCost, int siphoned) {}

    private record Pending(ServerPlayer player, int siphoned) {}

    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    private RiteCost() {}

    public static Split split(int base, boolean witch, int siphonedAvailable) {
        return split(base, witch, false, siphonedAvailable);
    }

    public static Split split(int base, boolean witch, boolean emissary, int siphonedAvailable) {
        return split(base, witch, emissary, WitchAltarBonus.EMISSARY_MULTIPLIER, 1.0, 1.0, siphonedAvailable);
    }

    /**
     * The skills of the caster, as multipliers: {@code emissaryMultiplier} (0.9, or 0.8 with Emissary's Bond), {@code extra} for what
     * comes on top of the witch discount (Steady Circle 0.95, Ancestral Voice 0.5), and {@code siphonFactor} (Spent Wisely 0.9): the
     * part of the cost siphoned power pays costs that much of the pool, so the pool covers more.
     */
    public static Split split(int base, boolean witch, boolean emissary, double emissaryMultiplier, double extra, double siphonFactor,
                              int siphonedAvailable) {
        int cost = witch ? WitchAltarBonus.scaledCost(base) : Math.max(0, base);
        if (emissary) {
            cost = WitchAltarBonus.scaledEmissaryCost(cost, emissaryMultiplier);
        }
        if (extra < 1 && cost > 0) {
            cost = (int) Math.ceil(cost * extra - WitchAltarBonus.EPSILON);
        }
        int pool = Math.max(0, siphonedAvailable);
        int coverable = siphonFactor < 1 ? (int) Math.floor(pool / siphonFactor + WitchAltarBonus.EPSILON) : pool;
        int covered = Math.min(cost, coverable);
        int spent = siphonFactor < 1 ? (int) Math.ceil(covered * siphonFactor - WitchAltarBonus.EPSILON) : covered;
        return new Split(cost - covered, spent);
    }

    /**
     * Returns the cost the altar has to pay. With {@code start} (the one-off start cost) the caster's siphoned power
     * pays first and is remembered until {@link #commit()} or {@link #discard()}; per-tick costs only get the witch
     * discount, so siphoned power never syncs every tick.
     */
    public static int begin(Level level, UUID caster, BlockPos chalk, PowerConsumer.PowerPosHolder holder, int base, boolean start,
                            @Nullable RiteType type) {
        PENDING.remove();
        if (level == null || level.isClientSide || base <= 0) {
            return base;
        }
        List<BlockPos> altars = holder.getPositions();
        boolean witch = WitchAltarBonus.appliesTo(level, caster, chalk, altars);
        int available = 0;
        ServerPlayer p = start && caster != null && level.getPlayerByUUID(caster) instanceof ServerPlayer sp ? sp : null;
        if (p != null && Siphoning.canSiphon(BloodlineAccess.get(p).species())) {
            available = BloodlineAccess.get(p).siphonedPower();
        }
        ServerPlayer performer = caster != null && level.getPlayerByUUID(caster) instanceof ServerPlayer sp ? sp : null;
        double emissary = WitchAltarBonus.EMISSARY_MULTIPLIER;
        double extra = 1;
        double siphonFactor = 1;
        if (performer != null) {
            emissary = 1 - LadderQuery.value(performer, "S-WIT-4", "discount", 1 - WitchAltarBonus.EMISSARY_MULTIPLIER);
            extra = LadderQuery.value(performer, "S-WIT-1", "cost_factor", 1.0);
            if (isLineageRite(level, type)) {
                extra *= LadderQuery.value(performer, "S-WIT-6", "cost_factor", 1.0);
            }
            siphonFactor = LadderQuery.value(performer, "S-SIP-4", "payment_factor", 1.0);
        }
        extra *= LunarMoods.riteCost(level, () -> isRite(level, type, LunarMood.FULL_MOON_RITES));
        Split s = split(base, witch, WitchAltarBonus.emissaryApplies(level, caster), emissary, extra, siphonFactor, available);
        if (s.siphoned() > 0) {
            PENDING.set(new Pending(p, s.siphoned()));
        }
        return s.altarCost();
    }

    /** The two Rites of Lineage (data type {@code supernaturalbloodlines:lineage}): their cost is the ones Ancestral Voice halves. */
    static final Set<String> LINEAGE_RITES = Set.of("supernaturalbloodlines:rite_of_lineage_reveal", "supernaturalbloodlines:rite_of_lineage_awaken");

    private static boolean isLineageRite(Level level, @Nullable RiteType type) {
        return isRite(level, type, LINEAGE_RITES);
    }

    private static boolean isRite(Level level, @Nullable RiteType type, Set<String> ids) {
        if (type == null) {
            return false;
        }
        ResourceLocation id = level.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY).getKey(type);
        return id != null && ids.contains(id.toString());
    }

    public static void commit() {
        Pending pending = PENDING.get();
        PENDING.remove();
        if (pending != null) {
            BloodlineState state = BloodlineAccess.get(pending.player());
            BloodlineAccess.set(pending.player(), state.toBuilder().siphonedPower(Math.max(0, state.siphonedPower() - pending.siphoned())).build());
        }
    }

    public static void discard() {
        PENDING.remove();
    }
}
