package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.Locale;

/**
 * Who cannot use a bed on a full-moon night (R141, {@code full_moon_sleep}). From dusk (tick 12000, earlier than {@link MoonRule#NIGHT_START}, so
 * nobody sleeps just before the shift) of the phase-0 day until the morning, in the Overworld only. Pure.
 */
public final class SleepRules {
    public static final long DUSK = 12000;

    public enum Mode {
        OFF, WEREWOLVES, EVERYONE;

        /** The config value; anything else is {@link #OFF}. */
        public static Mode parse(String s) {
            if (s != null) {
                for (Mode m : values()) {
                    if (m.name().equals(s.trim().toUpperCase(Locale.ROOT))) {
                        return m;
                    }
                }
            }
            return OFF;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private SleepRules() {
    }

    /** Whether the player may not sleep now. {@code phase} is the moon phase of the day {@code dayTime} is in. */
    public static boolean blocked(Mode mode, boolean werewolf, boolean hasFixedTime, int phase, long dayTime) {
        if (mode == Mode.OFF || hasFixedTime || phase != 0 || Math.floorMod(dayTime, 24000L) < DUSK) {
            return false;
        }
        return mode == Mode.EVERYONE || werewolf;
    }

    /** Whether the rule would apply to this player on a full-moon night: the part of the rule the server knows and the client does not. */
    public static boolean appliesTo(Mode mode, boolean werewolf) {
        return mode == Mode.EVERYONE || (mode == Mode.WEREWOLVES && werewolf);
    }
}
