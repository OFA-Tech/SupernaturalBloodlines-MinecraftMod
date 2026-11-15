package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * Client only: what Tensura gives the hub. Its live race and EP (from Tensura's own synced client data), a seal that opens Tensura's
 * status menu unchanged, and the hub's tab in Tensura's menu ({@link TensuraMenuTab}).
 */
public final class TensuraHubClient {
    private TensuraHubClient() {
    }

    public static void register() {
        HubHooks.setTensura(TensuraHubClient::info);
        HubHooks.addLauncher(new HubHooks.Launcher("tensura", "T", 0xFF9fd8e6, Component.translatable("supernaturalbloodlines.hub.launcher.tensura"),
                "io.github.manasmods.", () -> true, () -> ScreenHelper.openScreen(ScreenHelper.STATUS_MENU)));
        NeoForge.EVENT_BUS.addListener(TensuraMenuTab::onInit);
    }

    private static Optional<HubHooks.TensuraInfo> info() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        IExistence existence = TensuraStorages.getExistenceFrom(player);
        Component race = RaceAPI.getRaceFrom((LivingEntity) player).getRace().map(r -> (Component) r.getDisplayName())
                .orElse(Component.translatable("supernaturalbloodlines.hub.tensura_no_race"));
        return Optional.of(new HubHooks.TensuraInfo(race, existence.getEP(), existence.getMagicule()));
    }
}
