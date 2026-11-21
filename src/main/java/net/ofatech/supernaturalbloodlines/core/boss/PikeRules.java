package net.ofatech.supernaturalbloodlines.core.boss;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R188 and R189: the Valet Pike. Only it hurts the Spirit of Gevaudan, and one blow of it ends the Beast for good. It is made by a witch-type performer, on a full
 * moon night, from the bottled blood of somebody who is more than a plain player: a Hunter of the Five, an advanced vampire or werewolf (rung
 * {@link #ADVANCED_RUNG} or more of the ladder of 0 to 6), an Alpha or True Alpha, or an Original, a hybrid or a tribrid. It is never a drop.
 */
public final class PikeRules {
    /** The rung of the vampire or werewolf ladder from which the blood counts as advanced. */
    public static final int ADVANCED_RUNG = 4;

    /** Who the blood belonged to, as the rite finds out when it runs. */
    public enum Kind { PLAYER, NPC_ORIGINAL, OTHER, ABSENT }

    public record Giver(Kind kind, Species species, int vampireRung, int werewolfRung, boolean alpha) {
        public static Giver of(Kind kind) {
            return new Giver(kind, Species.NONE, 0, 0, false);
        }
    }

    private PikeRules() {
    }

    /** Whether the blood is strong enough for the pike. A plain player's blood is not; a creature's, or a giver who is not here, is not. */
    public static boolean qualifies(Giver g) {
        return switch (g.kind()) {
            case NPC_ORIGINAL -> true;
            case OTHER, ABSENT -> false;
            case PLAYER -> switch (g.species()) {
                case ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID, HUNTER_OF_THE_FIVE -> true;
                default -> g.alpha() || g.vampireRung() >= ADVANCED_RUNG || g.werewolfRung() >= ADVANCED_RUNG;
            };
        };
    }

    /** What the Spirit takes from a blow: only the Valet Pike hurts it. */
    public static boolean hurtsSpirit(boolean valetPike) {
        return valetPike;
    }

    /** A pike's blow on the Spirit ends the Beast for good, whatever the damage was. */
    public static boolean endsBeast(BeastPhase phase, boolean valetPike) {
        return valetPike && phase == BeastPhase.SPIRIT;
    }
}
