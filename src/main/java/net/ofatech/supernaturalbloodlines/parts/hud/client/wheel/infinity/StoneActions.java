package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.jetbrains.annotations.Nullable;

/** What a click on a stone entry sends to Infinity's own server, on top of a {@link StoneApi} (the tests give it a fake). */
public final class StoneActions {
    private StoneActions() {
    }

    /**
     * Uses what the entry stands for: the soul control entry sends his possession action, Pray sends his prayer (only while it is shown), a stone sends his {@code Use} for its current power (phase 1 stops a running
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
        if (e.ref().id().equals(StoneEntries.PRAY)) {
            return api.prayShown() && api.pray();
        }
        NodeInfo n = StoneEntries.nodeOf(api, e);
        if (n == null) {
            return false;
        }
        send(api, current, n, second);
        return true;
    }

    private static void send(StoneApi api, Map<Integer, String> current, NodeInfo n, boolean second) {
        boolean holdingThis = n.modeId().equals(api.holding());
        api.use(n.stone(), n.node(), StoneRules.phase(n.isSwitch(), n.isChannel(), holdingThis, second, n.isDual()));
        current.put(n.stone(), n.node());
    }

    /** The power of {@code stoneKey} named {@code nodeId}, used as its stone's power: what an aim fires, whatever the stone is on by the time it is confirmed. */
    public static boolean useNode(StoneApi api, Map<Integer, String> current, String stoneKey, @Nullable String nodeId, boolean second) {
        for (NodeInfo n : api.nodes()) {
            if (n.stoneKey().equals(stoneKey) && n.node().equals(nodeId) && n.unlocked()) {
                send(api, current, n, second);
                return true;
            }
        }
        return false;
    }

    /** What an input does on the entry's current power (his rules, {@link StoneMenus#route}); a plain use for anything that is not a stone power. */
    public static StoneMenus.Act route(StoneApi api, WheelEntry e, StoneMenus.Input input) {
        NodeInfo n = StoneEntries.nodeOf(api, e);
        return n == null ? (input == StoneMenus.Input.MENU ? StoneMenus.Act.NOTHING : StoneMenus.Act.PLAIN) : StoneMenus.route(n.menu(), n.menuOnLeft(), n.isDual(), input);
    }

    /** Carries out a menu act: opens his screen (the caller has nothing else to do, the screen replaced the wheel) or sends his request. False for a plain use or nothing. */
    public static boolean act(StoneApi api, Map<Integer, String> current, WheelEntry e, StoneMenus.Act act) {
        NodeInfo n = StoneEntries.nodeOf(api, e);
        if (n == null) {
            return false;
        }
        switch (act) {
            case OPEN_DESTINATION -> {
                return api.openDestination(n.stone(), n.node(), n.menu() == StoneMenus.Kind.DESTINATION_TARGET);
            }
            case OPEN_CONSTRUCT -> {
                return api.openConstruct();
            }
            case OPEN_QUICK_SLOTS -> {
                return api.openQuickSlots();
            }
            case OPEN_CHOICE -> {
                return api.requestChoiceMenu(n.stone(), n.node());
            }
            case SECOND_AT_ONCE -> send(api, current, n, true);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** What the panel and the narrator say about the entry's current power, from one read of it. */
    public static StoneView view(StoneApi api, WheelEntry e) {
        NodeInfo n = StoneEntries.nodeOf(api, e);
        return n == null ? StoneView.NONE : new StoneView(StoneMenus.clickWhat(n.menu(), n.menuOnLeft()), StoneMenus.help(n.menu(), n.menuOnLeft(), n.isDual()));
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
