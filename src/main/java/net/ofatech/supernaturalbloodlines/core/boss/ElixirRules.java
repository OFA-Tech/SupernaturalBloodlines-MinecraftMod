package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R168: the Elixir (Elijah's reward) brings its holder back from any death except a line death and {@code /kill}, once, and the Resurrection Ring goes
 * first: a death the ring would save is left to the ring, and the Elixir stays in the bag.
 */
public final class ElixirRules {
    /** What killed the holder. */
    public enum Kind { LINE_DEATH, KILL_COMMAND, OTHER }

    private ElixirRules() {
    }

    public static boolean saves(Kind kind, boolean ringWouldSave) {
        return kind == Kind.OTHER && !ringWouldSave;
    }

    /** The health the holder comes back with, a fraction of the maximum: never zero, never more than all of it. */
    public static float healTo(float max, double fraction) {
        return (float) Math.max(1.0, Math.min(max, max * fraction));
    }
}