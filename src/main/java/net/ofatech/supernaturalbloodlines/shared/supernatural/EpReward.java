package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.ObjLongConsumer;
import net.minecraft.world.entity.player.Player;

/**
 * Pays Tensura EP for a rung. The supernatural_tensura part installs the real payer; without Tensura (or with that part off) it
 * does nothing, which is how a rung pays no EP when Tensura is absent.
 */
public final class EpReward {
    private static volatile ObjLongConsumer<Player> payer = (p, ep) -> { };

    private EpReward() {
    }

    public static void set(ObjLongConsumer<Player> payer) {
        EpReward.payer = payer;
    }

    public static void pay(Player player, long ep) {
        if (ep > 0) {
            payer.accept(player, ep);
        }
    }
}
