package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarClientHook;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossHitPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitClient;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiMotion;
import net.ofatech.supernaturalbloodlines.shared.hud.DevBossBars;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * Our boss bars on the client, as one row: the focus boss as the full card and every other boss as a token beside it ({@link BossRow}). The server tells what vanilla's packet
 * cannot ({@link BossBarInfoPayload}, {@link BossHitPayload}); here every bar of ours is cancelled and the row is drawn once, at the first of them, which tells vanilla
 * that the row is one card tall whatever the number of bosses, so another mod's bar that comes after ours starts under it (one that came first stays above, and pushes the row down) and vanilla's rule (stop once a bar would start below a third of the
 * screen) cannot take any of ours away. The focus is chosen once a tick ({@link BossFocus}). Narration and a hover tooltip list every boss. Client only.
 */
public final class BossBarClient {
    /** More than this many of our bars at once are not drawn as a row (there are nine bosses). */
    static final int MAX_BARS = 16;
    private static final Map<UUID, BossBarInfo> INFOS = new HashMap<>();
    private static final BossBarInfo[] ORDER = new BossBarInfo[MAX_BARS];
    private static final List<BossBarInfo> TOKENS = new ArrayList<>(MAX_BARS);
    private static int count;
    private static GuiGraphics frame;
    private static String demoShown;
    private static Boolean statusCorner;

    private static UUID focusId;
    private static long focusSince;
    private static UUID prevFocusId;
    private static double fadeSince = Double.NEGATIVE_INFINITY;

    private static BossRow.Plan plan;
    private static long planKey = -1;
    private static BossRow.Plan lastPlan;
    private static int lastRowY;
    private static long lastDrawn = Long.MIN_VALUE;
    private static int plusHidden = -1;
    private static String plusText = "";
    private static int plusW;
    private static NarrationGate.State spoken;

    private static final BiConsumer<UUID, LerpingBossEvent> COLLECT = (id, event) -> {
        BossBarInfo info = INFOS.get(id);
        if (info != null && count < MAX_BARS) {
            info.event = event;
            ORDER[count++] = info;
        }
    };

    private BossBarClient() {
    }

    public static void register() {
        BossBarClientHook.install(BossBarClient::accept);
        BossBarClientHook.installHits(hit -> hit(hit.bar()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CustomizeGuiOverlayEvent.BossEventProgress.class, BossBarClient::onBar);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> clear());
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, BossBarClient::tooltip);
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

    /** This player has just hit the boss with this bar: its card is the focus from the next tick. */
    static void hit(UUID bar) {
        BossBarInfo info = INFOS.get(bar);
        if (info != null) {
            info.lastHit = UiMotion.tick();
        }
    }

    static void forget(UUID bar) {
        INFOS.remove(bar);
    }

    static void clear() {
        INFOS.clear();
        java.util.Arrays.fill(ORDER, null);
        TOKENS.clear();
        count = 0;
        frame = null;
        demoShown = null;
        focusId = null;
        prevFocusId = null;
        lastPlan = null;
        spoken = null;
        planKey = -1;
    }

    static void reload() {
        INFOS.values().forEach(BossBarInfo::dropCaches);
        BossPortraitTex.reload();
        planKey = -1;
        plusHidden = -1;
    }

    private static void collect(Minecraft mc) {
        java.util.Arrays.fill(ORDER, 0, count, null);
        count = 0;
        mc.gui.getBossOverlay().events.forEach(COLLECT);
    }

    private static BossBarInfo find(UUID id) {
        for (int k = 0; k < count; k++) {
            if (ORDER[k].bar.equals(id)) {
                return ORDER[k];
            }
        }
        return null;
    }

    /** The focus boss: the chosen one, or the first while none has been chosen yet. */
    private static BossBarInfo focus() {
        BossBarInfo f = focusId == null ? null : find(focusId);
        return f != null ? f : ORDER[0];
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        String want = DevBossBars.demo;
        if (!Objects.equals(want, demoShown)) {
            BossBarDemo.show(mc, want, demoShown);
            demoShown = want;
        }
        long now = UiMotion.tick();
        UUID demoFocus = BossBarDemo.focus();
        if (demoFocus != null) {
            hit(demoFocus); // a demo has no player to hit anything
        }
        collect(mc);
        if (count == 0) {
            focusId = null;
            return;
        }
        List<BossFocus.Candidate> candidates = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            BossBarInfo info = ORDER[k];
            var entity = info.entity < 0 ? null : mc.level.getEntity(info.entity);
            double dist = entity == null || mc.player == null ? BossFocus.UNKNOWN : mc.player.distanceToSqr(entity);
            candidates.add(new BossFocus.Candidate(info.bar, dist, info.lastHit));
        }
        UUID chosen = BossFocus.choose(candidates, focusId, focusSince, now);
        if (!Objects.equals(chosen, focusId)) {
            prevFocusId = focusId;
            focusId = chosen;
            focusSince = now;
            fadeSince = now;
        }
        if (mc.gui.getGuiTicks() % 100 == 0) {
            // a bar that is gone from the overlay (out of range, the boss gone) takes what we knew of it with it
            var events = mc.gui.getBossOverlay().events;
            INFOS.keySet().removeIf(id -> !events.containsKey(id));
        }
        narrate(mc, now);
    }

    private static void onBar(CustomizeGuiOverlayEvent.BossEventProgress e) {
        LerpingBossEvent event = e.getBossEvent();
        if (!INFOS.containsKey(event.getId())) {
            return; // another mod's bar, or one whose facts have not come: vanilla draws it
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        if (g != frame) {
            // the first of ours this frame: find all of ours in the overlay's order, and draw the whole row here
            frame = g;
            collect(mc);
            if (count > 0) {
                drawRow(g, mc, e.getY(), e.getPartialTick().getGameTimeDeltaPartialTick(false));
            }
        }
        e.setCanceled(true);
        e.setIncrement(BossBarLayout.increment(count > 0 && ORDER[0].event == event));
    }

    private static void drawRow(GuiGraphics g, Minecraft mc, int y, float partial) {
        HudKit kit = HudKit.of(HudStyle.current());
        boolean reduce = ReduceMotion.on();
        long tick = UiMotion.tick();
        double now = tick + partial;
        if (statusCorner == null) {
            statusCorner = ModList.get().isLoaded("tensura") || ModList.get().isLoaded("vampirism");
        }
        BossBarInfo focus = focus();
        TOKENS.clear();
        for (int k = 0; k < count; k++) {
            if (ORDER[k] != focus) {
                TOKENS.add(ORDER[k]);
            }
        }
        int guiW = g.guiWidth();
        long key = (long) guiW << 20 | (long) TOKENS.size() << 1 | (statusCorner ? 1 : 0);
        if (key != planKey || plan == null) {
            plan = BossRow.plan(guiW, statusCorner, TOKENS.size());
            planKey = key;
        }

        // the card, cross-fading with the one it replaces for a moment
        float fade = BossBarMotion.focusFade(fadeSince, now, reduce);
        BossBarInfo prev = fade < 1f && prevFocusId != null ? find(prevFocusId) : null;
        if (prev != null && prev != focus) {
            prev.prepare(mc.font, plan.cardW(), prev.event.getName());
            BossBarView.draw(g, mc.font, kit, prev, plan.cardX(), y, tick, now, reduce, prev.health.value(now, reduce), 1f - fade);
        } else {
            prev = null;
        }
        focus.prepare(mc.font, plan.cardW(), focus.event.getName());
        BossBarView.draw(g, mc.font, kit, focus, plan.cardX(), y, tick, now, reduce, focus.health.value(now, reduce), prev != null ? fade : 1f);

        // the tokens
        for (BossRow.Slot slot : plan.slots()) {
            if (slot.index() == BossRow.PLUS) {
                if (plan.hidden() != plusHidden) {
                    plusHidden = plan.hidden();
                    plusText = "+" + plusHidden;
                    plusW = mc.font.width(plusText);
                }
                BossBarView.plus(g, mc.font, kit, plusText, plusW, slot.x(), y);
            } else {
                BossBarInfo t = TOKENS.get(slot.index());
                t.prepare(mc.font, plan.cardW(), t.event.getName());
                BossBarView.token(g, mc.font, kit, t, slot.x(), y, tick, reduce, t.health.value(now, reduce));
            }
        }
        lastPlan = plan;
        lastRowY = y;
        lastDrawn = tick;
    }

    /** What a boss's tooltip and narration say about it: its name, its health as a whole percent and, for a boss with forms, the form it is in. */
    private static BossNarration.Entry entry(BossBarInfo i) {
        String form = i.look.set().shownChips() > 0 ? I18n.get(i.look.set().chipKey(i.look.active())) : null;
        String name = i.event != null ? i.event.getName().getString() : "";
        return new BossNarration.Entry(name, BossNarration.percent(i.hp, i.max), form);
    }

    private static Component text(BossNarration.Line line) {
        return Component.translatable(line.key(), line.args().toArray());
    }

    /**
     * Says the row aloud: the whole row when who is in it changes, just the focus when only the focus changes and not within a hold ({@link NarrationGate}). It is decided from
     * the ids alone; names, health and forms are looked up (and translated) only for what is actually said.
     */
    private static void narrate(Minecraft mc, long now) {
        BossBarInfo focus = focus();
        if (focus == null) {
            spoken = null;
            return;
        }
        java.util.Set<UUID> set = new java.util.HashSet<>();
        for (int k = 0; k < count; k++) {
            set.add(ORDER[k].bar);
        }
        NarrationGate.Say say = NarrationGate.decide(spoken, set, focus.bar, now);
        if (say == NarrationGate.Say.NONE) {
            return;
        }
        spoken = NarrationGate.after(spoken, say, set, focus.bar, now);
        if (!mc.options.narrator().get().shouldNarrateSystem()) {
            return;
        }
        StringBuilder said = new StringBuilder();
        if (say == NarrationGate.Say.FOCUS) {
            said.append(text(BossNarration.focusOnly(entry(focus))).getString());
        } else {
            List<BossNarration.Entry> others = new ArrayList<>(count);
            for (int k = 0; k < count; k++) {
                if (ORDER[k] != focus) {
                    others.add(entry(ORDER[k]));
                }
            }
            for (BossNarration.Line line : BossNarration.row(entry(focus), others)) {
                said.append(text(line).getString()).append(". ");
            }
        }
        mc.getNarrator().sayNow(said.toString());
    }
    /** The tooltip of a token, where the pointer is free (chat, a menu): the boss, or every boss the "+N" token holds. */
    private static void tooltip(ScreenEvent.Render.Post e) {
        if (lastPlan == null || UiMotion.tick() - lastDrawn > 2) {
            return;
        }
        BossRow.Slot slot = BossRow.slotAt(lastPlan, lastRowY, e.getMouseX(), e.getMouseY());
        if (slot == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (slot.index() == BossRow.PLUS) {
            List<BossNarration.Entry> hidden = new ArrayList<>();
            for (int k = lastPlan.slots().size() - 1; k < TOKENS.size(); k++) {
                hidden.add(entry(TOKENS.get(k)));
            }
            BossNarration.hidden(hidden).forEach(line -> lines.add(text(line)));
        } else if (slot.index() < TOKENS.size()) {
            lines.add(text(BossNarration.one(entry(TOKENS.get(slot.index())), "token")));
        }
        if (!lines.isEmpty()) {
            e.getGuiGraphics().renderComponentTooltip(Minecraft.getInstance().font, lines, e.getMouseX(), e.getMouseY());
        }
    }
}
