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
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SharedCooldown;
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

    /** Tensura's race ability, which has no skill id. */
    public static final ResourceLocation RACE_ABILITY_ID = ResourceLocation.fromNamespaceAndPath("tensura", "race_ability");
    /** The two groups the vampire race ability is in: Bats Mode is the bat form, a drain is the bite. */
    static final String BAT_GROUP = "bat_form";
    static final String BITE_GROUP = "bite";

    /** {@code only}: the one group this is counted in, null for every group of the power. */
    private record Pending(PowerRef ref, SideEvent event, boolean lasting, int mode, @Nullable String only) {
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

    static boolean isRaceAbility(PowerRef ref) {
        return ref.source() == PowerSource.TENSURA && RACE_ABILITY_ID.equals(ref.id());
    }

    /**
     * The groups a power counts in for this player. The race ability is the vampire race's only: another race's ability is not a bat form nor a bite. {@code only} narrows it to one
     * group (the race ability's press ends as a bat form or as a bite, never both).
     */
    private static List<OverlapGroup> groupsFor(ServerPlayer p, PowerRef ref, @Nullable String only) {
        List<OverlapGroup> all = table().groupsOf(ref);
        if (all.isEmpty() || isRaceAbility(ref) && !TensuraGate.vampireRace(p)) {
            return List.of();
        }
        return only == null ? all : all.stream().filter(g -> g.id().equals(only)).toList();
    }

    /** Whether the power is the same power for this player in every group it is listed in (false for another race's ability). */
    public static boolean groupable(ServerPlayer p, PowerRef ref) {
        return !isRaceAbility(ref) || TensuraGate.vampireRace(p);
    }

    /** True when the power is in at least one group for this player. */
    static boolean inAnyGroup(ServerPlayer p, PowerRef ref) {
        return !groupsFor(p, ref, null).isEmpty();
    }

    /** @return why this power may not start now, empty when it may (or is in no group, or the guard is off) */
    public static Optional<LockReason> check(ServerPlayer p, PowerRef ref) {
        if (!HudConfig.wheelStackGuardEnabled()) {
            return Optional.empty();
        }
        OverlapTable t = table();
        List<OverlapGroup> groups = groupsFor(p, ref, null);
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        GroupClock c = clock(p);
        long now = WheelClocks.now(p);
        List<Pending> pending = PENDING.getOrDefault(p.getUUID(), List.of());
        for (OverlapGroup g : groups) {
            List<StackRules.Intent> others = new ArrayList<>();
            for (Pending other : pending) {
                if ((other.only == null || other.only.equals(g.id())) && t.sideOf(g, other.ref) >= 0) {
                    others.add(new StackRules.Intent(t.sideKey(g, other.ref), other.event));
                }
            }
            Optional<LockReason> r = StackRules.mayStart(c, g.id(), t.sideKey(g, ref), now, others);
            if (r.isPresent()) {
                return r;
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

    /** A side is about to start (or end): counted at the end of the tick if it really did. */
    static void intend(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode) {
        intend(p, ref, event, lasting, mode, null);
    }

    static void intend(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode, @Nullable String only) {
        if (!HudConfig.wheelStackGuardEnabled() || groupsFor(p, ref, only).isEmpty()) {
            return;
        }
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Pending(ref, event, lasting, mode, only));
    }

    /**
     * Counts an event at once, in every group the power is in.
     *
     * @return the longest shared cooldown now running in those groups, in ticks
     */
    public static int apply(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode) {
        return apply(p, ref, event, lasting, mode, null);
    }

    static int apply(ServerPlayer p, PowerRef ref, SideEvent event, boolean lasting, int mode, @Nullable String only) {
        if (!HudConfig.wheelStackGuardEnabled()) {
            return 0;
        }
        OverlapTable t = table();
        List<OverlapGroup> groups = groupsFor(p, ref, only);
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
            int cooldown = event == SideEvent.ON && lasting ? 0 : mainCooldown(p, g, ref, mode);
            c = c.onEvent(g.id(), t.sideKey(g, ref), event, lasting, cooldown, now);
            longest = Math.max(longest, c.cooldownLeft(g.id(), now));
        }
        setClock(p, c);
        return longest;
    }

    /** Counts what was intended this tick, as far as it happened. */
    public static void settle(ServerPlayer p) {
        List<Pending> pending = PENDING.remove(p.getUUID());
        if (pending == null) {
            return;
        }
        for (Pending x : pending) {
            SideEvent happened = outcome(p, x);
            if (happened != null) {
                apply(p, x.ref, happened, x.lasting && happened != SideEvent.FIRED, x.mode, x.only);
            }
        }
    }

    /** What an intended event turned into: itself when it happened, another event when it happened in another way (a held spell that already ended), null when it did not. */
    @Nullable
    private static SideEvent outcome(ServerPlayer p, Pending x) {
        if (x.event == SideEvent.OFF) {
            return stillOn(p, x.ref) ? null : SideEvent.OFF;
        }
        return switch (x.ref.source()) {
            case VAMPIRISM, WEREWOLVES -> !VampirismGate.loaded() || VampirismSides.confirmed(p, x.ref.id(), x.lasting) ? x.event : null;
            case TENSURA -> TensuraGate.loaded() ? TensuraSides.outcome(p, x.ref.id(), x.event, x.mode) : x.event;
            default -> x.event;
        };
    }

    /** One tick of the gate for a player: what was intended is counted, and every ten ticks the clock is made to agree with what is really on. */
    public static void tick(ServerPlayer p) {
        settle(p);
        if (StackRules.reconcileDue(p.tickCount)) {
            reconcile(p);
        }
    }

    /**
     * Makes the clock agree with what is really on: a side that is no longer on is ended (the mod ended it without an event, the player died or left: its cooldown starts now), and a
     * side that is on but was never recorded is marked as the active side (the HUD or the guard was off when it started, Vampirism's own vision key has no event).
     */
    public static void reconcile(ServerPlayer p) {
        reconcile(p, true);
    }

    private static void reconcile(ServerPlayer p, boolean discover) {
        if (!HudConfig.wheelStackGuardEnabled()) {
            return;
        }
        OverlapTable t = table();
        for (OverlapGroup g : t.groups()) {
            var active = clock(p).activeSide(g.id());
            if (active.isPresent()) {
                int side = t.sideOfKey(g, active.get());
                if (side < 0 || !stillOnIn(p, g.members().get(side), g)) {
                    setClock(p, clock(p).onEvent(g.id(), active.get(), SideEvent.OFF, true, side < 0 ? 0 : mainCooldown(p, g, g.members().get(side), 0), WheelClocks.now(p)));
                }
            } else if (discover) {
                for (PowerRef member : g.members()) {
                    if (stillOnIn(p, member, g)) {
                        apply(p, member, SideEvent.ON, true, 0, g.id());
                        break;
                    }
                }
            }
        }
    }

    /** The player died, left or changed dimension: whatever was on is over. */
    public static void endAll(ServerPlayer p) {
        PENDING.remove(p.getUUID());
        reconcile(p, false);
    }

    /** True when the power is on right now (so using it again turns it off, which no group may stop). */
    public static boolean isOn(ServerPlayer p, PowerRef ref) {
        return stillOn(p, ref);
    }

    private static boolean stillOn(ServerPlayer p, PowerRef ref) {
        return switch (ref.source()) {
            case VAMPIRISM, WEREWOLVES -> VampirismGate.loaded() && VampirismSides.stillOn(p, ref.id());
            case TENSURA -> TensuraGate.loaded() && anyTierOn(p, ref);
            default -> false;
        };
    }

    /** A group names one tier of a chain, the player may own and have on any other: the clock saves them all as the first one, so this is on when any tier of the chain is. */
    private static boolean anyTierOn(ServerPlayer p, PowerRef ref) {
        for (PowerRef tier : table().tiersOf(ref)) {
            if (TensuraSides.stillOn(p, tier.id())) {
                return true;
            }
        }
        return false;
    }

    /** On in this group: the race ability is on in the bat form group only (Bats Mode), never in the bite one. */
    private static boolean stillOnIn(ServerPlayer p, PowerRef ref, OverlapGroup g) {
        return (!isRaceAbility(ref) || BAT_GROUP.equals(g.id())) && stillOn(p, ref);
    }

    /** The shared cooldown a use of {@code used} starts: see {@link SharedCooldown#pick}. */
    static int mainCooldown(ServerPlayer p, OverlapGroup g, PowerRef used, int mode) {
        int usedSide = table().sideOf(g, used);
        int[] own = new int[g.members().size()];
        for (int i = 0; i < own.length; i++) {
            own[i] = ownCooldown(p, g.members().get(i), i == usedSide ? used : null, mode);
        }
        return SharedCooldown.pick(own, g.main(), usedSide);
    }

    /** One member's own cooldown in ticks, {@link SharedCooldown#NOT_OWNED} when the player does not have it or it cannot be read. */
    private static int ownCooldown(ServerPlayer p, PowerRef member, @Nullable PowerRef usedHere, int mode) {
        return switch (member.source()) {
            case BLOODLINE -> ourCooldown(p, member.id());
            case VAMPIRISM, WEREWOLVES -> VampirismGate.loaded() ? VampirismSides.cooldownTicks(p, member.id()) : SharedCooldown.NOT_OWNED;
            // Tensura gives no cooldown before a press: the value is read from the instance right after the side was pressed; for another side it is unknown
            // (the tier that was used, which may be a higher tier of the chain the group names)
            case TENSURA -> TensuraGate.loaded() && usedHere != null ? TensuraSides.cooldownTicks(p, usedHere.id(), mode) : SharedCooldown.NOT_OWNED;
            default -> SharedCooldown.NOT_OWNED;
        };
    }

    private static int ourCooldown(ServerPlayer p, ResourceLocation id) {
        if (Loadout.BITE.equals(id)) {
            return BiteService.canBite(p) ? BiteService.COOLDOWN_TICKS : SharedCooldown.NOT_OWNED;
        }
        return ExtraActions.get(id).filter(a -> a.available(p)).map(a -> Math.max(0, a.cooldownTicks(p))).orElse(SharedCooldown.NOT_OWNED);
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof net.neoforged.neoforge.common.util.FakePlayer) {
            return;
        }
        tick(p);
    }
}
