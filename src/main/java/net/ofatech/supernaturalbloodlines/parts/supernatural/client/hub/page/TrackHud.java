package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * "Track on HUD": which task the ability HUD shows a line for. Kept on the client for the session: it is forgotten when the player logs out, when the task
 * is claimed, and when the task has been gone from the hub's model for {@link #GRACE_TICKS} (a sync gap of a moment does not drop it; the last line stays
 * on screen meanwhile). The button works only when the HUD part is installed ({@link TrackerAccess#available()}); otherwise it stays a disabled link that
 * says so.
 */
public final class TrackHud {
    /** How long a tracked task may be missing from the model before it is forgotten: five seconds. */
    public static final int GRACE_TICKS = 100;
    private static volatile String tracked;
    private static long builtAt = Long.MIN_VALUE;
    private static long missingSince = Long.MIN_VALUE;
    private static Optional<TrackerAccess.Line> cached = Optional.empty();

    private TrackHud() {
    }

    public static void register() {
        TrackerAccess.installHub(TrackHud::line, () -> tracked, id -> {
            forget();
            tracked = id;
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> forget());
    }

    /** Forgets the tracked task and its line. */
    static void forget() {
        tracked = null;
        builtAt = Long.MIN_VALUE;
        missingSince = Long.MIN_VALUE;
        cached = Optional.empty();
    }

    /** Whether a task missing since {@code missingSince} has been missing for too long at {@code now}. */
    static boolean expired(long missingSince, long now) {
        return missingSince != Long.MIN_VALUE && now - missingSince > GRACE_TICKS;
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
        long tick = mc.level.getGameTime();
        if (tick / 20 == builtAt) {
            return cached;
        }
        builtAt = tick / 20;
        for (HubModel.LadderView ladder : HubView.of(mc.player).ladders()) {
            for (HubModel.TaskRow row : ladder.tasks()) {
                if (!row.task().id().equals(id)) {
                    continue;
                }
                if (row.state() == TaskState.CLAIMED) {
                    forget();
                    return Optional.empty();
                }
                missingSince = Long.MIN_VALUE;
                long done = row.counted() ? row.progress() : row.state() == TaskState.OPEN ? 0 : 1;
                cached = Optional.of(new TrackerAccess.Line(PageKit.text(row.text()).getString(), done, row.counted() ? row.target() : 1));
                return cached;
            }
        }
        if (missingSince == Long.MIN_VALUE) {
            missingSince = tick;
        }
        if (expired(missingSince, tick)) {
            forget();
            return Optional.empty();
        }
        return cached;
    }
}
