package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The path of the player's Tensura race when it is one of our apex races ({@code primordial_vampire}), else null. The
 * supernatural_tensura part installs it; without Tensura nobody is on an apex race.
 */
public final class ApexRaceQuery {
    private static volatile Function<Player, String> query = p -> null;

    private ApexRaceQuery() {
    }

    public static void set(Function<Player, String> function) {
        query = function;
    }

    @Nullable
    public static String pathOf(Player p) {
        return query.apply(p);
    }
}
