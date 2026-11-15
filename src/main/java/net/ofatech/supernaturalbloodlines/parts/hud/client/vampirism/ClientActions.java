package net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;
import org.jetbrains.annotations.Nullable;

/**
 * The client's view of Vampirism's and Werewolves' actions for the HUD. Linked to Vampirism: reached only after {@code ModList.isLoaded("vampirism")},
 * and only from the client. The client's action handler is synced by Vampirism itself (its own overlays read it).
 */
public final class ClientActions {
    private ClientActions() {
    }

    public static Optional<? extends IFactionPlayer<?>> current(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer();
    }

    @Nullable
    public static IAction<?> byId(ResourceLocation id) {
        return VampirismRegistries.ACTION.get().get(id);
    }

    /** Where Vampirism and Werewolves keep an action's icon: {@code <namespace>:textures/actions/<path>.png}. */
    public static ResourceLocation icon(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/actions/" + id.getPath() + ".png");
    }

    /** Every action of the player's faction that the select wheel would show, as entries for the hub's Loadout tab. */
    public static List<LoadoutAccess.Entry> entries() {
        Player p = Minecraft.getInstance().player;
        List<LoadoutAccess.Entry> list = new ArrayList<>();
        if (p == null) {
            return list;
        }
        current(p).ifPresent(fp -> {
            for (Object o : fp.getActionHandler().getUnlockedActions()) {
                IAction<?> action = (IAction<?>) o;
                ResourceLocation id = VampirismRegistries.ACTION.get().getKey(action);
                if (id != null && action.showInSelectAction(p)) {
                    list.add(new LoadoutAccess.Entry(id, action.getName(), icon(id), null, null));
                }
            }
        });
        return list;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean unlocked(IFactionPlayer<?> fp, IAction<?> action) {
        return action.matchesFaction(fp.getFaction()) && ((IActionHandler) fp.getActionHandler()).isActionUnlocked(action);
    }
}
