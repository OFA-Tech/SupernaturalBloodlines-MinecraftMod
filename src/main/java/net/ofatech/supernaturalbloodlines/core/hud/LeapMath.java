package net.ofatech.supernaturalbloodlines.core.hud;

/** Hybrid Leap (R212): where the leap throws the player, how far it reaches, when it hits and what a landing costs. Pure. */
public final class LeapMath {
    /** The leap lands a blow within this many blocks of the target (centre to centre). */
    public static final double HIT_RADIUS = 2.5;
    /** The least upward push, so a leap always leaves the ground (a flat one would be eaten by friction). */
    public static final double MIN_LIFT = 0.2;
    /** The flight is over after this many ticks, landed or not. */
    public static final int FLIGHT_TICKS = 30;
    /** After a flight ends the fall is eased for this many ticks, so a landing the leap made does not hurt. */
    public static final int FALL_GRACE_TICKS = 20;
    /** The most of a fall that is forgiven (blocks): a leap off a cliff is no way to survive any fall. */
    public static final double FALL_FORGIVEN = 8.0;
    /** Distance covered in the air for each unit of speed with the standard lift (vanilla air drag and gravity, see {@code LeapMathTest}). */
    private static final double TRAVEL_PER_SPEED = 7.0;

    private LeapMath() {
    }

    /**
     * The push: the horizontal direction to the target as a unit vector times {@code speed}, and {@code lift} (never under {@link #MIN_LIFT}) upward.
     * A target right above or below (no horizontal direction) gets a straight up push, never NaN.
     */
    public static double[] impulse(double dx, double dy, double dz, double speed, double lift) {
        double up = Math.max(lift, MIN_LIFT);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-6) {
            return new double[] {0.0, up, 0.0};
        }
        return new double[] {dx / horizontal * speed, up, dz / horizontal * speed};
    }

    /** Horizontal speed that carries the player about as far as the target, a little past it (the blow lands on the way). */
    public static double speedFor(double distance) {
        return Math.max(0.35, Math.min(2.0, distance / TRAVEL_PER_SPEED));
    }

    /** The upward push: the standard 0.5, and more for a target higher up (0.06 a block, at most 0.9 in all). */
    public static double liftFor(double dy) {
        return Math.min(0.9, 0.5 + Math.max(0.0, dy) * 0.06);
    }

    /** The target is within the range (the range itself is in reach). */
    public static boolean inReach(double distance, double maxRange) {
        return distance <= maxRange;
    }

    /** The blow lands: the leap has brought the player within the radius. */
    public static boolean hits(double distanceToTarget, double hitRadius) {
        return distanceToTarget <= hitRadius;
    }

    /** The fall distance that counts after a leap's landing: the first {@link #FALL_FORGIVEN} blocks are free, the rest is a normal fall. */
    public static double fallAfterLeap(double fallDistance) {
        return Math.max(0.0, fallDistance - FALL_FORGIVEN);
    }

    /** Whether the flight is over: it landed (after it had left the ground), or its time ran out. */
    public static boolean flightOver(int ticksInFlight, boolean leftGround, boolean onGround) {
        return ticksInFlight >= FLIGHT_TICKS || leftGround && onGround;
    }
}
