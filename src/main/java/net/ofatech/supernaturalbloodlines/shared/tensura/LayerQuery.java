package net.ofatech.supernaturalbloodlines.shared.tensura;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The seam between species_tensura (which knows the species layer) and vampirism_tensura (which must not import it).
 * species_tensura installs the answers at init; until then, and when that part is off, both are false.
 */
public final class LayerQuery {
    private static volatile Predicate<Player> treatedAsUndead = p -> false;
    private static volatile BiPredicate<Player, ResourceLocation> apexVampireType = (p, race) -> false;

    private LayerQuery() {}

    public static void install(Predicate<Player> treatedAsUndeadBySpecies, BiPredicate<Player, ResourceLocation> isApexVampireType) {
        treatedAsUndead = treatedAsUndeadBySpecies;
        apexVampireType = isApexVampireType;
    }

    /**
     * For tests of vampirism_tensura's own rules: runs the body with both answers false, as when species_tensura is
     * off. Tensura's own undead checks (the species_tensura mixin) are not affected.
     */
    public static void runWithoutLayer(Runnable body) {
        Predicate<Player> undead = treatedAsUndead;
        BiPredicate<Player, ResourceLocation> apex = apexVampireType;
        install(p -> false, (p, race) -> false);
        try {
            body.run();
        } finally {
            install(undead, apex);
        }
    }

    /**
     * The player's layer makes Tensura treat them as undead (so Tensura's own holy handling already applies), and the
     * hook into Tensura is really in place.
     */
    public static boolean treatedAsUndeadBySpecies(Player p) {
        return treatedAsUndead.test(p);
    }

    /**
     * R103: {@code raceId} is an apex race and the player's species is vampire-type, so the undead/daemon denial for
     * hunters and werewolves does not apply to that race. Pass the race the player has, or is about to get.
     */
    public static boolean isApexVampireType(Player p, ResourceLocation raceId) {
        return apexVampireType.test(p, raceId);
    }
}
