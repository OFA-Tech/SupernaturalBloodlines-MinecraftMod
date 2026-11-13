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
 * is left goes to the altar. The siphoned power is only taken when the rite really went ahead ({@link #commit()}), so an
 * altar that cannot pay the rest costs the siphoner nothing.
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

    /** Returns the cost the altar has to pay; remembers the siphoned part until {@link #commit()} or {@link #discard()}. */
    public static int begin(Level level, UUID caster, PowerConsumer.PowerPosHolder holder, int base) {
        PENDING.remove();
        if (level == null || level.isClientSide || caster == null || base <= 0
                || !(level.getPlayerByUUID(caster) instanceof ServerPlayer p)) {
            return base;
        }
        List<BlockPos> altars = holder.getPositions();
        BloodlineState state = BloodlineAccess.get(p);
        int available = Siphoning.canSiphon(state.species()) ? state.siphonedPower() : 0;
        Split s = split(base, WitchAltarBonus.appliesToCaster(level, caster, altars), available);
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
