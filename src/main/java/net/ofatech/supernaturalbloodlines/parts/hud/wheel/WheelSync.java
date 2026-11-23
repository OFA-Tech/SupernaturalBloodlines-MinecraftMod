package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.wheel.ChangeGate;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelCataloguePayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/**
 * Sends the catalogue to a player who has the wheel open, once on open and again only when it changed (checked every {@code wheel_catalogue_interval_ticks}, only for viewers).
 * A closed wheel is never built. A logout, respawn or dimension change closes the wheel. With {@code wheel_enabled} off an open answers an empty list.
 */
public final class WheelSync {
    private static final class Viewer {
        final ChangeGate gate = new ChangeGate();
        int revision;
    }

    private static final Map<UUID, Viewer> VIEWERS = new ConcurrentHashMap<>();
    private static BiConsumer<ServerPlayer, WheelCataloguePayload> sink = (p, payload) -> PacketDistributor.sendToPlayer(p, payload);

    private WheelSync() {
    }

    /** Test seam: where the payloads go (the default is the player's connection). */
    public static void setSink(BiConsumer<ServerPlayer, WheelCataloguePayload> s) {
        sink = s;
    }

    public static void resetSink() {
        sink = (p, payload) -> PacketDistributor.sendToPlayer(p, payload);
    }

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
                tick(p);
            }
        });
    }

    public static boolean viewing(ServerPlayer p) {
        return VIEWERS.containsKey(p.getUUID());
    }

    public static void open(ServerPlayer p) {
        Viewer v = new Viewer();
        VIEWERS.put(p.getUUID(), v);
        if (!HudConfig.wheelEnabled()) {
            v.gate.offer(List.of().hashCode());
            sink.accept(p, new WheelCataloguePayload(++v.revision, List.of()));
            return;
        }
        push(p, v);
    }

    public static void close(ServerPlayer p) {
        VIEWERS.remove(p.getUUID());
    }

    public static void tick(ServerPlayer p) {
        Viewer v = VIEWERS.get(p.getUUID());
        if (v == null || !HudConfig.wheelEnabled() || p.tickCount % HudConfig.wheelCatalogueIntervalTicks() != 0) {
            return;
        }
        push(p, v);
    }

    private static void push(ServerPlayer p, Viewer v) {
        List<WheelEntry> entries = WheelCatalogue.build(p);
        if (v.gate.offer(entries.hashCode())) {
            sink.accept(p, new WheelCataloguePayload(++v.revision, entries));
        }
    }
}
