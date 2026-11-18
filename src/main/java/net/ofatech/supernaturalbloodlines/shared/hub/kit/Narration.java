package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;

/**
 * What the screen reader is told about the hub, as plain strings (the screen turns them into narration): the page, then the focused target's name and tooltip;
 * over a confirm dialog, the dialog's title and text and the focused button instead, because nothing behind a dialog can be reached. Pure.
 */
public final class Narration {
    private Narration() {
    }

    /**
     * @param pageTitle     the page the hub shows
     * @param focusTooltip  the lines of the focused target's tooltip (its name first), or null when nothing has the focus
     * @param dialogTitle   the title of the open confirm dialog, or null when there is none
     * @param dialogBody    its text
     * @param dialogFocused the label of the button that has the focus in the dialog
     */
    public static List<String> lines(String pageTitle, List<String> focusTooltip, String dialogTitle, String dialogBody, String dialogFocused) {
        List<String> out = new ArrayList<>();
        if (dialogTitle != null) {
            out.add(dialogTitle);
            if (dialogBody != null && !dialogBody.isBlank()) {
                out.add(dialogBody);
            }
            if (dialogFocused != null && !dialogFocused.isBlank()) {
                out.add(dialogFocused);
            }
            return out;
        }
        out.add(pageTitle);
        if (focusTooltip != null) {
            focusTooltip.stream().filter(s -> s != null && !s.isBlank()).forEach(out::add);
        }
        return out;
    }
}
