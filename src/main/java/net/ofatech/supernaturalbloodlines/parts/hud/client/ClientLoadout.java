package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.ClientActions;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetLoadoutPayload;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;
import org.jetbrains.annotations.Nullable;

/** What the hub's Loadout tab sees (installed as {@link LoadoutAccess}): the loadout the server last sent, and a request to change it that the server checks. */
public final class ClientLoadout implements LoadoutAccess.Provider {
    public static final ResourceLocation BITE_ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/bite.png");

    private static final boolean VAMPIRISM = ModList.get().isLoaded("vampirism");

    public static Loadout loadout() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Loadout.DEFAULT;
        }
        // with the supernatural part off there is no hub to assign in: the empty slots show the first unlocked actions, the same rule the server uses
        return LoadoutService.needsFallback() ? LoadoutService.effective(player) : player.getData(ModAttachments.HUD_LOADOUT);
    }

    @Override
    public List<Optional<ResourceLocation>> slots() {
        return loadout().slots();
    }

    @Override
    public void hubOpened() {
        net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelActions.view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer.HUB, true);
    }

    @Override
    public void hubClosed() {
        net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelActions.view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer.HUB, false);
    }

    @Override
    public List<LoadoutAccess.Entry> assignable() {
        List<LoadoutAccess.Entry> list = new ArrayList<>();
        var catalogue = net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelList.current();
        if (!catalogue.isEmpty()) {
            // the wheel has been opened: the page offers what the wheel offers (one entry per power, rule C), not a second list
            var refs = net.ofatech.supernaturalbloodlines.core.wheel.WheelRules.loadoutPowers(catalogue.stream().map(e -> e.ref()).toList());
            for (var e : catalogue) {
                if (refs.contains(e.ref())) {
                    list.add(new LoadoutAccess.Entry(e.ref().id(), e.name(), e.icon(), null, e.state().locked() ? "supernaturalbloodlines.wheel.lock." + e.state().lock().name().toLowerCase(java.util.Locale.ROOT) : null));
                }
            }
            return list;
        }
        list.add(new LoadoutAccess.Entry(Loadout.BITE, Component.translatable("supernaturalbloodlines.hud.bite.name"), BITE_ICON, BiteView.note(), BiteView.greyed()));
        for (var a : net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.all()) {
            if (net.minecraft.client.Minecraft.getInstance().player != null && a.available(net.minecraft.client.Minecraft.getInstance().player)) {
                list.add(new LoadoutAccess.Entry(a.id(), a.name(), a.icon(), a.note(), null));
            }
        }
        if (VAMPIRISM) {
            list.addAll(ClientActions.entries());
        }
        return list;
    }

    @Override
    public void set(int slot, @Nullable ResourceLocation id) {
        HudSend.send(new SetLoadoutPayload(slot, id == null ? "" : id.toString()));
    }

    @Override
    public String keyLabel(int slot) {
        return HudKeys.SLOTS[slot].getTranslatedKeyMessage().getString();
    }
}
