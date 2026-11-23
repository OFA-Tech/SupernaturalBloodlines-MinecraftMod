package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
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
        if (out.size() > WheelCataloguePayload.MAX_ENTRIES) {
            SupernaturalBloodlines.LOGGER.warn("Wheel: {} powers, showing the first {}", out.size(), WheelCataloguePayload.MAX_ENTRIES);
            return List.copyOf(out.subList(0, WheelCataloguePayload.MAX_ENTRIES));
        }
        return List.copyOf(out);
    }

    private static WheelEntry lockedForCannotAct(WheelEntry e) {
        PowerState s = e.state();
        if (s.lock() != LockReason.NONE) {
            return e;
        }
        return new WheelEntry(e.ref(), e.group(), e.kind(), e.name(), e.modeName(), e.modeNames(), e.icon(),
                new PowerState(s.on(), s.cooldownEnd(), s.cooldownTotal(), LockReason.CANNOT_ACT, s.mode(), s.modeCount()), e.slottable(), e.mastery());
    }

    private static void warnOnce(PowerProvider provider, Throwable ex) {
        if (WARNED.add(provider.getClass())) {
            SupernaturalBloodlines.LOGGER.warn("Wheel: the {} provider failed and is skipped (shown once)", provider.getClass().getSimpleName(), ex);
        }
    }
}
