package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

/**
 * What only the species part knows about a player's tonight, for the hub's "The world now" (R140): whether the full-moon sleep rule applies to them,
 * and whether an awakening waits. The species part installs it; without it both are false. The bosses part adds whether a boss is near. The client
 * works out the moon itself, so these are the whole of what the server adds.
 */
public final class WorldNowHook {
    /**
     * @param sleepRule        the full-moon sleep rule applies to this player on a full-moon night
     * @param awakeningPending a werewolf gene waits for a dusk
     * @param trial            the Alpha Trial (R147): {@link #TRIAL_NONE}, {@link #TRIAL_POSSIBLE} (could be started tonight) or {@link #TRIAL_RUNNING}
     * @param bossNear         a boss (plan 17) is out there or down within 128 blocks
     */
    public record Flags(boolean sleepRule, boolean awakeningPending, int trial, boolean bossNear) {
        public static final int TRIAL_NONE = 0;
        public static final int TRIAL_POSSIBLE = 1;
        public static final int TRIAL_RUNNING = 2;
        public static final Flags NONE = new Flags(false, false, TRIAL_NONE, false);

        public Flags(boolean sleepRule, boolean awakeningPending) {
            this(sleepRule, awakeningPending, TRIAL_NONE, false);
        }

        public Flags(boolean sleepRule, boolean awakeningPending, int trial) {
            this(sleepRule, awakeningPending, trial, false);
        }

        public Flags {
            trial = Math.max(TRIAL_NONE, Math.min(TRIAL_RUNNING, trial));
        }

        public Flags withBossNear(boolean near) {
            return new Flags(sleepRule, awakeningPending, trial, near);
        }
    }

    private static volatile Function<ServerPlayer, Flags> source = p -> Flags.NONE;
    private static volatile Predicate<ServerPlayer> bossNear = p -> false;

    private WorldNowHook() {
    }

    public static void set(Function<ServerPlayer, Flags> function) {
        source = function == null ? p -> Flags.NONE : function;
    }

    /** The bosses part installs whether a boss is near a player. */
    public static void setBossNear(Predicate<ServerPlayer> near) {
        bossNear = near == null ? p -> false : near;
    }

    public static Flags of(ServerPlayer p) {
        Flags flags = source.apply(p);
        return bossNear.test(p) ? flags.withBossNear(true) : flags;
    }
}
