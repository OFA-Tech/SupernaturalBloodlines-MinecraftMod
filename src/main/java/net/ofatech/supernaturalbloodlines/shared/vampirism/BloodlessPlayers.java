package net.ofatech.supernaturalbloodlines.shared.vampirism;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/** Whether a player has no blood to bite. The vampirism_tensura part installs Tensura's race check; without it every player has blood. */
public final class BloodlessPlayers {
    private static volatile Predicate<Player> check = p -> false;

    private BloodlessPlayers() {
    }

    public static void set(Predicate<Player> predicate) {
        check = predicate;
    }

    public static boolean test(Player p) {
        return check.test(p);
    }
}
