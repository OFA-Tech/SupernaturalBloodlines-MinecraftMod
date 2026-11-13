package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.entities.player.werewolf.actions.WerewolfFormAction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;

/**
 * Hybrids shift at will, so the forced full-moon shift is cancelled for them (no mixin). Werewolves forces it from
 * its player tick through {@code ActionHandler.toggleAction}, which fires the cancellable
 * {@code ActionEvent.ActionActivatedEvent}; a voluntary shift arrives from a network packet, outside any player tick.
 * So a form action activated while this player is being ticked is the forced one. Links Werewolves: register it only
 * behind {@code ModList.isLoaded("werewolves")}.
 */
public final class FormControl {
    // PlayerTickEvent also fires on the client thread of a singleplayer game, hence the thread-local
    private static final ThreadLocal<Player> TICKING = new ThreadLocal<>();

    private FormControl() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerTickEvent.Pre e) -> enterTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerTickEvent.Post e) -> leaveTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FormControl::onActionActivated);
    }

    public static void enterTick(Player p) {
        TICKING.set(p);
    }

    public static void leaveTick(Player p) {
        if (TICKING.get() == p) {
            TICKING.remove();
        }
    }

    private static void onActionActivated(ActionEvent.ActionActivatedEvent e) {
        Player p = e.getFactionPlayer().asEntity();
        if (e.getAction() instanceof WerewolfFormAction && p == TICKING.get() && !p.level().isClientSide
                && HybridTraits.isHybrid(BloodlineAccess.get(p).species())) {
            e.setCanceled(true);
        }
    }
}
