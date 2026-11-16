package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * The state of a confirm dialog. The safe button (cancel) has the focus first, so Enter alone never confirms; Esc always cancels. Pure.
 */
public final class ConfirmModel {
    public enum Key {
        ESCAPE, ENTER, TAB, LEFT, RIGHT
    }

    public enum Result {
        NONE, CONFIRM, CANCEL
    }

    private boolean confirmFocused;

    public boolean confirmFocused() {
        return confirmFocused;
    }

    public Result press(Key key) {
        switch (key) {
            case ESCAPE -> {
                return Result.CANCEL;
            }
            case ENTER -> {
                return confirmFocused ? Result.CONFIRM : Result.CANCEL;
            }
            case TAB -> confirmFocused = !confirmFocused;
            case LEFT -> confirmFocused = false;
            case RIGHT -> confirmFocused = true;
        }
        return Result.NONE;
    }

    /** A click: on the confirm button confirms, anywhere else (the cancel button, outside the dialog) cancels. */
    public static Result click(boolean onConfirm) {
        return onConfirm ? Result.CONFIRM : Result.CANCEL;
    }
}
