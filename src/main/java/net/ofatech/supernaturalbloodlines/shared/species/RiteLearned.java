package net.ofatech.supernaturalbloodlines.shared.species;

import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;

/** "This player performed this rite" (not "was its target": a curse must not teach its victim): the supernatural part says it, the species part, which knows the unlocks, teaches it. */
public final class RiteLearned {
    private static volatile BiConsumer<Player, String> learner = (p, rite) -> {
    };

    private RiteLearned() {
    }

    public static void set(BiConsumer<Player, String> learner) {
        RiteLearned.learner = learner;
    }

    /** @param riteId the rite's id ({@code supernaturalbloodlines:rite_of_severance}) */
    public static void performed(Player player, String riteId) {
        learner.accept(player, riteId);
    }
}
