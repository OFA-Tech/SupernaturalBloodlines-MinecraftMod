package net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism;

import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.ActionCooldown;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.net.ActionCooldowns;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.parts.hud.client.BiteView;
import net.ofatech.supernaturalbloodlines.parts.hud.client.ClientLoadout;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;

/**
 * Supernatural mode: the six slots come from the player's loadout (what the server last sent), and everything the HUD shows about an action comes straight
 * from Vampirism's own client handler on every tick (cooldown, running, whether it can be used): nothing is stored twice. Bite is our own pseudo action.
 * Linked to Vampirism: reached only after {@code ModList.isLoaded("vampirism")}.
 */
public final class ActionSlots {
    public static final String GREY_LOCKED = "supernaturalbloodlines.hud.slot.locked";
    public static final String GREY_MISSING = "supernaturalbloodlines.hud.slot.action_missing";
    private static final String ACTION = "supernaturalbloodlines.hud.action.";

    private ActionSlots() {
    }

    public static void refresh(LocalPlayer p, SlotView[] out) {
        Loadout loadout = ClientLoadout.loadout();
        for (int i = 0; i < out.length; i++) {
            out[i] = view(p, loadout.get(i).orElse(null), HudKeys.SLOTS[i].getTranslatedKeyMessage().getString());
        }
    }

    /** An action another part owns (the Alpha Roar): its own icon and name, locked when the player may no longer use it, and the cooldown the server announced. */
    private static SlotView extraView(LocalPlayer p, ExtraActions.Action a, String key) {
        var level = p.level();
        int total = ActionCooldowns.total(a.id());
        int left = ActionCooldowns.left(a.id(), level.getGameTime());
        return new SlotView(a.icon(), key, ActionCooldown.fraction(left, total), left > 0 ? ActionCooldown.seconds(left) : 0, false,
                a.available(p) ? null : GREY_LOCKED, a.name().getString(), a.note().getString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SlotView view(LocalPlayer p, ResourceLocation id, String key) {
        if (id == null) {
            return SlotView.EMPTY.withKey(key);
        }
        if (Loadout.BITE.equals(id)) {
            return BiteView.slot(key);
        }
        var extra = ExtraActions.get(id);
        if (extra.isPresent()) {
            return extraView(p, extra.get(), key);
        }
        IAction action = ClientActions.byId(id);
        ResourceLocation icon = ClientActions.icon(id);
        if (action == null) {
            return new SlotView(icon, key, 0f, 0, false, GREY_MISSING, id.toString(), null);
        }
        String name = action.getName().getString();
        Optional<? extends IFactionPlayer<?>> current = ClientActions.current(p);
        if (current.isEmpty() || !ClientActions.unlocked(current.get(), action)) {
            return new SlotView(icon, key, 0f, 0, false, GREY_LOCKED, name, null).withDescription(description(id));
        }
        IFactionPlayer fp = current.get();
        IActionHandler handler = fp.getActionHandler();
        boolean active = handler.isActionActive(id);
        boolean onCooldown = handler.isActionOnCooldown(action);
        float fraction = onCooldown ? Math.min(1f, handler.getPercentageForAction(action)) : 0f;
        int seconds = onCooldown ? (int) Math.ceil(action.getCooldown(fp) * fraction / 20f) : 0;
        IAction.PERM perm = action.canUse(fp);
        boolean blocked = !active && perm != IAction.PERM.ALLOWED && perm != IAction.PERM.COOLDOWN;
        return new SlotView(icon, key, fraction, seconds, active, blocked ? ACTION + perm.name().toLowerCase(Locale.ROOT) : null, name, null).withDescription(description(id));
    }

    /** Vampirism's (and Werewolves') one-line description of an action, {@code action.<namespace>.<path>.desc}; null when the mod has none. */
    @org.jetbrains.annotations.Nullable
    static String description(ResourceLocation id) {
        String key = "action." + id.getNamespace() + "." + id.getPath() + ".desc";
        return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : null;
    }
}
