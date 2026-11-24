package net.ofatech.supernaturalbloodlines.core.group.coven;

/** Who may do what in a coven (R250, R251, R260, R261). */
public final class CovenRules {
    public enum Role { REGENT, ELDER, MEMBER }

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private CovenRules() {
    }

    public static boolean mayBeElder(long daysInCoven, int minDays) {
        return daysInCoven >= minDays;
    }

    /** {@code members} counts the Regent. Nobody is an Elder in a coven of two; from three there is at least one seat, and one more per {@code perElder} members. */
    public static int elderCap(int members, int perElder) {
        if (members < 3) {
            return 0;
        }
        return Math.max(1, members / Math.max(1, perElder));
    }

    /** An Elder or the Regent calls a shunning; the Regent can never be shunned. */
    public static boolean mayCallShun(Role caller, Role target) {
        return (caller == Role.REGENT || caller == Role.ELDER) && target != Role.REGENT;
    }

    /** An Elder calls for a new Regent and names an Elder; one election per cooldown ({@code cooldownUntil} is the millis it ends, 0 = none yet). */
    public static boolean mayCallElection(Role caller, Role nominee, long cooldownUntil, long now) {
        return caller == Role.ELDER && nominee == Role.ELDER && now >= cooldownUntil;
    }

    public static long banUntil(long now, int days) {
        return now + days * DAY_MILLIS;
    }

    public static boolean banned(long until, long now) {
        return until > now;
    }
}