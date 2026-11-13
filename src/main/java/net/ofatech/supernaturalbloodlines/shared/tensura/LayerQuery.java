package net.ofatech.supernaturalbloodlines.shared.tensura;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * The seam between species_tensura (which knows the species layer) and vampirism_tensura (which must not import it).
 * species_tensura installs the answers at init; until then, and when that part is off, both are false.
 */
public final class LayerQuery {
    private static volatile Predicate<Player> treatedAsUndead = p -> false;
    private static volatile Predicate<Player> vampireTypeSpecies = p -> false;

    private LayerQuery() {}

    public static void install(Predicate<Player> treatedAsUndeadBySpecies, Predicate<Player> isVampireTypeSpecies) {
        treatedAsUndead = treatedAsUndeadBySpecies;
        vampireTypeSpecies = isVampireTypeSpecies;
    }

    /** For tests of vampirism_tensura alone: runs the body as if species_tensura were off. */
    public static void runWithoutLayer(Runnable body) {
        Predicate<Player> undead = treatedAsUndead;
        Predicate<Player> vampire = vampireTypeSpecies;
        install(p -> false, p -> false);
        try {
            body.run();
        } finally {
            install(undead, vampire);
        }
    }

    /** The player's layer makes Tensura treat them as undead (so Tensura's own holy handling already applies). */
    public static boolean treatedAsUndeadBySpecies(Player p) {
        return treatedAsUndead.test(p);
    }

    /** A vampire-type species (R103: exempt from the undead/daemon denial for hunters and werewolves). */
    public static boolean isVampireTypeSpecies(Player p) {
        return vampireTypeSpecies.test(p);
    }
}
