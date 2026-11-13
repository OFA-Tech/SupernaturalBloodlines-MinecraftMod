package net.ofatech.supernaturalbloodlines.shared.werewolves;

import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.entities.player.werewolf.actions.WerewolfFormAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;

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
    private static boolean registered;

    private ForcedShift() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerTickEvent.Pre e) -> enterTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerTickEvent.Post e) -> leaveTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ForcedShift::onActionActivated);
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
        if (isForced(e) && MoonControl.of(p) != MoonControl.Control.NONE) {
            e.setCanceled(true);
        }
    }
}
