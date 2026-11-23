package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/**
 * The actions of the player's current faction (Vampirism, and Werewolves through the same action handler), plus Vampirism's Vision entry. Linked to Vampirism:
 * reached only after {@code ModList.isLoaded("vampirism")}. An action that is not unlocked is not listed.
 */
public final class FactionProvider implements PowerProvider {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<WheelEntry> list(ServerPlayer p) {
        List<WheelEntry> out = new ArrayList<>();
        Optional<? extends IFactionPlayer<?>> current = FactionActions.current(p);
        if (current.isEmpty()) {
            return out;
        }
        IFactionPlayer fp = current.get();
        IActionHandler handler = (IActionHandler) fp.getActionHandler();
        for (ResourceLocation id : FactionActions.unlockedIds(p)) {
            IAction action = FactionActions.byId(id);
            if (action == null) {
                continue;
            }
            boolean lasting = action instanceof ILastingAction;
            PowerKind kind = lasting ? PowerKind.TOGGLE : AimedActions.IDS.contains(id) ? PowerKind.AIMED : PowerKind.INSTANT;
            boolean on = lasting && handler.isActionActive(id);
            int left = 0;
            int total = 0;
            if (handler.isActionOnCooldown(action)) {
                total = Math.max(1, action.getCooldown(fp));
                left = Math.max(1, (int) Math.ceil(-handler.getPercentageForAction(action) * total));
            }
            LockReason lock = LockReason.NONE;
            if (!on) {
                IAction.PERM perm = action.canUse(fp);
                if (perm == IAction.PERM.DISALLOWED || perm == IAction.PERM.DISABLED || perm == IAction.PERM.PERMISSION_DISALLOWED) {
                    lock = LockReason.REFUSED_NOW;
                }
            }
            out.add(new WheelEntry(new PowerRef(sourceOf(action, id), id), PowerGroup.MAIN, kind, action.getName(), Component.empty(), List.of(),
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/actions/" + id.getPath() + ".png"),
                    new PowerState(on, 0L, 0, lock, 0, 1).withCooldown(WheelClocks.of(p).observe(new PowerRef(sourceOf(action, id), id).key(), WheelClocks.now(p), left, 3, total)),
                    true, -1f));
        }
        if (fp.getFaction() == VReference.VAMPIRE_FACTION && VampirismVision.available()) {
            boolean active = VampirismVision.active(p);
            out.add(new WheelEntry(new PowerRef(PowerSource.VAMPIRISM, VampirismVision.ID), PowerGroup.MAIN, PowerKind.INSTANT,
                    Component.translatable("supernaturalbloodlines.wheel.vision"), VampirismVision.activeName(p), List.of(), VampirismVision.ICON,
                    new PowerState(active, 0L, 0, LockReason.NONE, 0, 1), false, -1f));
        }
        return out;
    }

    static PowerSource sourceOf(IAction<?> action, ResourceLocation id) {
        String ns = action.getFaction().map(f -> f.getID().getNamespace()).orElse(id.getNamespace());
        return "werewolves".equals(ns) ? PowerSource.WEREWOLVES : PowerSource.VAMPIRISM;
    }
}
