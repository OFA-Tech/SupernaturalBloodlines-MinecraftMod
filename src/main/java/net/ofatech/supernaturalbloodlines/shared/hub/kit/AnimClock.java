package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A clock per element: it remembers the state each id was last seen in and the tick it changed, so a tween runs from the change and a screen that is
 * drawn every frame never restarts one. The first sighting of an id is its resting state, not a change (it does not animate on open). Pure.
 */
public final class AnimClock {
    private record Seen(Object state, double since) {
    }

    private final Map<String, Seen> seen = new HashMap<>();

    /** Records {@code state} for {@code id} at {@code now}; the tick this state began (now, the first time an id is seen: a screen opens at rest). */
    public double since(String id, Object state, double now) {
        Seen s = seen.get(id);
        if (s == null) {
            seen.put(id, new Seen(state, Double.NEGATIVE_INFINITY));
            return Double.NEGATIVE_INFINITY;
        }
        if (!Objects.equals(s.state(), state)) {
            seen.put(id, new Seen(state, now));
            return now;
        }
        return s.since();
    }

    /** The eased progress of the change into {@code state} (1 at rest, or with reduce motion on). */
    public float progress(String id, Object state, double now, int duration, Easing easing, boolean reduce) {
        return Motion.progress(since(id, state, now), now, duration, easing, reduce);
    }

    public void clear() {
        seen.clear();
    }
}
