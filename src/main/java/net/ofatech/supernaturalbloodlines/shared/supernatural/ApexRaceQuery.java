package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import org.jetbrains.annotations.Nullable;

/**
 * The path of the player's Tensura race when it is one of our apex races ({@code primordial_vampire}), else null, and whether that
 * race is at the apex tier of its line for a supernatural race (the {@code tensura_tier} detector). The supernatural_tensura part
 * installs both; without Tensura nobody is on an apex race and nobody is at a tier.
 */
public final class ApexRaceQuery {
    private static volatile Function<Player, String> query = p -> null;
    private static volatile BiPredicate<Player, SupernaturalRace> tier = (p, r) -> false;

    private ApexRaceQuery() {
    }

    public static void set(Function<Player, String> function) {
        query = function;
    }

    public static void setTier(BiPredicate<Player, SupernaturalRace> predicate) {
        tier = predicate;
    }

    /** The player's Tensura race is at the apex tier of a line this race may be on (or is already an apex race of ours). */
    public static boolean atApexTier(Player p, SupernaturalRace race) {
        return tier.test(p, race);
    }

    @Nullable
    public static String pathOf(Player p) {
        return query.apply(p);
    }
}
