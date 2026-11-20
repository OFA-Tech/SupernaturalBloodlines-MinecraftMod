package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R168: the Elixir (Elijah's reward) brings its holder back from any death except a line death, {@code /kill}, a death that is part of becoming something
 * and the edge of the world, and the Resurrection Ring goes first: a death the ring would save is left to the ring, and the Elixir stays in the bag.
 */
public final class ElixirRules {
    /**
     * What killed the holder. {@code TURNING} is a death that is part of becoming something (a hybrid transition, a vampire's turning, Original blood in
     * the veins): the Elixir must not undo it. {@code VOID} is the world's edge: a rescue there only drops them again.
     */
    public enum Kind { LINE_DEATH, KILL_COMMAND, TURNING, VOID, OTHER }

    private ElixirRules() {
    }

    /** The kind of a death, from what is true of it: the first that applies, in this order. The end of an Original by white oak counts as a line death. */
    public static Kind classify(boolean lineDeath, boolean killCommand, boolean originalEnd, boolean turning, boolean outOfWorld) {
        if (lineDeath || originalEnd) {
            return Kind.LINE_DEATH;
        }
        if (killCommand) {
            return Kind.KILL_COMMAND;
        }
        if (turning) {
            return Kind.TURNING;
        }
        return outOfWorld ? Kind.VOID : Kind.OTHER;
    }

    public static boolean saves(Kind kind, boolean ringWouldSave) {
        return kind == Kind.OTHER && !ringWouldSave;
    }

    /** The health the holder comes back with, a fraction of the maximum: never zero, never more than all of it. */
    public static float healTo(float max, double fraction) {
        return (float) Math.max(1.0, Math.min(max, max * fraction));
    }
}
