package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.Dedup;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.GroupClock;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapTable;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.StackRules;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelCataloguePayload;

/**
 * Every power the player has, from the installed providers, in the wheel's order (source, then the provider's own order). A provider that throws is logged once and
 * skipped: it never breaks the list. A player who cannot act (downed, snapped, dying, asleep, spectator) sees every usable power locked. Server only.
 */
public final class WheelCatalogue {
    private static final List<PowerProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Set<Class<?>> WARNED = new HashSet<>();
    /** How many times {@link #build} ran: a test seam to prove a closed wheel is never built. */
    private static volatile int builds;

    private WheelCatalogue() {
    }

    public static void install(PowerProvider p) {
        PROVIDERS.add(p);
    }

    public static void remove(PowerProvider p) {
        PROVIDERS.remove(p);
    }

    public static int builds() {
        return builds;
    }

    public static List<WheelEntry> build(ServerPlayer p) {
        builds++;
        boolean cannotAct = ActionService.cannotAct(p);
        List<WheelEntry> out = new ArrayList<>();
        for (PowerProvider provider : PROVIDERS) {
            try {
                for (WheelEntry e : provider.list(p)) {
                    out.add(cannotAct ? lockedForCannotAct(e) : e);
                }
            } catch (RuntimeException | LinkageError ex) {
                warnOnce(provider, ex);
            }
        }
        out.sort(Comparator.comparingInt(e -> e.ref().source().ordinal()));
        out = withGroups(p, out);
        if (out.size() > WheelCataloguePayload.MAX_ENTRIES) {
            SupernaturalBloodlines.LOGGER.warn("Wheel: {} powers, showing the first {}", out.size(), WheelCataloguePayload.MAX_ENTRIES);
            return List.copyOf(out.subList(0, WheelCataloguePayload.MAX_ENTRIES));
        }
        return List.copyOf(out);
    }

    /**
     * Rule C. Every power that is in a group carries the group's shared cooldown and is locked while another side is on; with deduplication on, one entry stays for each group (the side
     * that is on, else the main side, else the next one) and carries the group's id.
     */
    private static List<WheelEntry> withGroups(ServerPlayer p, List<WheelEntry> entries) {
        OverlapTable table = WheelGate.table();
        if (table.groups().isEmpty()) {
            return entries;
        }
        GroupClock clock = WheelGate.clock(p);
        long now = WheelClocks.now(p);
        List<Cand> cands = new ArrayList<>(entries.size());
        for (WheelEntry e : entries) {
            cands.add(new Cand(e, e.modeCount() <= 1 && WheelGate.groupable(p, e.ref())));
        }
        List<Cand> shown = cands;
        Map<Cand, OverlapGroup> groupOf = Map.of();
        if (HudConfig.wheelDedupEnabled()) {
            Map<String, OverlapGroup> byId = new HashMap<>();
            table.groups().forEach(g -> byId.put(g.id(), g));
            Dedup.Result<Cand> r = Dedup.apply(cands, table, id -> {
                OverlapGroup g = byId.get(id);
                return g == null ? -1 : clock.activeSide(id).map(key -> table.sideOfKey(g, key)).orElse(-1);
            });
            shown = r.shown();
            groupOf = r.groupOf();
        }
        List<WheelEntry> out = new ArrayList<>(shown.size());
        for (Cand c : shown) {
            WheelEntry e = c.entry;
            if (HudConfig.wheelStackGuardEnabled() && c.groupable) {
                for (OverlapGroup g : table.groupsOf(e.ref())) {
                    e = e.withState(StackRules.overlay(e.state(), clock, g.id(), table.sideKey(g, e.ref()), now));
                }
            }
            OverlapGroup stands = groupOf.get(c);
            out.add(stands == null ? e : e.withGroup(stands.id()));
        }
        return out;
    }

    /** An entry as the groups see it: {@code groupable} is false for a skill with modes and for the race ability of a race that is not the vampire's. */
    private record Cand(WheelEntry entry, boolean groupable) implements Dedup.Item {
        @Override
        public PowerRef ref() {
            return entry.ref();
        }

        @Override
        public int modeCount() {
            return entry.modeCount();
        }
    }

    private static WheelEntry lockedForCannotAct(WheelEntry e) {
        PowerState s = e.state();
        if (s.lock() != LockReason.NONE) {
            return e;
        }
        return e.withState(s.withLock(LockReason.CANNOT_ACT));
    }

    private static void warnOnce(PowerProvider provider, Throwable ex) {
        if (WARNED.add(provider.getClass())) {
            SupernaturalBloodlines.LOGGER.warn("Wheel: the {} provider failed and is skipped (shown once)", provider.getClass().getSimpleName(), ex);
        }
    }
}
