package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import org.jetbrains.annotations.Nullable;

/**
 * {@link StoneApi} read from Infinity Stones' classes by reflection (no build dependency, his mod unchanged). Everything is resolved once in {@link #create}; one class or
 * method that is not the shape verified against 8.0.0-alpha.6 makes {@code create} throw {@link Missing} naming what, and the compat stays off. The soul part is resolved
 * separately: if it is missing the stones still work and only the soul control entry (and its covered key) is left out.
 */
public final class ReflectiveStoneApi implements StoneApi {
    /** Finds a class by name; the real one is {@code Class.forName}, the tests give a map. */
    @FunctionalInterface
    public interface ClassFinder {
        Class<?> find(String name) throws ClassNotFoundException;
    }

    /** A class or method of his that was not found as expected. */
    public static final class Missing extends Exception {
        public Missing(String what, Throwable cause) {
            super(what, cause);
        }

        public Missing(String what) {
            super(what);
        }
    }

    private static final String ROOT = "com.infinitystones.tensura.";

    private final Method wheelState;
    private final Method receivedAt;
    private final Method stateEntries;
    private final Method stateHolding;
    private final Method entryStone;
    private final Method entryNode;
    private final Method entryUnlocked;
    private final Method entryToggled;
    private final Method entrySetting;
    private final Method stoneValues;
    private final Field stoneKey;
    private final Method treeOf;
    private final Field treeNodes;
    private final Method nodeDisplayName;
    private final Method nodeDisplayDescription;
    private final Method nodeModeId;
    private final Method needsAim;
    private final Method isChannel;
    private final Method isSwitch;
    private final Method setting;
    private final Method settingMin;
    private final Method settingMax;
    private final Method settingStep;
    private final Method isDual;
    private final Method stoneColor;
    private final Method hasDestination;
    private final Method movesTarget;
    private final Method menuOnLeft;
    private final Method hasChoiceMenu;
    private final Method requestMenu;
    private final Constructor<?> useCtor;
    private final Constructor<?> settingCtor;
    private final Constructor<?> destinationCtor;
    private final Constructor<?> constructCtor;
    private final Constructor<?> quickSlotsCtor;
    private final Method isHeldBy;
    private final Constructor<?> prayCtor;
    @Nullable
    private final Method gatherLocked;
    private final StoneFailures failures;
    @Nullable
    private final Field soulState;
    @Nullable
    private final Method soulMode;
    @Nullable
    private final Constructor<?> soulActionCtor;
    private final Map<String, NodeFacts> facts = new HashMap<>();
    private Object[] stones;
    /** His state is replaced, never changed, so the list read from it is good until the object is another one (or the language changes). */
    private Object seenState;
    private List<NodeInfo> seenNodes = List.of();

    /** What a node is, which never changes while the game runs (his trees are read from the jar) but for the language of its name and description. */
    private record NodeFacts(String name, String description, boolean isSwitch, boolean isChannel, boolean needsAim, boolean isDual, @Nullable NodeInfo.Levels levels,
                             StoneMenus.Kind menu, boolean menuOnLeft) {
    }

    private ReflectiveStoneApi(ClassFinder f, StoneFailures failures) throws Missing {
        this.failures = failures;
        Class<?> wheelClientState = cls(f, "wheel.WheelClientState");
        Class<?> payloadState = cls(f, "wheel.WheelPayloads$State");
        Class<?> payloadEntry = cls(f, "wheel.WheelPayloads$Entry");
        Class<?> stone = cls(f, "skill.Stone");
        Class<?> tree = cls(f, "skill.StoneTree");
        Class<?> node = cls(f, "skill.StoneTree$Node");
        Class<?> meta = cls(f, "wheel.AbilityMeta");
        Class<?> spec = cls(f, "wheel.AbilityMeta$SettingSpec");
        Class<?> dual = cls(f, "skill.DualModes");
        Class<?> use = cls(f, "wheel.WheelPayloads$Use");
        Class<?> settingPayload = cls(f, "wheel.WheelPayloads$Setting");
        wheelState = method(wheelClientState, "get");
        receivedAt = method(wheelClientState, "receivedAt");
        stateEntries = method(payloadState, "entries");
        stateHolding = method(payloadState, "holding");
        entryStone = method(payloadEntry, "stone");
        entryNode = method(payloadEntry, "node");
        entryUnlocked = method(payloadEntry, "unlocked");
        entryToggled = method(payloadEntry, "toggled");
        entrySetting = method(payloadEntry, "setting");
        stoneValues = method(stone, "values");
        stoneKey = field(stone, "key");
        treeOf = method(tree, "of", stone);
        treeNodes = field(tree, "nodes");
        nodeDisplayName = method(node, "displayName");
        nodeDisplayDescription = method(node, "displayDescription");
        nodeModeId = method(node, "modeId");
        needsAim = method(meta, "needsAim", node);
        isChannel = method(meta, "isChannel", node);
        isSwitch = method(meta, "isSwitch", node);
        setting = method(meta, "setting", node);
        settingMin = method(spec, "min");
        settingMax = method(spec, "max");
        settingStep = method(spec, "step");
        isDual = method(dual, "isDual", String.class);
        Class<?> extraClient = cls(f, "skill.ability.extra.client.ExtraClientApi");
        Class<?> destination = cls(f, "client.wheel.DestinationScreen");
        Class<?> construct = cls(f, "client.will.ConstructScreen");
        Class<?> quickSlots = cls(f, "client.will.QuickSlotsScreen");
        stoneColor = method(stone, "color");
        hasDestination = method(dual, "hasDestination", String.class);
        movesTarget = method(dual, "movesTarget", String.class);
        menuOnLeft = method(dual, "menuOnLeft", String.class);
        hasChoiceMenu = method(extraClient, "hasMenu", String.class);
        requestMenu = method(extraClient, "requestMenu", int.class, String.class);
        useCtor = ctor(use, int.class, String.class, int.class);
        settingCtor = ctor(settingPayload, int.class, String.class, int.class);
        destinationCtor = ctor(destination, int.class, String.class, boolean.class, int.class);
        constructCtor = ctor(construct);
        quickSlotsCtor = ctor(quickSlots);
        // his wheel key prays when no stone is carried: his GatherClient.onWheelKey, whose condition and packet this repeats
        isHeldBy = method(stone, "isHeldBy", Player.class);
        prayCtor = ctor(cls(f, "skill.ability.extra.space.GatherNet$Pray"));
        Method lockedProbe = null;
        try {
            // package-private in his class: read only to match his condition, and treated as "not locked" when it is not there
            lockedProbe = cls(f, "skill.ability.extra.space.client.GatherClient").getDeclaredMethod("isLocked");
            lockedProbe.setAccessible(true);
        } catch (Missing | ReflectiveOperationException | RuntimeException | LinkageError none) {
            lockedProbe = null;
        }
        gatherLocked = lockedProbe;
        Field state = null;
        Method mode = null;
        Constructor<?> action = null;
        try {
            Class<?> soulNet = cls(f, "soul.SoulNet");
            Class<?> soulStateClass = cls(f, "soul.SoulNet$State");
            Class<?> soulAction = cls(f, "soul.SoulNet$Action");
            cls(f, "soul.Possession");
            state = field(soulNet, "state");
            mode = method(soulStateClass, "mode");
            action = ctor(soulAction, int.class, int.class);
        } catch (Missing soulMissing) {
            state = null;
            mode = null;
            action = null;
        }
        soulState = state;
        soulMode = mode;
        soulActionCtor = action;
    }

    /** Resolves everything once; throws {@link Missing} naming the first thing that is not as expected. */
    public static ReflectiveStoneApi create(ClassFinder f) throws Missing {
        return new ReflectiveStoneApi(f, StoneFailures.NONE);
    }

    /** As {@link #create(ClassFinder)}, with where run-time failures go: an action that fails is logged once, a read of his powers that fails turns the compat off. */
    public static ReflectiveStoneApi create(ClassFinder f, StoneFailures failures) throws Missing {
        return new ReflectiveStoneApi(f, failures);
    }

    private static Class<?> cls(ClassFinder f, String name) throws Missing {
        try {
            return f.find(ROOT + name);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new Missing("class " + name, e);
        }
    }

    private static Method method(Class<?> c, String name, Class<?>... args) throws Missing {
        try {
            return c.getMethod(name, args);
        } catch (NoSuchMethodException | LinkageError e) {
            throw new Missing("method " + c.getSimpleName() + "." + name, e);
        }
    }

    private static Field field(Class<?> c, String name) throws Missing {
        try {
            return c.getField(name);
        } catch (NoSuchFieldException | LinkageError e) {
            throw new Missing("field " + c.getSimpleName() + "." + name, e);
        }
    }

    private static Constructor<?> ctor(Class<?> c, Class<?>... args) throws Missing {
        try {
            return c.getConstructor(args);
        } catch (NoSuchMethodException | LinkageError e) {
            throw new Missing("constructor " + c.getSimpleName(), e);
        }
    }

    private static Object call(Method m, @Nullable Object on, Object... args) throws ReflectiveOperationException {
        try {
            return m.invoke(on, args);
        } catch (InvocationTargetException e) {
            throw e;
        }
    }

    private Object[] stones() throws ReflectiveOperationException {
        if (stones == null) {
            stones = (Object[]) call(stoneValues, null);
        }
        return stones;
    }

    @Override
    public List<NodeInfo> nodes() {
        List<NodeInfo> out = new ArrayList<>();
        try {
            Object state = call(wheelState, null);
            if (state == seenState) {
                return seenNodes;
            }
            for (Object entry : (List<?>) call(stateEntries, state)) {
                int stone = (int) call(entryStone, entry);
                String node = (String) call(entryNode, entry);
                Object stoneObj = stones()[stone];
                String key = (String) stoneKey.get(stoneObj);
                NodeFacts nf = facts.get(key + "." + node);
                if (nf == null) {
                    nf = readFacts(stoneObj, node);
                    if (nf == null) {
                        continue;
                    }
                    facts.put(key + "." + node, nf);
                }
                out.add(new NodeInfo(stone, key, node, nf.name(), nf.description(), (boolean) call(entryUnlocked, entry), (boolean) call(entryToggled, entry),
                        (int) call(entrySetting, entry), nf.isSwitch(), nf.isChannel(), nf.needsAim(), nf.isDual(), nf.levels(), nf.menu(), nf.menuOnLeft()));
            }
            seenState = state;
            seenNodes = List.copyOf(out);
            return seenNodes;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // his state was not what the probe verified: the stones cannot be listed, so the compat is off (his wheel key comes back) rather than empty with his key covered
            failures.broken("reading his wheel state", e);
            return List.of();
        }
    }

    @Override
    public void reload() {
        facts.clear();
        seenState = null;
        seenNodes = List.of();
    }

    @Nullable
    private NodeFacts readFacts(Object stoneObj, String nodeId) throws ReflectiveOperationException {
        Object tree = call(treeOf, null, stoneObj);
        Object node = ((Map<?, ?>) treeNodes.get(tree)).get(nodeId);
        if (node == null) {
            return null;
        }
        String name = ((Component) call(nodeDisplayName, node)).getString();
        String description = ((Component) call(nodeDisplayDescription, node)).getString();
        NodeInfo.Levels levels = null;
        Object spec = call(setting, null, node);
        if (spec != null) {
            levels = new NodeInfo.Levels((int) call(settingMin, spec), (int) call(settingMax, spec), Math.max(1, (int) call(settingStep, spec)));
        }
        String modeId = (String) call(nodeModeId, node);
        boolean destination = (boolean) call(hasDestination, null, modeId);
        StoneMenus.Kind menu = StoneMenus.kind(StoneMenus.WILL_MODE.equals(modeId), destination, destination && (boolean) call(movesTarget, null, modeId),
                (boolean) call(hasChoiceMenu, null, modeId));
        return new NodeFacts(name, description, (boolean) call(isSwitch, null, node), (boolean) call(isChannel, null, node), (boolean) call(needsAim, null, node),
                (boolean) call(isDual, null, modeId), levels, menu, (boolean) call(menuOnLeft, null, modeId));
    }

    @Override
    public String holding() {
        try {
            return (String) call(stateHolding, call(wheelState, null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return "";
        }
    }

    @Override
    public long receivedAt() {
        try {
            return (long) call(receivedAt, null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return 0L;
        }
    }

    private static void send(Object payload) {
        PacketDistributor.sendToServer((CustomPacketPayload) payload);
    }

    /** Sends a payload; false (and one warning for {@code what}) when it could not be built or sent. */
    private boolean sendIt(String what, Callable<Object> payload) {
        try {
            send(payload.call());
            return true;
        } catch (Exception | LinkageError e) {
            failures.failed(what, e);
            return false;
        }
    }

    @Override
    public void use(int stone, String node, int phase) {
        sendIt("Use", () -> useCtor.newInstance(stone, node, phase));
    }

    @Override
    public void setLevel(int stone, String node, int delta) {
        sendIt("Setting", () -> settingCtor.newInstance(stone, node, delta));
    }

    private boolean show(String what, Callable<Object> screen) {
        try {
            Minecraft.getInstance().setScreen((Screen) screen.call());
            return true;
        } catch (Exception | LinkageError e) {
            failures.failed(what, e);
            return false;
        }
    }

    @Override
    public boolean openDestination(int stone, String node, boolean movesTarget) {
        return show("his destination screen", () -> destinationCtor.newInstance(stone, node, movesTarget, (int) call(stoneColor, stones()[stone])));
    }

    @Override
    public boolean openConstruct() {
        return show("his construct screen", () -> constructCtor.newInstance());
    }

    @Override
    public boolean openQuickSlots() {
        return show("his quick slots screen", () -> quickSlotsCtor.newInstance());
    }

    @Override
    public boolean requestChoiceMenu(int stone, String node) {
        try {
            call(requestMenu, null, stone, node);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failures.failed("his choice menu request", e);
            return false;
        }
    }

    @Override
    public boolean prayShown() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc == null ? null : mc.player;
        if (player == null) {
            return false;
        }
        try {
            boolean locked = gatherLocked != null && (boolean) call(gatherLocked, null);
            boolean holds = false;
            for (Object s : stones()) {
                if ((boolean) call(isHeldBy, s, player)) {
                    holds = true;
                    break;
                }
            }
            return StoneRules.prayShown(true, locked, holds);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // broken, not just failed: with Pray hidden and his key covered a stoneless player could not pray at all
            failures.broken("reading whether a stone is carried", e);
            return false;
        }
    }

    @Override
    public boolean pray() {
        return sendIt("Pray", () -> prayCtor.newInstance());
    }
    @Override
    public boolean soulSupported() {
        return soulState != null;
    }

    @Override
    public int soulMode() {
        if (soulState == null || soulMode == null) {
            return 0;
        }
        try {
            return (int) call(soulMode, soulState.get(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return 0;
        }
    }

    @Override
    public void soulAction(int action) {
        if (soulActionCtor == null) {
            return;
        }
        sendIt("soul action", () -> soulActionCtor.newInstance(action, 0));
    }
}
