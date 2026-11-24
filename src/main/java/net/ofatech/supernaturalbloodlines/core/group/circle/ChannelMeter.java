package net.ofatech.supernaturalbloodlines.core.group.circle;

/** The per-witch channel meter: how much power a member can give a rite before it hurts. Refills lazily on the wall clock. */
public final class ChannelMeter {
    public enum Zone { CLEAN, NOSEBLEED, DAMAGE, REFUSED }

    private static final long MINUTE = 60_000L;

    private ChannelMeter() {
    }

    /** The meter now: {@code cap} comes back per {@code regenMinutes}, never past {@code cap}; a clock that went back adds nothing. */
    public static int valueAt(int stored, long atMillis, long nowMillis, int cap, int regenMinutes) {
        int start = Math.max(0, Math.min(stored, cap));
        if (nowMillis <= atMillis || cap <= 0) {
            return start;
        }
        long window = Math.max(1, regenMinutes) * MINUTE;
        long gained = (long) cap * (nowMillis - atMillis) / window;
        return (int) Math.min(cap, start + gained);
    }

    public static int capacity(boolean witchType, int cap, double otherFactor) {
        return witchType ? cap : (int) Math.floor(cap * Math.max(0, Math.min(1, otherFactor)));
    }

    /** {@code overdraw} = the share minus the meter. Up to half the capacity is a nosebleed, up to all of it pain, more is refused. */
    public static Zone zone(int overdraw, int cap) {
        if (overdraw <= 0) {
            return Zone.CLEAN;
        }
        if (overdraw <= cap / 2) {
            return Zone.NOSEBLEED;
        }
        return overdraw <= cap ? Zone.DAMAGE : Zone.REFUSED;
    }

    /** Hit points of pain, 0 to 10: from nothing at the nosebleed edge to ten at the hard cap. */
    public static int damageHp(int overdraw, int cap) {
        int half = cap / 2;
        if (half <= 0 || overdraw <= half) {
            return 0;
        }
        return (int) Math.max(0, Math.min(10, Math.floor(10.0 * (overdraw - half) / half)));
    }
}