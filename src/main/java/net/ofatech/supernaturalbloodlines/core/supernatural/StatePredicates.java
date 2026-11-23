package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;
import java.util.OptionalLong;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackMoonRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;

/**
 * The {@code state} predicates of the ladders, read from the bloodline state. A value is a number (kills, days) or 1 when it
 * holds; 0 when it does not. No game classes: what needs the world (emissary, moon control) is passed in.
 */
public final class StatePredicates {
    public static final long DAY_TICKS = 24_000L;

    /** Every predicate id the ladders may use. */
    public static final List<String> IDS = List.of("moon_unbound", "emissary", "rank_beta", "rank_alpha", "beta_days", "moon_control",
            "defender_kills", "mark_kills", "siphoned_power", "hunter_of_the_five");

    private StatePredicates() {
    }

    /** @return empty for an id this version does not know */
    public static OptionalLong value(String predicate, BloodlineState s, long gameTime, boolean emissary, boolean moonControl) {
        WolfState.Rank rank = s.wolf().rank();
        return switch (predicate) {
            // R24a: a hybrid is bound until the Rite of the Moon
            case "moon_unbound" -> OptionalLong.of(PackMoonRules.hybridLike(s.species(), s.hybridCurseBound()) ? 1 : 0);
            case "emissary" -> OptionalLong.of(emissary ? 1 : 0);
            case "rank_beta" -> OptionalLong.of(rank == WolfState.Rank.BETA || isAlpha(rank) ? 1 : 0);
            // Alpha the R90 way, True Alpha only the R91 way: the rank is the proof either way
            case "rank_alpha" -> OptionalLong.of(isAlpha(rank) ? 1 : 0);
            // a Beta's days in the pack; an Alpha's betaSince is cleared, so for Beta-or-Alpha time use the pack_days counter
            case "beta_days" -> OptionalLong.of(rank == WolfState.Rank.BETA && s.wolf().betaSince() > 0
                    ? Math.max(0, gameTime - s.wolf().betaSince()) / DAY_TICKS : 0);
            case "moon_control" -> OptionalLong.of(moonControl ? 1 : 0);
            case "defender_kills" -> OptionalLong.of(s.wolf().defenderKills());
            case "mark_kills" -> OptionalLong.of(s.markKills());
            case "siphoned_power" -> OptionalLong.of(s.siphonedPower());
            case "hunter_of_the_five" -> OptionalLong.of(s.species() == Species.HUNTER_OF_THE_FIVE ? 1 : 0);
            default -> OptionalLong.empty();
        };
    }

    /** In the pack as a Beta, Alpha or True Alpha: the rank that makes {@code pack_days} count. */
    public static boolean inPack(BloodlineState s) {
        WolfState.Rank rank = s.wolf().rank();
        return rank == WolfState.Rank.BETA || isAlpha(rank);
    }

    private static boolean isAlpha(WolfState.Rank rank) {
        return rank == WolfState.Rank.ALPHA || rank == WolfState.Rank.TRUE_ALPHA;
    }

}
