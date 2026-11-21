package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * A Tensura skill's cooldown as ManasCore keeps it. {@code ManasSkillInstance.getCoolDown(mode)} counts <b>seconds</b>, not ticks: ManasCore's
 * {@code SkillStorage.checkSkillTimers} runs once every 20 server ticks and lowers each running cooldown by 1. Reading it as ticks (dividing by 20 and
 * rounding up) showed "1s" for any cooldown of up to 20 seconds. Pure.
 */
public final class SkillCooldown {
    private SkillCooldown() {
    }

    /** The seconds to show for a ManasCore cooldown value. */
    public static int seconds(int manasCoreCooldown) {
        return Math.max(0, manasCoreCooldown);
    }

    /** How much of the cooldown is left (1 just started, 0 ready), against the longest one seen since the skill was ready. */
    public static float fraction(int left, int peak) {
        return ActionCooldown.fraction(left, peak);
    }
}
