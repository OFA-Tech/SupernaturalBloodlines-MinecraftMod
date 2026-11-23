package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.ofatech.supernaturalbloodlines.core.hud.SprintLeap;

/** The one guard the Hybrid Leap and the sprint-leap share (R212): armed when a trigger for the Hybrid Leap is sent. Links no Werewolves class. */
public final class HybridLeapGuard {
    private static long until;

    private HybridLeapGuard() {
    }

    /** A Hybrid Leap was just asked for at game time {@code now}. */
    public static void thrown(long now) {
        until = SprintLeap.hybridGuardUntil(now);
    }

    public static boolean inFlight(long now) {
        return SprintLeap.hybridInFlight(now, until);
    }

    public static void reset() {
        until = 0;
    }
}