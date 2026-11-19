package net.ofatech.supernaturalbloodlines.parts.species.moon;

import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules.Pending;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * What a player's delayed awakening (R142) has waiting: a read and a write of the attachment, with no Werewolves class in it, so the hub's
 * "something stirs" flag can ask it whether or not Werewolves is installed. The awakening itself is {@link DelayedAwakening}.
 */
public final class AwakeningQueue {
    private AwakeningQueue() {
    }

    public static Pending of(Player p) {
        return p.getData(SpeciesRegistries.PENDING_AWAKENING);
    }

    public static boolean pending(Player p) {
        return of(p).waiting();
    }

    public static long dueOf(Player p) {
        return of(p).due();
    }

    public static void set(Player p, Pending pending) {
        p.setData(SpeciesRegistries.PENDING_AWAKENING, pending);
    }

    public static void clear(Player p) {
        p.removeData(SpeciesRegistries.PENDING_AWAKENING);
    }
}
