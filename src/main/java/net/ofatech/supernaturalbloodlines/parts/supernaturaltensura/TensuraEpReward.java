package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.shared.supernatural.EpReward;

/** Adds the rung's EP to the player's Tensura existence: a flat sum, no +25% boost and no evolution requirement touched. */
public final class TensuraEpReward {
    private TensuraEpReward() {
    }

    public static void register() {
        EpReward.set(TensuraEpReward::add);
    }

    static void add(Player p, long ep) {
        IExistence existence = TensuraStorages.getExistenceFrom(p);
        existence.setEP(existence.getEP() + ep);
        existence.markDirty();
    }
}
