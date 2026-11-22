package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** The 1 px line over a text box: the warning colour for a value that was refused, the accent for a box being typed in, none otherwise. Pure. */
public final class FieldEdge {
    private FieldEdge() {
    }

    /** @return the line's colour, or 0 for no line; a refused value wins over a box being typed in, since the person has to fix it */
    public static int colour(boolean editing, boolean invalid, int warn, int accent) {
        return invalid ? warn : editing ? accent : 0;
    }
}