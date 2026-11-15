package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
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
        return player == null ? Loadout.DEFAULT : player.getData(ModAttachments.HUD_LOADOUT);
    }

    @Override
    public List<Optional<ResourceLocation>> slots() {
        return loadout().slots();
    }

    @Override
    public List<LoadoutAccess.Entry> assignable() {
        List<LoadoutAccess.Entry> list = new ArrayList<>();
        list.add(new LoadoutAccess.Entry(Loadout.BITE, Component.translatable("supernaturalbloodlines.hud.bite.name"), BITE_ICON, BiteView.note(), BiteView.greyed()));
        if (VAMPIRISM) {
            list.addAll(ClientActions.entries());
        }
        return list;
    }

    @Override
    public void set(int slot, @Nullable ResourceLocation id) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new SetLoadoutPayload(slot, id == null ? "" : id.toString()));
        }
    }

    @Override
    public String keyLabel(int slot) {
        return HudKeys.SLOTS[slot].getTranslatedKeyMessage().getString();
    }
}
