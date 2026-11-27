package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/** Who counts as an innocent victim: the one rule the werewolf spark, the Code and the curse's self-defence share. */
public final class Innocence {
    private Innocence() {
    }

    /** A villager, or a player who did not deliberately damage the killer in the last 10 seconds ({@link FightLedger}). */
    public static boolean of(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Villager) {
            return true;
        }
        return victim instanceof Player p && !FightLedger.struck(p.getUUID(), killer.getUUID(), PackIndex.now());
    }
}
