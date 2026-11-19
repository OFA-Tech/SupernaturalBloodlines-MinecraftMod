package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * What only the species part knows about a player's tonight, for the hub's "The world now" (R140): whether the full-moon sleep rule applies to them,
 * and whether an awakening waits. The species part installs it; without it both are false. The client works out the moon itself, so these two are
 * the whole of what the server adds.
 */
public final class WorldNowHook {
    /** @param sleepRule the full-moon sleep rule applies to this player on a full-moon night; @param awakeningPending a werewolf gene waits for a dusk */
    public record Flags(boolean sleepRule, boolean awakeningPending) {
        public static final Flags NONE = new Flags(false, false);
    }

    private static volatile Function<ServerPlayer, Flags> source = p -> Flags.NONE;

    private WorldNowHook() {
    }

    public static void set(Function<ServerPlayer, Flags> function) {
        source = function == null ? p -> Flags.NONE : function;
    }

    public static Flags of(ServerPlayer p) {
        return source.apply(p);
    }
}
