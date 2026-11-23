package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.jetbrains.annotations.Nullable;

/**
 * Infinity Stones on the wheel (client only, by reflection, his mod unchanged). Each carried stone is one entry whose unlocked powers are its modes; the soul control entry
 * shows while a possession runs. Everything is off, quietly, when his mod is missing, the setting {@code infinity_compat_enabled} is off, or his classes are not the shape we
 * verified (one warning, then off for the session). His server validates every call; we only choose which call to make.
 */
public final class InfinityCompat {
    static final String MOD = "infinity";
    private static final InfinityProbe PROBE = new InfinityProbe(() -> ModList.get().isLoaded(MOD), HudClientConfig::infinityCompat,
            () -> ReflectiveStoneApi.create(Class::forName), m -> SupernaturalBloodlines.LOGGER.warn(m));
    /** The power each stone was last on (stone ordinal to node id); his server takes the node explicitly, so the choice lives only here. */
    private static final Map<Integer, String> CURRENT = new HashMap<>();
    private static long joinedAt;

    private InfinityCompat() {
    }

    public static State state() {
        return PROBE.state();
    }

    /** A world was joined: a state received before this moment is another world's. */
    public static void joined() {
        joinedAt = System.currentTimeMillis();
        CURRENT.clear();
    }

    /** Whether his soul-control key may be covered: only while the wheel can do its job. */
    public static boolean soulControlCovered() {
        return PROBE.soulControlCovered();
    }

    /** The plain Tensura entries of namespace {@code infinity} are not shown while the compat works (the stones come from {@link #entries()}). */
    public static boolean hidesTensura(String namespace) {
        return StoneRules.hidesTensuraEntry(state(), namespace);
    }

    public static List<WheelEntry> entries() {
        StoneApi api = PROBE.api();
        return api == null ? List.of() : StoneEntries.build(api, CURRENT, joinedAt);
    }

    /** Whether an entry is one of ours while the compat works, so the wheel routes it here and never to our own server (which refuses it). */
    public static boolean owns(WheelEntry e) {
        return e.ref().source() == PowerSource.INFINITY && PROBE.state() == State.ACTIVE;
    }

    /** The description of the power an entry is on now, for the side panel. */
    public static String description(WheelEntry e) {
        StoneApi api = PROBE.api();
        NodeInfo n = api == null ? null : StoneEntries.nodeOf(api, e);
        return n == null ? "" : n.description();
    }

    public static boolean hasLevels(WheelEntry e) {
        StoneApi api = PROBE.api();
        NodeInfo n = api == null ? null : StoneEntries.nodeOf(api, e);
        return n != null && n.levels() != null;
    }

    public static boolean use(WheelEntry e, boolean second) {
        StoneApi api = PROBE.api();
        return api != null && StoneActions.use(api, CURRENT, e, second);
    }

    public static void cycle(WheelEntry e, int delta) {
        StoneApi api = PROBE.api();
        if (api != null) {
            StoneActions.cycle(api, CURRENT, e, delta);
        }
    }

    public static void level(WheelEntry e, int delta) {
        StoneApi api = PROBE.api();
        if (api != null) {
            StoneActions.level(api, e, delta);
        }
    }

    /** For the aim: the entry of a stone looked up again by reference (its power may have changed while aiming). */
    @Nullable
    public static WheelEntry find(PowerRef ref) {
        for (WheelEntry e : entries()) {
            if (e.ref().equals(ref)) {
                return e;
            }
        }
        return null;
    }
}
