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

    /** A villager, or a player who was not fighting the killer (did not hurt them in the last 10 seconds, by any means). */
    public static boolean of(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Villager) {
            return true;
        }
        return victim instanceof Player p && !fought(p, killer);
    }

    /**
     * The victim's own last target, or the killer's last attacker: the second covers what the first misses (a potion, an arrow's owner, TNT, a tamed wolf,
     * or a victim who hit the killer and then something else).
     */
    private static boolean fought(Player victim, ServerPlayer killer) {
        boolean struck = victim.getLastHurtMob() == killer && victim.tickCount - victim.getLastHurtMobTimestamp() <= FIGHT_WINDOW;
        boolean hurtBy = killer.getLastHurtByMob() == victim && killer.tickCount - killer.getLastHurtByMobTimestamp() <= FIGHT_WINDOW;
        return struck || hurtBy || killer.getKillCredit() == victim;
    }
}