package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * When jumping while sprinting asks Werewolves for its leap. Werewolves' own key path (javap, 2.0.3.4): with its leap key held and jump down, the client
 * sends {@code ServerboundSimpleInputEventPacket(LEAP)} only if the leap action is not on cooldown and the form is transformed, and toggles the action
 * itself; the server answers the packet with {@code toggleAction(LEAP)}, which is where unlocked, config and cooldown are checked. We ask the same way and
 * only for a jump that really starts a sprint jump, and never to turn an active leap off (a second toggle would). Pure.
 */
public final class SprintLeap {
    private SprintLeap() {
    }

    /**
     * @param setting        client setting {@code sprint_leap}
     * @param jumpPressed    the jump key went down this tick (not held)
     * @param sprinting      the player is running
     * @param canJump        on the ground, not riding, not swimming or flying: a jump that will happen
     * @param werewolf       in the werewolf faction
     * @param transformed    the form is transformed (Werewolves refuses a leap otherwise)
     * @param unlocked       the leap action is unlocked
     * @param onCooldown     the leap action is on cooldown
     * @param active         the leap action is already on (waiting for the jump)
     * @param screenOpen     a screen is open
     * @param hybridLeapInFlight a Hybrid Leap was just thrown (R212): the two leaps share this one guard, so a jump in the air of one never arms the other
     */
    public static boolean shouldLeap(boolean setting, boolean jumpPressed, boolean sprinting, boolean canJump, boolean werewolf, boolean transformed,
                                     boolean unlocked, boolean onCooldown, boolean active, boolean screenOpen, boolean hybridLeapInFlight) {
        return setting && jumpPressed && sprinting && canJump && werewolf && transformed && unlocked && !onCooldown && !active && !screenOpen && !hybridLeapInFlight;
    }

    /** How long (ticks) after a Hybrid Leap is thrown the sprint-leap stays off: the client does not see the server's flight, so it guesses it from its own trigger. */
    public static final int HYBRID_GUARD_TICKS = 20;

    /** The game time until which a Hybrid Leap thrown at {@code now} keeps the sprint-leap off. */
    public static long hybridGuardUntil(long now) {
        return now + HYBRID_GUARD_TICKS;
    }

    /** A Hybrid Leap is (still) in flight as far as the sprint-leap can tell. */
    public static boolean hybridInFlight(long now, long guardUntil) {
        return now < guardUntil;
    }

    /** Ticks after arming in which the jump must start (the player leaves the ground). */
    public static final int JUMP_WINDOW = 3;

    /**
     * The jump that the leap was armed for never happened (a ceiling, a trapdoor, water, a screen that grabbed the key): the leap would stay on for the next
     * jump, sprinting or not, so it is turned off again. Werewolves' own key has the same hole; ours closes it.
     *
     * @param ticksSinceArm ticks since the leap was switched on
     * @param leftGround    the player was off the ground at some tick since
     */
    public static boolean jumpBlocked(int ticksSinceArm, boolean leftGround) {
        return ticksSinceArm >= JUMP_WINDOW && !leftGround;
    }
}
