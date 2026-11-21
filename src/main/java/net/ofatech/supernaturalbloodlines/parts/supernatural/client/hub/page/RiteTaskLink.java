package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule;

/** The "Open in the book" link under a ladder task that asks for one of our rites. */
final class RiteTaskLink {
    static final String LABEL = "supernaturalbloodlines.hub.task.open_book";

    private RiteTaskLink() {
    }

    /** Draws the link when the task names a rite; returns the y below it. */
    static int add(HubScreen.Frame f, HubModel.TaskRow row, int x, int y, int w) {
        String rite = RiteLinkRule.riteOf(row.task());
        if (rite == null) {
            return y;
        }
        Component label = Component.translatable(LABEL);
        Rect b = new Rect(x, y + 3, Math.min(w, PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.button(f, "task:book:" + row.task().id(), b, label, RiteLinks.hasBook(), false, RiteLinks.tip(rite, null), () -> RiteLinks.open(rite));
        return b.bottom() + 2;
    }
}
