package net.ofatech.supernaturalbloodlines.parts.hud.action;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
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

    /**
     * Toggles the action like Vampirism's own key does, with a context built here: the block the player looks at, and the creature the client's crosshair
     * named if it is in the same level, within {@code bite_reach} and in sight.
     *
     * @return the refusal (a lower case {@code PERM} name), null when it was allowed
     */
    @Nullable
    public static String toggleFor(ServerPlayer p, ResourceLocation id, int targetEntityId) {
        Optional<BlockPos> block = Optional.empty();
        HitResult hit = p.pick(Math.min(HudConfig.biteReach(), 5.0), 1f, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            block = Optional.of(b.getBlockPos());
        }
        Optional<Entity> entity = Optional.empty();
        Entity found = targetEntityId < 0 ? null : p.level().getEntity(targetEntityId);
        if (found instanceof LivingEntity living && living.isAlive() && living != p && p.distanceTo(living) <= HudConfig.biteReach() && p.hasLineOfSight(living)) {
            entity = Optional.of(living);
        }
        IAction.PERM perm = toggle(p, id, new Context(block, entity));
        return perm == null ? null : perm.name().toLowerCase(Locale.ROOT);
    }

    private record Context(Optional<BlockPos> targetBlock, Optional<Entity> targetEntity) implements IAction.ActivationContext {
    }

    /** Toggles the action like Vampirism's own key does. Returns the refusal (a {@code PERM}) or null when it was allowed. */
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
