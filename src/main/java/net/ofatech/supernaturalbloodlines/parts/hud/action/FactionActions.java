package net.ofatech.supernaturalbloodlines.parts.hud.action;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Vampirism's action API for a player's current faction (Vampirism, and Werewolves, whose player is an {@code IFactionPlayer} too). The only class
 * of the action code that links Vampirism: callers check {@code ModList.isLoaded("vampirism")} first. An action id is only a name, so everything
 * is looked up here and checked against what the player really has unlocked.
 */
public final class FactionActions {
    private FactionActions() {
    }

    @Nullable
    public static IAction<?> byId(ResourceLocation id) {
        return VampirismRegistries.ACTION.get().get(id);
    }

    @Nullable
    public static ResourceLocation idOf(IAction<?> action) {
        return VampirismRegistries.ACTION.get().getKey(action);
    }

    public static Optional<? extends IFactionPlayer<?>> current(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer();
    }

    /** True when the player's current faction has this action unlocked (an action of another faction never is). */
    public static boolean unlocked(Player p, ResourceLocation id) {
        IAction<?> action = byId(id);
        return action != null && current(p).map(fp -> unlocked(fp, action)).orElse(false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean unlocked(IFactionPlayer<?> fp, IAction<?> action) {
        return action.matchesFaction(fp.getFaction()) && ((IActionHandler) fp.getActionHandler()).isActionUnlocked(action);
    }

    /** The ids of every unlocked action that the select-action wheel would show, in Vampirism's own order. */
    public static List<ResourceLocation> unlockedIds(Player p) {
        List<ResourceLocation> ids = new ArrayList<>();
        current(p).ifPresent(fp -> {
            for (Object a : fp.getActionHandler().getUnlockedActions()) {
                IAction<?> action = (IAction<?>) a;
                ResourceLocation id = idOf(action);
                if (id != null && action.showInSelectAction(p)) {
                    ids.add(id);
                }
            }
        });
        return ids;
    }

    /** Toggles the action like Vampirism's own key does. Returns the refusal (a {@code PERM} name) or null when it was allowed. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public static IAction.PERM toggle(ServerPlayer p, ResourceLocation id, IAction.ActivationContext ctx) {
        IAction action = byId(id);
        Optional<? extends IFactionPlayer<?>> fp = current(p);
        if (action == null || fp.isEmpty() || !unlocked(fp.get(), action)) {
            return IAction.PERM.NOT_UNLOCKED;
        }
        IAction.PERM perm = ((IActionHandler) fp.get().getActionHandler()).toggleAction(action, ctx);
        return perm == IAction.PERM.ALLOWED ? null : perm;
    }
}
