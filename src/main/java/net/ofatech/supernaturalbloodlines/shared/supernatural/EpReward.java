package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.ObjLongConsumer;
import net.minecraft.world.entity.player.Player;

/**
 * Pays Tensura EP for a rung. The supernatural_tensura part installs the real payer; without Tensura (or with that part off) it
 * does nothing, which is how a rung pays no EP when Tensura is absent.
 */
public final class EpReward {
    private static volatile ObjLongConsumer<Player> payer;

    private EpReward() {
    }

    public static void set(ObjLongConsumer<Player> payer) {
        EpReward.payer = payer;
    }

    /** True when something pays EP at all (Tensura is there and its part is on). */
    public static boolean installed() {
        return payer != null;
    }

    /** Pays and returns the EP that was really paid: 0 when nothing is installed. */
    public static long pay(Player player, long ep) {
        ObjLongConsumer<Player> p = payer;
        if (p == null || ep <= 0) {
            return 0;
        }
        p.accept(player, ep);
        return ep;
    }
}
