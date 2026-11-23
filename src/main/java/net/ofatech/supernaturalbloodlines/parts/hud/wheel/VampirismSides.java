package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SharedCooldown;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SideEvent;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;

/**
 * Rule C for the actions of Vampirism and Werewolves: it listens on their own action events, so the gate sits below every path (our slots, the wheel, their keys and wheel).
 * Linked to Vampirism: installed only after {@code ModList.isLoaded("vampirism")}.
 * <ul>
 * <li>{@link ActionEvent.ActionActivatedEvent} (cancellable) is cancelled when the gate says no; otherwise the start is intended.</li>
 * <li>{@link ActionEvent.ActionDeactivatedEvent} counts the end at once and raises the cooldown the action starts when it ends to at least the group's (the main side's own).</li>
 * </ul>
 */
final class VampirismSides {
    private static boolean installed;

    private VampirismSides() {
    }

    static void install() {
        if (installed) {
            return;
        }
        installed = true;
        // last, so an event another mod already cancelled is not counted and ours is the final word
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VampirismSides::onActivated);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VampirismSides::onDeactivated);
    }

    private static PowerRef refOf(IAction<?> action) {
        ResourceLocation id = FactionActions.idOf(action);
        return id == null ? null : new PowerRef(FactionProvider.sourceOf(action, id), id);
    }

    private static void onActivated(ActionEvent.ActionActivatedEvent e) {
        if (e.isCanceled() || !(e.getFactionPlayer().asEntity() instanceof ServerPlayer p)) {
            return;
        }
        PowerRef ref = refOf(e.getAction());
        if (ref == null || WheelGate.table().groupsOf(ref).isEmpty()) {
            return;
        }
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            e.setCanceled(true);
            WheelGate.tell(p, refusal.get());
            return;
        }
        boolean lasting = e.getAction() instanceof ILastingAction<?>;
        WheelGate.intend(p, ref, lasting ? SideEvent.ON : SideEvent.FIRED, lasting, 0);
    }

    private static void onDeactivated(ActionEvent.ActionDeactivatedEvent e) {
        if (!(e.getFactionPlayer().asEntity() instanceof ServerPlayer p)) {
            return;
        }
        PowerRef ref = refOf(e.getAction());
        if (ref == null || WheelGate.table().groupsOf(ref).isEmpty() || !HudConfig.wheelStackGuardEnabled()) {
            return;
        }
        int groupLeft = WheelGate.apply(p, ref, SideEvent.OFF, true, 0);
        // the cooldown the action starts now is at least the group's: the main side's own, whichever side was on
        e.setCooldown(Math.max(e.getCooldown(), groupLeft));
    }

    /** The cooldown the player's action has (0 for none; the vision pseudo power has none), {@link SharedCooldown#NOT_OWNED} when they do not have it unlocked in their current faction. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int cooldownTicks(ServerPlayer p, ResourceLocation id) {
        Optional<? extends IFactionPlayer<?>> fp = FactionActions.current(p);
        if (VampirismVision.ID.equals(id)) {
            return fp.isPresent() ? 0 : SharedCooldown.NOT_OWNED;
        }
        IAction action = FactionActions.byId(id);
        if (action == null || fp.isEmpty() || !FactionActions.unlocked(fp.get(), action)) {
            return SharedCooldown.NOT_OWNED;
        }
        return Math.max(0, action.getCooldown(fp.get()));
    }

    /** Did the start that was intended really happen: a lasting action is active, an instant one is on cooldown (or has none to show). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean confirmed(ServerPlayer p, ResourceLocation id, boolean lasting) {
        IAction action = FactionActions.byId(id);
        Optional<? extends IFactionPlayer<?>> fp = FactionActions.current(p);
        if (action == null || fp.isEmpty()) {
            return false;
        }
        IActionHandler handler = (IActionHandler) fp.get().getActionHandler();
        if (lasting) {
            return handler.isActionActive(id);
        }
        return action.getCooldown(fp.get()) <= 0 || handler.isActionOnCooldown(action);
    }

    static boolean stillOn(ServerPlayer p, ResourceLocation id) {
        if (VampirismVision.ID.equals(id)) {
            return VampirismVision.active(p);
        }
        return FactionActions.current(p).map(fp -> fp.getActionHandler().isActionActive(id)).orElse(false);
    }
}
