package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/** Who counts as an innocent victim: the one rule the werewolf spark and the brotherhood's Code share. */
public final class Innocence {
    /** A player who attacked the killer within this many ticks of dying was fighting them, not innocent. */
    public static final int FIGHT_WINDOW = 200;

    private Innocence() {
    }

    /** A villager, or a player who was not fighting the killer (did not hit them in the last 10 seconds). */
    public static boolean of(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Villager) {
            return true;
        }
        return victim instanceof Player p && !(p.getLastHurtMob() == killer && p.tickCount - p.getLastHurtMobTimestamp() <= FIGHT_WINDOW);
    }
}