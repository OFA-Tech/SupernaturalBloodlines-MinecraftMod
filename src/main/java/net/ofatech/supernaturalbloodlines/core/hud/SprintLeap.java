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
     */
    public static boolean shouldLeap(boolean setting, boolean jumpPressed, boolean sprinting, boolean canJump, boolean werewolf, boolean transformed,
                                     boolean unlocked, boolean onCooldown, boolean active, boolean screenOpen) {
        return setting && jumpPressed && sprinting && canJump && werewolf && transformed && unlocked && !onCooldown && !active && !screenOpen;
    }
}
