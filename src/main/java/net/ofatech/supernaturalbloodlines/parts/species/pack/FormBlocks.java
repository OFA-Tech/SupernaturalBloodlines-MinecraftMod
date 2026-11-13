package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.util.FormHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/**
 * The two weaknesses that cancel every Werewolves form action, forced or chosen: the lunar eclipse and Shocked. Cancelling
 * the activation does nothing to a form that is already on, so once a second a blocked player's active form is dropped
 * ({@code FormHelper.deactivateWerewolfActions}, as the Alpha Roar does). Links Werewolves (through {@link ForcedShift}):
 * call {@link #register} only behind {@code isLoaded("werewolves")}.
 */
public final class FormBlocks {
    public static final String ECLIPSE_MESSAGE = "supernaturalbloodlines.eclipse.blocked";
    public static final String SHOCKED_MESSAGE = "supernaturalbloodlines.shocked.blocked";
    public static final int SWEEP_PERIOD = 20;

    private FormBlocks() {
    }

    public static void register() {
        ForcedShift.register();
        ForcedShift.addFormBlock(p -> LunarEclipse.active(p.server), ECLIPSE_MESSAGE);
        ForcedShift.addFormBlock(p -> p.hasEffect(SpeciesRegistries.SHOCKED), SHOCKED_MESSAGE);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % SWEEP_PERIOD == 0) {
                sweep(p);
            }
        });
    }

    /** Drops the form of a player who is under a block. True when a form was on and is off now. */
    public static boolean sweep(ServerPlayer p) {
        if (!LunarEclipse.active(p.server) && !p.hasEffect(SpeciesRegistries.SHOCKED)) {
            return false;
        }
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer()
                .filter(f -> f instanceof IWerewolfPlayer).map(f -> (IWerewolfPlayer) f)
                .filter(FormHelper::isFormActionActive)
                .map(w -> {
                    FormHelper.deactivateWerewolfActions(w);
                    return true;
                }).orElse(false);
    }
}
