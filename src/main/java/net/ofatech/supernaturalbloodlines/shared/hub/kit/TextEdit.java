package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * The text of one field being typed into: a caret, insert, delete and the moves, one line or several (Enter adds a line). The page keeps the
 * original, so Esc reverts and Enter commits. Pure; the widgets draw it and pass keys to it.
 */
public final class TextEdit {
    private final boolean multiline;
    private final int max;
    private String text;
    private int caret;

    public TextEdit(String text, boolean multiline, int max) {
        this.multiline = multiline;
        this.max = max;
        this.text = text;
        this.caret = text.length();
    }

    public String text() {
        return text;
    }

    public int caret() {
        return caret;
    }

    public boolean multiline() {
        return multiline;
    }

    public void type(char c) {
        if (c < 32 || c == 127 || text.length() >= max) {
            return;
        }
        text = text.substring(0, caret) + c + text.substring(caret);
        caret++;
    }

    public void newline() {
        if (multiline && text.length() < max) {
            text = text.substring(0, caret) + '\n' + text.substring(caret);
            caret++;
        }
    }

    public void backspace() {
        if (caret > 0) {
            text = text.substring(0, caret - 1) + text.substring(caret);
            caret--;
        }
    }

    public void delete() {
        if (caret < text.length()) {
            text = text.substring(0, caret) + text.substring(caret + 1);
        }
    }

    public void left() {
        caret = Math.max(0, caret - 1);
    }

    public void right() {
        caret = Math.min(text.length(), caret + 1);
    }

    public void home() {
        caret = text.lastIndexOf('\n', caret - 1) + 1;
    }

    public void end() {
        int n = text.indexOf('\n', caret);
        caret = n < 0 ? text.length() : n;
    }
}
