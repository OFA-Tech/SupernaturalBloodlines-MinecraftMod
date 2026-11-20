package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * The state of a choice dialog: which of the {@code choices} buttons, or the decline button after them, has the focus, and how far the list is scrolled.
 * The decline button has the focus first, so Enter alone never swears an oath (like {@link ConfirmModel}'s safe button); Esc only closes. Pure.
 */
public final class ChoiceModel {
    public enum Key {
        UP, DOWN, TAB, ENTER, ESCAPE
    }

    public enum Type {
        NONE, PICK, DECLINE, CLOSE
    }

    public record Answer(Type type, int index) {
        static final Answer NONE = new Answer(Type.NONE, -1);
    }

    private final int choices;
    private final int rows;
    private int focus;
    private int scroll;

    public ChoiceModel(int choices, int rows) {
        this.choices = Math.max(0, choices);
        this.rows = Math.max(1, rows);
        this.focus = this.choices;
    }

    /** 0 to choices - 1 for a choice, {@code choices} for the decline button. */
    public int focus() {
        return focus;
    }

    public int scroll() {
        return scroll;
    }

    public boolean scrolls() {
        return choices > rows;
    }

    public Answer press(Key key) {
        switch (key) {
            case ESCAPE -> {
                return new Answer(Type.CLOSE, -1);
            }
            case ENTER -> {
                return focus >= choices ? new Answer(Type.DECLINE, -1) : new Answer(Type.PICK, focus);
            }
            case UP -> focus = Math.max(0, focus - 1);
            case DOWN -> focus = Math.min(choices, focus + 1);
            case TAB -> focus = focus >= choices ? 0 : focus + 1;
        }
        followFocus();
        return Answer.NONE;
    }

    /** The wheel: {@code direction} is +1 (down) or -1 (up); it moves the list, not the focus. */
    public void wheel(int direction) {
        scroll = Math.max(0, Math.min(Math.max(0, choices - rows), scroll + Integer.signum(direction)));
    }

    /** A click on the row {@code row} of the visible list (0 is the first one shown), or on the decline button when {@code row} is -1. */
    public Answer click(int row) {
        if (row < 0) {
            focus = choices;
            return new Answer(Type.DECLINE, -1);
        }
        int index = scroll + row;
        if (index >= choices || row >= rows) {
            return Answer.NONE;
        }
        focus = index;
        return new Answer(Type.PICK, index);
    }

    private void followFocus() {
        if (focus >= choices) {
            return;
        }
        if (focus < scroll) {
            scroll = focus;
        } else if (focus >= scroll + rows) {
            scroll = focus - rows + 1;
        }
    }
}
