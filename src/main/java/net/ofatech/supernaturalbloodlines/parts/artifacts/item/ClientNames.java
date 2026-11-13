package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Client only: the name of an online player from the tab list. Touch it only when {@code FMLEnvironment.dist} is CLIENT. */
final class ClientNames {
    private ClientNames() {
    }

    /** Null when that player is not online. */
    static String of(UUID id) {
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
        return info == null ? null : info.getProfile().getName();
    }
}
