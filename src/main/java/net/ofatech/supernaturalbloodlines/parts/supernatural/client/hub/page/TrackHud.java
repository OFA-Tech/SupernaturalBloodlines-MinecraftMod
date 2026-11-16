package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * "Track on HUD": which task the ability HUD shows a line for. Kept on the client for the session (a task that is claimed or gone stops showing and is
 * forgotten). The button works only when the HUD part is installed ({@link TrackerAccess#available()}); otherwise it stays a disabled link that says so.
 */
public final class TrackHud {
    private static volatile String tracked;
    private static long builtAt = Long.MIN_VALUE;
    private static Optional<TrackerAccess.Line> cached = Optional.empty();

    private TrackHud() {
    }

    public static void register() {
        TrackerAccess.installHub(TrackHud::line, () -> tracked, id -> {
            tracked = id;
            builtAt = Long.MIN_VALUE;
        });
    }

    /** The "Track on HUD" (or "Stop tracking") action for a task of the hub, for any theme's detail. */
    public static Detail.Action action(String taskId, boolean claimed) {
        boolean on = TrackerAccess.available();
        boolean now = on && TrackerAccess.isTracked(taskId);
        Component label = Component.translatable(PageKit.HUB + (now ? "path.untrack" : "path.track"));
        Component why = on ? claimed ? Component.translatable(PageKit.HUB + "path.track_done") : null : Component.translatable(PageKit.HUB + "path.track_needs_hud");
        return new Detail.Action("track", label, false, on && !claimed, why, null, () -> TrackerAccess.toggle(taskId));
    }

    /** The tracked task's line from the hub's own model, at most once a second. */
    private static Optional<TrackerAccess.Line> line() {
        Minecraft mc = Minecraft.getInstance();
        String id = tracked;
        if (id == null || mc.player == null || mc.level == null) {
            return Optional.empty();
        }
        long now = mc.level.getGameTime() / 20;
        if (now == builtAt) {
            return cached;
        }
        builtAt = now;
        cached = Optional.empty();
        for (HubModel.LadderView ladder : HubView.of(mc.player).ladders()) {
            for (HubModel.TaskRow row : ladder.tasks()) {
                if (row.task().id().equals(id)) {
                    if (row.state() == net.ofatech.supernaturalbloodlines.shared.hub.TaskState.CLAIMED) {
                        tracked = null;
                        return cached;
                    }
                    cached = Optional.of(new TrackerAccess.Line(PageKit.text(row.text()).getString(), row.counted() ? row.progress() : row.state() == net.ofatech.supernaturalbloodlines.shared.hub.TaskState.OPEN ? 0 : 1, row.counted() ? row.target() : 1));
                    return cached;
                }
            }
        }
        return cached;
    }
}
