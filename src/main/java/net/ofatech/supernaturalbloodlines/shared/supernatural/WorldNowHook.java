package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * What only the species part knows about a player's tonight, for the hub's "The world now" (R140): whether the full-moon sleep rule applies to them,
 * and whether an awakening waits. The species part installs it; without it both are false. The bosses part adds which boss is near. The client
 * works out the moon itself, so these are the whole of what the server adds.
 */
public final class WorldNowHook {
    /** The nearest boss within reach of the Home page: none, standing (out there) or fallen (an Original lying open to white oak), and who it is when this player may know. */
    public record BossNear(int kind, String name) {
        public static final int NONE_KIND = 0;
        public static final int STANDING = 1;
        public static final int FALLEN = 2;
        public static final BossNear NONE = new BossNear(NONE_KIND, "");

        public BossNear {
            kind = Math.max(NONE_KIND, Math.min(FALLEN, kind));
            name = kind == NONE_KIND || name == null ? "" : name;
        }
    }

    /**
     * @param sleepRule        the full-moon sleep rule applies to this player on a full-moon night
     * @param awakeningPending a werewolf gene waits for a dusk
     * @param trial            the Alpha Trial (R147): {@link #TRIAL_NONE}, {@link #TRIAL_POSSIBLE} (could be started tonight) or {@link #TRIAL_RUNNING}
     * @param bossNear         {@link BossNear#NONE_KIND}, {@link BossNear#STANDING} or {@link BossNear#FALLEN}: a boss (plan 17) within reach
     * @param bossName         the id of that boss when this player may know it (an Original they have discovered), else empty
     */
    public record Flags(boolean sleepRule, boolean awakeningPending, int trial, int bossNear, String bossName) {
        public static final int TRIAL_NONE = 0;
        public static final int TRIAL_POSSIBLE = 1;
        public static final int TRIAL_RUNNING = 2;
        public static final Flags NONE = new Flags(false, false, TRIAL_NONE, BossNear.NONE_KIND, "");

        public Flags(boolean sleepRule, boolean awakeningPending) {
            this(sleepRule, awakeningPending, TRIAL_NONE, BossNear.NONE_KIND, "");
        }

        public Flags(boolean sleepRule, boolean awakeningPending, int trial) {
            this(sleepRule, awakeningPending, trial, BossNear.NONE_KIND, "");
        }

        public Flags {
            trial = Math.max(TRIAL_NONE, Math.min(TRIAL_RUNNING, trial));
            BossNear clean = new BossNear(bossNear, bossName);
            bossNear = clean.kind();
            bossName = clean.name();
        }

        public Flags withBossNear(BossNear near) {
            return new Flags(sleepRule, awakeningPending, trial, near.kind(), near.name());
        }
    }

    private static volatile Function<ServerPlayer, Flags> source = p -> Flags.NONE;
    private static volatile Function<ServerPlayer, BossNear> bossNear = p -> BossNear.NONE;

    private WorldNowHook() {
    }

    public static void set(Function<ServerPlayer, Flags> function) {
        source = function == null ? p -> Flags.NONE : function;
    }

    /** The bosses part installs which boss is near a player. */
    public static void setBossNear(Function<ServerPlayer, BossNear> near) {
        bossNear = near == null ? p -> BossNear.NONE : near;
    }

    public static Flags of(ServerPlayer p) {
        Flags flags = source.apply(p);
        BossNear near = bossNear.apply(p);
        return near.kind() == BossNear.NONE_KIND ? flags : flags.withBossNear(near);
    }
}
