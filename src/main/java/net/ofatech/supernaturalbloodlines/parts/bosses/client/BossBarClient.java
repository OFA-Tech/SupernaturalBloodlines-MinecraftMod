package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarClientHook;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitClient;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiMotion;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * Our boss bars on the client. The server tells what vanilla's packet cannot ({@link BossBarInfoPayload}); here every bar of ours is cancelled and drawn as one stack
 * at the first of them: the first carries the whole stack's height as its increment and the others carry none, so another mod's vanilla bar starts under our stack and
 * vanilla's rule (stop once a bar would start below a third of the screen) cannot take our second and third cards away. Client only.
 */
public final class BossBarClient {
    /** More than this many of our cards at once is not drawn as cards (the rest stay vanilla's). */
    static final int MAX_BARS = 6;
    private static final Map<UUID, BossBarInfo> INFOS = new HashMap<>();
    private static final LerpingBossEvent[] EVENTS = new LerpingBossEvent[MAX_BARS];
    private static final BossBarInfo[] COLLECTED = new BossBarInfo[MAX_BARS];
    private static int count;
    private static int shown;
    private static GuiGraphics frame;
    private static String demoShown;
    private static Boolean statusCorner;
    private static final BiConsumer<UUID, LerpingBossEvent> COLLECT = (id, event) -> {
        BossBarInfo info = INFOS.get(id);
        if (info != null && count < MAX_BARS) {
            EVENTS[count] = event;
            COLLECTED[count] = info;
            count++;
        }
    };

    private BossBarClient() {
    }

    public static void register() {
        BossBarClientHook.install(BossBarClient::accept);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CustomizeGuiOverlayEvent.BossEventProgress.class, BossBarClient::onBar);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> clear());
        KitClient.onReload(BossBarClient::reload);
    }

    static void accept(BossBarInfoPayload p) {
        BossBarInfo known = INFOS.get(p.bar());
        double now = UiMotion.tick();
        if (known == null) {
            INFOS.put(p.bar(), new BossBarInfo(p, now));
        } else {
            known.apply(p, now, false);
        }
    }

    static void forget(UUID bar) {
        INFOS.remove(bar);
    }

    static void clear() {
        INFOS.clear();
        frame = null;
        count = 0;
        shown = 0;
        demoShown = null;
    }

    static void reload() {
        INFOS.values().forEach(BossBarInfo::dropCaches);
        BossPortraitTex.reload();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        String want = net.ofatech.supernaturalbloodlines.shared.hud.DevBossBars.demo;
        if (!java.util.Objects.equals(want, demoShown)) {
            BossBarDemo.show(mc, want, demoShown);
            demoShown = want;
        }
        if (mc.gui.getGuiTicks() % 100 == 0) {
            // a bar that is gone from the overlay (out of range, the boss gone) takes what we knew of it with it
            var events = mc.gui.getBossOverlay().events;
            INFOS.keySet().removeIf(id -> !events.containsKey(id));
        }
    }

    private static void onBar(CustomizeGuiOverlayEvent.BossEventProgress e) {
        LerpingBossEvent event = e.getBossEvent();
        if (!INFOS.containsKey(event.getId())) {
            return; // another mod's bar, or one whose facts have not come: vanilla draws it
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        if (g != frame) {
            // the first of ours this frame: find all of ours in the overlay's order, and draw the whole stack here
            frame = g;
            count = 0;
            mc.gui.getBossOverlay().events.forEach(COLLECT);
            shown = Math.min(count, BossBarLayout.barsThatFit(g.guiHeight(), e.getY()));
            drawStack(g, mc, e, e.getY());
        }
        int index = -1;
        for (int k = 0; k < shown; k++) {
            if (EVENTS[k] == event) {
                index = k;
                break;
            }
        }
        if (index < 0) {
            return; // beyond what fits: vanilla's bar
        }
        e.setCanceled(true);
        e.setIncrement(index == 0 ? BossBarLayout.increment(shown) : 0);
    }

    private static void drawStack(GuiGraphics g, Minecraft mc, CustomizeGuiOverlayEvent.BossEventProgress e, int base) {
        HudKit kit = HudKit.of(HudStyle.current());
        boolean reduce = ReduceMotion.on();
        long tick = UiMotion.tick();
        double now = tick + e.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (statusCorner == null) {
            statusCorner = net.neoforged.fml.ModList.get().isLoaded("tensura") || net.neoforged.fml.ModList.get().isLoaded("vampirism");
        }
        int cardW = BossBarLayout.cardWidth(g.guiWidth(), statusCorner);
        int x = BossBarLayout.cardX(g.guiWidth(), cardW, statusCorner);
        for (int k = 0; k < shown; k++) {
            BossBarInfo info = COLLECTED[k];
            LerpingBossEvent event = EVENTS[k];
            info.prepare(mc.font, cardW, event.getName());
            float progress = info.health.value(now, reduce);
            BossBarView.draw(g, mc.font, kit, info, x, BossBarLayout.cardY(base, k), tick, now, reduce, progress);
        }
    }
}
