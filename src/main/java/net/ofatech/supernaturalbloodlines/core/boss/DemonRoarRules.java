package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R180: the Demon Wolf Roar (S-WOL-11, granted by Deucalion's death). On the Alpha Roar's key, with a cooldown of its own, it makes the other <b>player</b> Alphas
 * within range shift: werewolves, not the roarer and not the roarer's own pack, in the roarer's level. It is a way of forcing a player, so it follows the
 * rules of hurting one: nobody where the server has PvP off, nobody the roarer may not harm (the same team), nobody in spectator mode. A Beta is not an Alpha and
 * does not shift. The range and the cooldown are the skill's own parameters ({@code s-wol-11.json}); these are what they fall back to.
 */
public final class DemonRoarRules {
    public static final String SKILL = "S-WOL-11";
    public static final double RANGE = 32.0;
    public static final int COOLDOWN = 2400;

    private DemonRoarRules() {
    }

    public record Candidate(boolean werewolf, boolean alpha, boolean self, boolean ownPack, boolean sameLevel, double distanceSquared, boolean spectator, boolean canHarm) {
    }

    public static boolean shifts(Candidate c, double range, boolean pvpAllowed) {
        return pvpAllowed && c.werewolf() && c.alpha() && !c.self() && !c.ownPack() && c.sameLevel() && !c.spectator() && c.canHarm() && c.distanceSquared() <= range * range;
    }

    /** {@code last} is the server tick of the last roar, or -1 for none. */
    public static boolean ready(long last, long now, int cooldown) {
        return last < 0 || now - last >= cooldown;
    }

    public static long secondsLeft(long last, long now, int cooldown) {
        return Math.max(0, (cooldown - (now - last) + 19) / 20);
    }
}