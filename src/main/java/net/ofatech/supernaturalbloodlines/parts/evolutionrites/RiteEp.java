package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/** The one place a rite takes EP from a player or gives it back ({@code NoDirectEpSpendTest}), so a circle can split the rite's price. */
public final class RiteEp {
    private RiteEp() {
    }

    /** Takes the rite's EP price from the target's base Max EP. */
    public static void charge(ServerPlayer target, double total) {
        if (total > 0) {
            EnergyHelper.setBaseMaxEP(target, EnergyHelper.getBaseMaxEP(target) - total);
        }
    }

    /** Puts base Max EP back to {@code value}: Tensura's own price for a learn that was refused or undone. */
    public static void restore(ServerPlayer target, double value) {
        EnergyHelper.setBaseMaxEP(target, value);
    }

    /** What the target must hold for the rite: the whole price. */
    public static double owedBy(ServerPlayer target, double total) {
        return total;
    }
}
