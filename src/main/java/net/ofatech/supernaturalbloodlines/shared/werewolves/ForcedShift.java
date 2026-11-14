package net.ofatech.supernaturalbloodlines.shared.werewolves;

import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.entities.player.werewolf.actions.WerewolfFormAction;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;

/**
 * Spots and cancels the forced full-moon shift for players with {@link MoonControl} (no mixin). Werewolves forces the
 * shift from its player tick through {@code ActionHandler.toggleAction}, which fires the cancellable
 * {@code ActionEvent.ActionActivatedEvent}; a voluntary shift arrives from a network packet, outside any player tick.
 * So a form action activated while this player is being ticked is the forced one. Links Werewolves: call
 * {@link #register} only behind {@code ModList.isLoaded("werewolves")}. Every part that grants control calls it; only
 * the first call does anything.
 */
public final class ForcedShift {
    // PlayerTickEvent also fires on the client thread of a singleplayer game, hence the thread-local
    private static final ThreadLocal<Player> TICKING = new ThreadLocal<>();
    public static final String BOUND_MESSAGE = "supernaturalbloodlines.hybrid_curse.bound";
    public static final String NO_CONTROL_MESSAGE = "supernaturalbloodlines.moon_control.none";
    // reasons to allow a form action without moon control (the Alpha Roar lets the pack shift)
    private static final List<Predicate<ServerPlayer>> PERMITS = new CopyOnWriteArrayList<>();
    // reasons to cancel every form action, forced or not (the lunar eclipse, Shocked), each with the message it shows
    private record FormBlock(Predicate<ServerPlayer> blocked, String message) {
    }

    private static final List<FormBlock> BLOCKS = new CopyOnWriteArrayList<>();
    private static boolean registered;

    private ForcedShift() {
    }

    /** A condition that cancels every Werewolves form action of the player (the roar's permit does not lift it). */
    public static void addFormBlock(Predicate<ServerPlayer> blocked, String messageKey) {
        BLOCKS.add(new FormBlock(blocked, messageKey));
    }

    /** A temporary exemption from the form gate: while it holds, the player may shift at will. */
    public static void addFormPermit(Predicate<ServerPlayer> permitted) {
        PERMITS.add(permitted);
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerTickEvent.Pre e) -> enterTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerTickEvent.Post e) -> leaveTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ForcedShift::onActionActivated);
        // last, and not for a cancelled shift: the full moon forced the wolf into a form and it happened
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ActionEvent.ActionActivatedEvent e) -> {
            if (isForced(e) && e.getFactionPlayer().asEntity() instanceof Player p) {
                TaskSignals.fire(p, "forced_shift_survived", 1);
            }
        });
    }

    public static void enterTick(Player p) {
        TICKING.set(p);
    }

    public static void leaveTick(Player p) {
        if (TICKING.get() == p) {
            TICKING.remove();
        }
    }

    /** True for a form action Werewolves activates from this player's own tick: the full-moon shift. */
    public static boolean isForced(ActionEvent.ActionActivatedEvent e) {
        Player p = e.getFactionPlayer().asEntity();
        return e.getAction() instanceof WerewolfFormAction && p == TICKING.get() && !p.level().isClientSide;
    }

    private static void onActionActivated(ActionEvent.ActionActivatedEvent e) {
        if (!(e.getFactionPlayer().asEntity() instanceof ServerPlayer p)) {
            return;
        }
        // R24: a bound hybrid has no forms at all, forced or not, until the Rite of the Moon
        if (e.getAction() instanceof WerewolfFormAction && BloodlineAccess.get(p).hybridCurseBound()) {
            e.setCanceled(true);
            if (!isForced(e)) { // the forced shift retries every second: no message spam
                p.displayClientMessage(Component.translatable(BOUND_MESSAGE), true);
            }
            return;
        }
        if (e.getAction() instanceof WerewolfFormAction) {
            for (FormBlock block : BLOCKS) {
                if (block.blocked().test(p)) {
                    e.setCanceled(true);
                    if (!isForced(e)) {
                        p.displayClientMessage(Component.translatable(block.message()), true);
                    }
                    return;
                }
            }
        }
        if (isForced(e)) {
            if (MoonControl.of(p) != MoonControl.Control.NONE || halvedAndSpared(p) || moonCalmSpares(p)) {
                e.setCanceled(true);
            }
        } else if (e.getAction() instanceof WerewolfFormAction form && MoonControl.moonBound() && !allowedAtWill(p, form)) {
            // R96: without control a werewolf shifts only when the full moon forces it
            e.setCanceled(true);
            p.displayClientMessage(Component.translatable(NO_CONTROL_MESSAGE), true);
        }
    }

    /** Whether {@code p} may toggle this form action on by choice: HALF allows human and survivalist, FULL the beasts too. */
    private static boolean allowedAtWill(ServerPlayer p, WerewolfFormAction action) {
        if (PERMITS.stream().anyMatch(permit -> permit.test(p))) {
            return true;
        }
        WerewolfForm form = action.getForm();
        return switch (MoonControl.of(p)) {
            case NONE -> false;
            case HALF -> form == WerewolfForm.HUMAN || form == WerewolfForm.SURVIVALIST;
            case FULL -> true;
        };
    }

    /** S-WOL-2 Moon Calm: the forced shift comes {@code shift_chance_factor} (0.75) as often, so that share of the nights is spared on top of the rest. */
    public static boolean moonCalmSpares(ServerPlayer p) {
        double spared = 1 - LadderQuery.value(p, "S-WOL-2", "shift_chance_factor", 1.0);
        return spared > 0 && MoonControl.sparedOn(p.getUUID(), p.level().getDayTime() / 24000L, spared);
    }

    /**
     * R90 and R94: a born wolf, or a wolf with an anchor near, is spared the forced shift on half of the nights. The two
     * reasons do not stack (one coin per night), and a born wolf with an anchor has control anyway.
     */
    private static boolean halvedAndSpared(ServerPlayer p) {
        boolean born = BloodlineAccess.get(p).wolf().lineage() == WolfState.Lineage.BORN;
        return (born || MoonControl.forcedShiftHalved(p)) && MoonControl.bornSparedOn(p.getUUID(), p.level().getDayTime() / 24000L);
    }
}
