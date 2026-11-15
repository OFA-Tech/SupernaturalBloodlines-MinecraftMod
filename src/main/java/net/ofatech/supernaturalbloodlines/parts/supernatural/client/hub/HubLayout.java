package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.List;

/** Where a theme puts things: the frame, the two content regions the pages fill, and one tab per visible page, in page order. Pure. */
public record HubLayout(Rect frame, Rect left, Rect right, List<Rect> tabs) {
    public HubLayout {
        tabs = List.copyOf(tabs);
    }
}
