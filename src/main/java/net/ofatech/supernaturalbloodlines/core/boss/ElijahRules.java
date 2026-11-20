package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R170: Elijah comes once a day, with the chance {@code elijah_chance_per_day}, to a player who has discovered an Original or holds an Original's
 * blood, by day or by night, in the overworld, on the ground, in any difficulty (he is calm, so a peaceful world keeps nothing away). He is calm: he fights only if he is hit, or after an
 * oath was broken.
 */
public final class ElijahRules {
    public record Facts(boolean knowsOriginal, boolean holdsOriginalBlood, boolean overworld, boolean onGround, boolean spectator) {
    }

    private ElijahRules() {
    }

    public static boolean eligible(Facts f) {
        return (f.knowsOriginal() || f.holdsOriginalBlood()) && f.overworld() && f.onGround() && !f.spectator();
    }

    /** Whether he fights this player: he was hit by them, or an oath was broken (then everybody). */
    public static boolean fights(boolean hostileForGood, boolean hitByThem) {
        return hostileForGood || hitByThem;
    }
}