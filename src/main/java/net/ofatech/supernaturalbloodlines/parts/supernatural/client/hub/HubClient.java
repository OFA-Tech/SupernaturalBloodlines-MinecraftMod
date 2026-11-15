package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotRequestPayload;

/** Client only: opens the hub and keeps the last snapshot the server sent (it is asked for on every open, never kept in sync). */
public final class HubClient {
    private static volatile HubSnapshotPayload snapshot = HubSnapshotPayload.EMPTY;
    private static HubPage lastPage = HubPage.PATH;

    private HubClient() {
    }

    public static void receive(HubSnapshotPayload payload) {
        snapshot = payload;
    }

    public static HubSnapshotPayload snapshot() {
        return snapshot;
    }

    static void remember(HubPage page) {
        lastPage = page;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        snapshot = HubSnapshotPayload.EMPTY; // never show another world's names while the answer is on its way
        if (mc.getConnection().hasChannel(HubSnapshotRequestPayload.TYPE)) {
            PacketDistributor.sendToServer(HubSnapshotRequestPayload.INSTANCE);
        }
        mc.setScreen(new HubScreen(lastPage));
    }
}
