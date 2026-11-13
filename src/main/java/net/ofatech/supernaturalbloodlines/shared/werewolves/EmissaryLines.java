package net.ofatech.supernaturalbloodlines.shared.werewolves;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * Who counts as a pack Emissary, for the artifacts part: a mountain ash line laid by an Emissary also stops the
 * supernatural mobs. The species part says who (it owns the packs), artifacts asks when a line is placed; neither
 * imports the other. Without the species part nobody is an Emissary.
 */
public final class EmissaryLines {
    private static volatile Predicate<Player> emissary = p -> false;

    private EmissaryLines() {
    }

    public static void setEmissaryCheck(Predicate<Player> check) {
        emissary = check;
    }

    public static boolean isEmissary(Player p) {
        return emissary.test(p);
    }
}
