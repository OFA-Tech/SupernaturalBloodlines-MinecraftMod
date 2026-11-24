package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.jetbrains.annotations.Nullable;

/** What a click on a stone entry sends to Infinity's own server, on top of a {@link StoneApi} (the tests give it a fake). */
public final class StoneActions {
    private StoneActions() {
    }

    /**
     * Uses what the entry stands for: the soul control entry sends his possession action, a stone sends his {@code Use} for its current power (phase 1 stops a running
     * channel, 2 is the second option of a dual power, 0 the plain use). Returns false when nothing was sent.
     */
    public static boolean use(StoneApi api, Map<Integer, String> current, WheelEntry e, boolean second) {
        if (e.ref().id().equals(StoneEntries.SOUL_CONTROL)) {
            int action = StoneRules.soulAction(api.soulMode());
            if (action < 0) {
                return false;
            }
            api.soulAction(action);
            return true;
        }
        NodeInfo n = StoneEntries.nodeOf(api, e);
        if (n == null) {
            return false;
        }
        boolean holdingThis = n.modeId().equals(api.holding());
        api.use(n.stone(), n.node(), StoneRules.phase(n.isSwitch(), n.isChannel(), holdingThis, second, n.isDual()));
        current.put(n.stone(), n.node());
        return true;
    }

    /** Alt + click: the next (or previous) unlocked power of the stone, round the list; locked powers are not in it. */
    public static void cycle(StoneApi api, Map<Integer, String> current, WheelEntry e, int delta) {
        NodeInfo n = StoneEntries.nodeOf(api, e);
        if (n == null) {
            return;
        }
        List<NodeInfo> open = new ArrayList<>();
        for (NodeInfo x : api.nodes()) {
            if (x.stone() == n.stone() && x.unlocked()) {
                open.add(x);
            }
        }
        if (open.size() < 2) {
            return;
        }
        int at = StoneEntries.indexOf(open, n.node());
        current.put(n.stone(), open.get(Math.floorMod(at + (delta < 0 ? -1 : 1), open.size())).node());
    }

    /** Scroll over a power with levels: his {@code Setting} with +1 or -1; nothing for a power without. */
    public static void level(StoneApi api, WheelEntry e, int delta) {
        NodeInfo n = StoneEntries.nodeOf(api, e);
        if (n == null || n.levels() == null || delta == 0) {
            return;
        }
        api.setLevel(n.stone(), n.node(), delta > 0 ? 1 : -1);
    }

    @Nullable
    public static NodeInfo node(StoneApi api, WheelEntry e) {
        return StoneEntries.nodeOf(api, e);
    }
}
