package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.api.altar.PowerConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;

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
        int cost = witch ? WitchAltarBonus.scaledCost(base) : Math.max(0, base);
        int used = Math.min(cost, Math.max(0, siphonedAvailable));
        return new Split(cost - used, used);
    }

    /**
     * Returns the cost the altar has to pay. With {@code start} (the one-off start cost) the caster's siphoned power
     * pays first and is remembered until {@link #commit()} or {@link #discard()}; per-tick costs only get the witch
     * discount, so siphoned power never syncs every tick.
     */
    public static int begin(Level level, UUID caster, BlockPos chalk, PowerConsumer.PowerPosHolder holder, int base, boolean start) {
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
        Split s = split(base, witch, available);
        if (s.siphoned() > 0) {
            PENDING.set(new Pending(p, s.siphoned()));
        }
        return s.altarCost();
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
