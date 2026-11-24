package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleCost;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;

/**
 * The one place a rite takes EP from a player or gives it back ({@code NoDirectEpSpendTest}), so a circle can split the rite's price: with a plan staged by
 * the rite cost hook the members pay their shares and the target only what is its own share (nothing when it stands outside the circle).
 */
public final class RiteEp {
    private RiteEp() {
    }

    /** Called once by the evolution rites part: the circle's EP floor reads base Max EP from here (the species part has no Tensura). */
    public static void install() {
        CircleCost.baseMaxEp = EnergyHelper::getBaseMaxEP;
    }

    private static Map<UUID, CirclePlan.Share> shares() {
        CircleStage.Staged staged = CircleStage.current();
        return staged == null ? Map.of() : staged.result().shares();
    }

    /** Takes the rite's EP price: from the target alone for a solo cast, from each member of the staged circle (their share) otherwise. */
    public static void charge(ServerPlayer target, double total) {
        Map<UUID, CirclePlan.Share> shares = shares();
        if (shares.isEmpty()) {
            if (total > 0) {
                EnergyHelper.setBaseMaxEP(target, EnergyHelper.getBaseMaxEP(target) - total);
            }
            return;
        }
        for (Map.Entry<UUID, CirclePlan.Share> e : shares.entrySet()) {
            if (e.getValue().ep() > 0 && target.serverLevel().getPlayerByUUID(e.getKey()) instanceof ServerPlayer member) {
                EnergyHelper.setBaseMaxEP(member, EnergyHelper.getBaseMaxEP(member) - e.getValue().ep());
            }
        }
    }

    /** Puts base Max EP back to {@code value}: Tensura's own price for a learn that was refused or undone (never shared). */
    public static void restore(ServerPlayer target, double value) {
        EnergyHelper.setBaseMaxEP(target, value);
    }

    /** What the target must hold for the rite: the whole price for a solo cast, its own share inside a circle, nothing outside it. */
    public static double owedBy(ServerPlayer target, double total) {
        Map<UUID, CirclePlan.Share> shares = shares();
        if (shares.isEmpty()) {
            return total;
        }
        CirclePlan.Share own = shares.get(target.getUUID());
        return own == null ? 0 : own.ep();
    }
}