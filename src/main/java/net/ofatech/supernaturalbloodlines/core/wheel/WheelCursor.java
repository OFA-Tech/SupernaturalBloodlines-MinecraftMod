package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Hit;

/**
 * What the wheel treats as "the one under the pointer": the mouse sets it only when the mouse moves or clicks, a key that moves the focus clears it (so a mouse that is just
 * resting where it was cannot take the focus back each frame), and the focus is what is used while there is no hover. Pure.
 */
public final class WheelCursor {
    private Hit hover;
    private Hit focus;

    public void mouseMoved(Hit over) {
        hover = over;
    }

    /** A key moved the focus: the mouse no longer decides until it moves again. */
    public void keyFocus(Hit to) {
        focus = to;
        hover = null;
    }

    public Optional<Hit> active() {
        return Optional.ofNullable(hover != null ? hover : focus);
    }

    public Optional<Hit> hover() {
        return Optional.ofNullable(hover);
    }

    public Optional<Hit> focus() {
        return Optional.ofNullable(focus);
    }

    /**
     * The list or the layout changed under a resting pointer: the cell it was over may be another power now (or none). Only a hover that the mouse set is looked at again; a
     * focus that a key set is never taken back by a pointer that has not moved.
     */
    public void rehit(Hit under) {
        if (hover != null) {
            hover = under;
        }
    }

    /** The page or the open path changed under the pointer: the old cell is not the one it is over any more. */
    public void clearHover() {
        hover = null;
    }
}
