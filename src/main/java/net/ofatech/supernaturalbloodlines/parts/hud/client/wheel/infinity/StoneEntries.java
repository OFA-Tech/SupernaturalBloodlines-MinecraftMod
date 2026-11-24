package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.jetbrains.annotations.Nullable;

/**
 * One wheel entry per carried stone, its unlocked powers are its modes (his server takes the node explicitly, so which one is current is only remembered here), and the
 * soul control entry while a possession runs. Pure on top of a {@link StoneApi}: the tests give it a fake.
 */
public final class StoneEntries {
    static final String MOD = "infinity";
    public static final ResourceLocation SOUL_CONTROL = ResourceLocation.fromNamespaceAndPath(MOD, "soul_control");
    private static final int SOUL_STONE = 4;

    private StoneEntries() {
    }

    public static ResourceLocation stoneId(String key) {
        return ResourceLocation.fromNamespaceAndPath(MOD, key + "_stone");
    }

    /**
     * @param current  the node id each stone was last on (by stone ordinal); a node that is not unlocked any more falls back to the first one
     * @param joinedAt when this world was joined (ms): a state received before it is another world's and nothing is usable
     */
    public static List<WheelEntry> build(StoneApi api, Map<Integer, String> current, long joinedAt) {
        Map<Integer, List<NodeInfo>> byStone = new LinkedHashMap<>();
        for (NodeInfo n : api.nodes()) {
            byStone.computeIfAbsent(n.stone(), k -> new ArrayList<>()).add(n);
        }
        boolean stale = StoneRules.stale(api.receivedAt(), joinedAt);
        String holding = api.holding();
        WheelEntry soul = soulEntry(api);
        List<WheelEntry> out = new ArrayList<>();
        boolean soulAdded = false;
        for (Map.Entry<Integer, List<NodeInfo>> e : byStone.entrySet()) {
            out.add(entry(e.getValue(), current.get(e.getKey()), holding, stale));
            if (e.getKey() == SOUL_STONE && soul != null) {
                out.add(soul);
                soulAdded = true;
            }
        }
        if (!soulAdded && soul != null) {
            out.add(soul);
        }
        return out;
    }

    @Nullable
    private static WheelEntry soulEntry(StoneApi api) {
        if (!StoneRules.soulEntryShown(StoneRules.State.ACTIVE, api.soulSupported(), api.soulMode())) {
            return null;
        }
        return new WheelEntry(new PowerRef(PowerSource.INFINITY, SOUL_CONTROL), PowerGroup.MAIN, PowerKind.INSTANT, Component.translatable("supernaturalbloodlines.wheel.infinity.soul_control"),
                Component.empty(), List.of(), icon("soul"), PowerState.READY, false, -1f);
    }

    private static ResourceLocation icon(String key) {
        return ResourceLocation.fromNamespaceAndPath(MOD, "textures/item/" + key + "_stone.png");
    }

    /** The unlocked powers of a stone, in his order. */
    public static List<NodeInfo> unlocked(List<NodeInfo> nodes) {
        List<NodeInfo> out = new ArrayList<>();
        for (NodeInfo n : nodes) {
            if (n.unlocked()) {
                out.add(n);
            }
        }
        return out;
    }

    static int indexOf(List<NodeInfo> unlocked, @Nullable String nodeId) {
        for (int i = 0; i < unlocked.size(); i++) {
            if (unlocked.get(i).node().equals(nodeId)) {
                return i;
            }
        }
        return 0;
    }

    private static WheelEntry entry(List<NodeInfo> nodes, @Nullable String currentNode, String holding, boolean stale) {
        NodeInfo first = nodes.get(0);
        List<NodeInfo> open = unlocked(nodes);
        PowerRef ref = new PowerRef(PowerSource.INFINITY, stoneId(first.stoneKey()));
        Component name = Component.translatableWithFallback("supernaturalbloodlines.wheel.infinity.stone." + first.stoneKey(), capital(first.stoneKey()) + " Stone");
        if (open.isEmpty()) {
            return new WheelEntry(ref, PowerGroup.MAIN, PowerKind.INSTANT, name, Component.empty(), List.of(), icon(first.stoneKey()),
                    PowerState.READY.withLock(LockReason.NOT_UNLOCKED), false, -1f);
        }
        int mode = indexOf(open, currentNode);
        NodeInfo node = open.get(mode);
        List<Component> modeNames = new ArrayList<>();
        for (NodeInfo n : open) {
            modeNames.add(Component.literal(n.name()));
        }
        Component modeName = Component.literal(node.name() + (node.levels() == null ? "" : " " + node.setting() + "/" + node.levels().max()));
        boolean holdingThis = node.modeId().equals(holding);
        PowerKind kind = StoneRules.kind(node.isSwitch(), node.isChannel(), node.needsAim(), holdingThis);
        LockReason lock = stale ? LockReason.REFUSED_NOW : LockReason.NONE;
        PowerState state = new PowerState(node.toggled() || holdingThis, 0L, 0, lock, mode, open.size());
        return new WheelEntry(ref, PowerGroup.MAIN, kind, name, modeName, modeNames, icon(first.stoneKey()), state, false, -1f);
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The power an entry of a stone stands for now: its unlocked node at the entry's mode; null for the soul control entry or a stone with nothing unlocked. */
    @Nullable
    public static NodeInfo nodeOf(StoneApi api, WheelEntry e) {
        String id = e.ref().id().getPath();
        if (!id.endsWith("_stone")) {
            return null;
        }
        String key = id.substring(0, id.length() - "_stone".length());
        List<NodeInfo> open = new ArrayList<>();
        for (NodeInfo n : api.nodes()) {
            if (n.stoneKey().equals(key) && n.unlocked()) {
                open.add(n);
            }
        }
        int mode = e.state().mode();
        return mode >= 0 && mode < open.size() ? open.get(mode) : null;
    }
}
