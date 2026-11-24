package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;

/** What the side panel and the narrator say about the power a stone entry is on: the lang suffix of its click row and its extra rows ({@link StoneMenus#help}). */
public record StoneView(String clickWhat, List<StoneMenus.Help> help) {
    /** An entry that is no stone power: the usual click row and no extras. */
    public static final StoneView NONE = new StoneView("click_use", List.of());
}
