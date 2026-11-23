package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.GroupClock;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapTable;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SideEvent;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.StackRules;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/**
 * Rule C on the server: no stacking, and one cooldown for every side of a power that several mods give. It links no other mod: Vampirism and Tensura are reached through
 * {@link VampirismSides} and {@link TensuraSides}, which listen on those mods' own events, so a press from a slot, from the wheel, from their own key or screen all pass the same
 * gate.
 * <ul>
 * <li>{@link #check} says whether a side may start now (another side is on, or the shared cooldown runs).</li>
 * <li>A start is first <i>intended</i> and only counted at the end of the player's tick if it really happened ({@link #settle}): Vampirism's event is posted before the action may still
 * say no, and a toggle can be refused after its event. An action that ended is counted at once.</li>
 * <li>The shared cooldown is always <b>the main side's own</b> ({@link #mainCooldown}), whichever side was used. A lasting side starts it when it ends.</li>
 * <li>{@link #reconcile} clears a side that is no longer on (its mod ended it without an event), so a stale "on" never locks the other side out.</li>
 * </ul>
 */
public final class WheelGate {
    private static final String RESOURCE = "/data/supernaturalbloodlines/wheel/overlaps.json";
    private static volatile OverlapTable table;
    private static volatile boolean warned;

    private record Pending(PowerRef ref, SideEvent event, boolean lasting, int mode) {
    }

    private static final Map<UUID, List<Pending>> PENDING = new ConcurrentHashMap<>();
    private static boolean registered;

    private WheelGate() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(WheelGate::onTick);
    }

    /** Listens on Vampirism's action events (call only when Vampirism is loaded). */
    public static void installVampirism() {
        VampirismSides.install();
    }

    /** Listens on ManasCore's skill and race events (call only when Tensura is loaded). */
    public static void installTensura() {
        TensuraSides.install();
    }

    // ---- the table ----

    /** The approved overlap table: read once from the mod's own data. A broken file is logged once and means no groups (nothing is hidden or blocked). */
    public static OverlapTable table() {
        OverlapTable t = table;
        if (t == null) {
            synchronized (WheelGate.class) {
                t = table;
                if (t == null) {
                    t = load();
                    table = t;
                }
            }
        }
        return t;
    }

    private static OverlapTable load() {
        try (InputStream in = WheelGate.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("missing " + RESOURCE);
            }
            return OverlapTable.parse(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            if (!warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("Wheel: the overlap table could not be read, so no power is merged or blocked", e);
            }
            return OverlapTable.EMPTY;
        }
    }

    /** Test seam: a table to use instead of the shipped one; {@code null} puts the shipped one back. */
    public static void setTableForTest(@Nullable OverlapTable t) {
        table = t;
    }

    // ---- the clock ----

    public static GroupClock clock(ServerPlayer p) {
        return p.getData(ModAttachments.WHEEL_CLOCK.get());
    }

    private static void setClock(ServerPlayer p, GroupClock c) {
        p.setData(ModAttachments.WHEEL_CLOCK.get(), c);
    }

    public static void resetForTest(ServerPlayer p) {
        PENDING.remove(p.getUUID());
        setClock(p, GroupClock.idle());
    }

    // ---- the gate ----

    /** @return why this power may not start now, empty when it may (or is in no group, or the guard is off) */
    public static Optional<LockReason> check(ServerPlayer p, PowerRef ref) {
        if (!HudConfig.wheelStackGuardEnabled()) {
            return Optional.empty();
        }
        OverlapTable t = table();
        List<OverlapGroup> groups = t.groupsOf(ref);
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        GroupClock c = clock(p);
        long now = WheelClocks.now(p);
        List<Pending> pending = PENDING.getOrDefault(p.getUUID(), List.of());
        for (OverlapGroup g : groups) {
            int side = t.sideOf(g, ref);
            Optional<LockReason> r = StackRules.mayStart(c, g.id(), side, now);
            if (r.isPresent()) {
                return r;
            }
            for (Pending other : pending) {
                if (other.event == SideEvent.ON && t.sideOf(g, other.ref) >= 0 && t.sideOf(g, other.ref) != side) {
                    return Optional.of(LockReason.GROUP_ACTIVE);
                }
            }
        }
        return Optional.empty();
    }

    /** The lang key a refusal for this reason shows. */
    public static String key(LockReason reason) {
        return "supernaturalbloodlines.wheel.lock." + reason.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** One action bar line, and the key. */
    public static String tell(ServerPlayer p, LockReason reason) {
        String key = key(reason);
        p.displayClientMessage(Component.translatable(key), true);
        return key;
    }

    /** The gate for an action named by id (ours, Vampirism's or Werewolves'), as the slots and the wheel use it. @return the refusal's lang key, null when it may start */
    @Nullable
    public static String checkAction(ServerPlayer p, ResourceLocation id) {
        PowerRef ref = actionRef(id);
        if (ref == null || isOn(p, ref)) {
            return null; // turning something off is never a start
        }
        Optional<LockReason> r = check(p, ref);
        return r.isPresent() ? tell(p, r.get()) : null;
    }

    /** An action of ours that just ran (a bite, the Alpha Roar...): our code has no event to listen on, so the path that ran it says so. */
    public static void ranOurs(ServerPlayer p, ResourceLocation id) {
        if (HudConfig.wheelStackGuardEnabled() && (Loadout.BITE.equals(id) || ExtraActions.get(id).isPresent())) {
            apply(p, new PowerRef(PowerSource.BLOODLINE, id), SideEvent.FIRED, false, 0);
        }
    }

    @Nullable
    private static PowerRef actionRef(ResourceLocation id) {
        if (Loadout.BITE.equals(id) || ExtraActions.get(id).isPresent()) {
            return new PowerRef(PowerSource.BLOODLINE, id);
        }
        PowerSource source = VampirismGate.sourceOf(id);
        return source == null ? null : new PowerRef(source, id);
    }

    // ---- recording ----

    /** A side is about to start: counted at the end of the tick if it really did. */
    static void intend(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode) {
        if (!HudConfig.wheelStackGuardEnabled() || table().groupsOf(ref).isEmpty()) {
            return;
        }
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Pending(ref, event, lasting, mode));
    }

    /**
     * Counts an event at once, in every group the power is in.
     *
     * @return the longest shared cooldown now running in those groups, in ticks
     */
    public static int apply(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode) {
        if (!HudConfig.wheelStackGuardEnabled()) {
            return 0;
        }
        OverlapTable t = table();
        List<OverlapGroup> groups = t.groupsOf(ref);
        if (groups.isEmpty()) {
            return 0;
        }
        if (event == SideEvent.OFF) {
            List<Pending> pending = PENDING.get(p.getUUID());
            if (pending != null) {
                pending.removeIf(x -> x.ref.equals(ref) && x.event == SideEvent.ON);
            }
        }
        GroupClock c = clock(p);
        long now = WheelClocks.now(p);
        int longest = 0;
        for (OverlapGroup g : groups) {
            int side = t.sideOf(g, ref);
            int cooldown = event == SideEvent.ON && lasting ? 0 : mainCooldown(p, g, ref, mode);
            c = c.onEvent(g.id(), side, event, lasting, cooldown, now);
            longest = Math.max(longest, c.cooldownLeft(g.id(), now));
        }
        setClock(p, c);
        return longest;
    }

    /** Counts what was intended this tick, if it happened. */
    public static void settle(ServerPlayer p) {
        List<Pending> pending = PENDING.remove(p.getUUID());
        if (pending == null) {
            return;
        }
        for (Pending x : pending) {
            if (confirmed(p, x)) {
                apply(p, x.ref, x.event, x.lasting, x.mode);
            }
        }
    }

    private static boolean confirmed(ServerPlayer p, Pending x) {
        return switch (x.ref.source()) {
            case VAMPIRISM, WEREWOLVES -> !VampirismGate.loaded() || VampirismSides.confirmed(p, x.ref.id(), x.lasting);
            case TENSURA -> !TensuraGate.loaded() || x.event != SideEvent.ON || TensuraSides.toggled(p, x.ref.id());
            default -> true;
        };
    }

    /** Clears every side that is no longer on (the mod ended it without an event, the player died or left): its cooldown starts now. */
    public static void reconcile(ServerPlayer p) {
        GroupClock c = clock(p);
        if (c.isIdle()) {
            return;
        }
        OverlapTable t = table();
        for (OverlapGroup g : t.groups()) {
            var active = c.activeSide(g.id());
            if (active.isPresent() && active.getAsInt() < g.members().size()) {
                PowerRef ref = g.members().get(active.getAsInt());
                if (!stillOn(p, ref)) {
                    apply(p, ref, SideEvent.OFF, true, 0);
                    c = clock(p);
                }
            }
        }
    }

    /** The player died, left or changed dimension: whatever was on is over. */
    public static void endAll(ServerPlayer p) {
        PENDING.remove(p.getUUID());
        reconcile(p);
    }

    /** True when the power is on right now (so using it again turns it off, which no group may stop). */
    public static boolean isOn(ServerPlayer p, PowerRef ref) {
        return stillOn(p, ref);
    }

    private static boolean stillOn(ServerPlayer p, PowerRef ref) {
        return switch (ref.source()) {
            case VAMPIRISM, WEREWOLVES -> VampirismGate.loaded() && VampirismSides.stillOn(p, ref.id());
            case TENSURA -> TensuraGate.loaded() && TensuraSides.stillOn(p, ref.id());
            default -> false;
        };
    }

    /** The shared cooldown a use of {@code used} starts: the main side's own, 0 when the main side has none or the player does not have it. */
    static int mainCooldown(ServerPlayer p, OverlapGroup g, PowerRef used, int mode) {
        PowerRef main = g.mainMember();
        OverlapTable t = table();
        return switch (main.source()) {
            case BLOODLINE -> ourCooldown(p, main.id());
            case VAMPIRISM, WEREWOLVES -> VampirismGate.loaded() ? VampirismSides.cooldownTicks(p, main.id()) : 0;
            // Tensura gives no cooldown before a press: the value is read from the instance right after the main side was pressed; for another side it is unknown (no cooldown)
            case TENSURA -> TensuraGate.loaded() && t.canonical(used).equals(t.canonical(main)) ? TensuraSides.cooldownTicks(p, main.id(), mode) : 0;
            default -> 0;
        };
    }

    private static int ourCooldown(ServerPlayer p, ResourceLocation id) {
        if (Loadout.BITE.equals(id)) {
            return BiteService.canBite(p) ? BiteService.COOLDOWN_TICKS : 0;
        }
        return ExtraActions.get(id).filter(a -> a.available(p)).map(a -> Math.max(0, a.cooldownTicks(p))).orElse(0);
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof net.neoforged.neoforge.common.util.FakePlayer) {
            return;
        }
        settle(p);
        if (p.tickCount % 10 == 0) {
            reconcile(p);
        }
    }
}
