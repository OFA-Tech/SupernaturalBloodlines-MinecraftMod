package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * What every task card has in common, whichever theme lays it out: the border that says its state, how far along it is and the count beside the bar, and its
 * target and focus ring. The Blood Moon's path card and the Quests cards of the Blood Moon and Tensura differ only in where the pieces go.
 */
final class TaskCard {
    private TaskCard() {
    }

    /** Green when it can be claimed, the accent when it is the chosen one, else the quiet muted line. */
    static int border(HubTheme t, HubModel.TaskRow row, boolean chosen) {
        return row.state() == TaskState.CLAIMABLE ? t.good() : chosen ? t.accent() : (t.muted() & 0xFFFFFF) | 0x66000000;
    }

    /** The line inside a card's plate: green when it can be claimed, the accent when it is the chosen one, none ({@code 0}) otherwise. */
    static int edge(TaskState state, boolean chosen, int good, int accent) {
        return state == TaskState.CLAIMABLE ? good : chosen ? accent : 0;
    }

    /** 0 to 1: the progress of a counted task, else done or not. */
    static float fraction(HubModel.TaskRow row) {
        boolean done = row.state() == TaskState.CLAIMED || row.state() == TaskState.CLAIMABLE;
        return row.counted() && row.target() > 0 ? Math.min(1f, row.progress() / (float) row.target()) : done ? 1f : 0f;
    }

    /** "3/5" for a counted task, a tick for one that is done, else nothing. */
    static String count(HubModel.TaskRow row) {
        boolean done = row.state() == TaskState.CLAIMED || row.state() == TaskState.CLAIMABLE;
        return row.counted() && row.target() > 1 ? row.progress() + "/" + row.target() : done ? "\u2713" : "";
    }

    /** Registers the card as the target that picks its task, with the tooltip and the focus ring. */
    static void target(HubScreen.Frame f, HubModel.TaskRow row, Rect box) {
        String id = row.task().id();
        f.target("task:" + id, box, PageKit.tooltip(row), () -> f.select(id));
        if (f.focused("task:" + id)) {
            KitDraw.focusRing(f.g, box, f.theme().focusColor());
        }
    }
}
