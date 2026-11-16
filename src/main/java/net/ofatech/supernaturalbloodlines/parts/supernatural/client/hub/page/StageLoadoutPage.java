package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.hud.LoadoutTabModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;

/**
 * Loadout for the Blood Moon and Tensura: "Your hand", six round slots with their keys (R F V / Z X C, left and right of the hotbar), and "Abilities you
 * know" as a grid of icon tiles. Pick an ability then a slot, or a slot then an ability ({@link LoadoutTabModel}). The server checks what is sent.
 */
public final class StageLoadoutPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "skills.loadout.";
    private final LoadoutTabModel model = new LoadoutTabModel();

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        BookKit.moonHead(f, Component.translatable(PageKit.HUB + "page.loadout"), Component.translatable(PageKit.HUB + "loadout.sub"));
        Rect r = f.region(BookKit.FULL);
        int y = f.begin(BookKit.FULL);
        if (!LoadoutTab.available()) {
            f.end(PageKit.wrapped(f, Component.translatable(PageKit.HUB + "loadout.off"), r.x(), y, r.w(), t.muted()));
            return;
        }
        List<LoadoutAccess.Entry> entries = LoadoutAccess.assignable();
        Map<ResourceLocation, LoadoutAccess.Entry> byId = new HashMap<>();
        entries.forEach(e -> byId.put(e.id(), e));
        List<Optional<ResourceLocation>> slots = LoadoutAccess.get();
        y = BookKit.caps(f, Component.translatable(PageKit.HUB + "loadout.hand"), r.x(), y, t.muted());
        int tile = 36;
        int gap = 8;
        for (int i = 0; i < Loadout.SLOTS && i < slots.size(); i++) {
            Rect box = new Rect(r.x() + i * (tile + gap), y + 2, tile, tile);
            slot(f, box, i, slots.get(i), byId);
        }
        y += tile + 22;
        y = PageKit.wrapped(f, Component.translatable(KEY + "keys"), r.x(), y, r.w(), t.muted()) + 6;
        y = BookKit.caps(f, Component.translatable(PageKit.HUB + "loadout.known"), r.x(), y, t.muted());
        int cols = Math.max(1, r.w() / (tile + gap + 12));
        int col = 0;
        int rowTop = y + 2;
        for (LoadoutAccess.Entry e : entries) {
            Rect box = new Rect(r.x() + col * (tile + gap + 12), rowTop, tile, tile);
            known(f, box, e);
            if (++col == cols) {
                col = 0;
                rowTop += tile + 22;
            }
        }
        if (entries.isEmpty()) {
            rowTop = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), rowTop, r.w(), t.muted());
        } else if (col != 0) {
            rowTop += tile + 22;
        }
        f.end(rowTop);
        detail(f, entries, byId, slots);
    }

    private void slot(HubScreen.Frame f, Rect box, int i, Optional<ResourceLocation> id, Map<ResourceLocation, LoadoutAccess.Entry> byId) {
        HubTheme t = f.theme();
        boolean picked = Integer.valueOf(i).equals(model.pickedSlot());
        BookKit.outline(f.g, box, t.paper(), picked ? t.accent() : (t.muted() & 0xFFFFFF) | 0x88000000);
        Component label = Component.translatable(KEY + "empty");
        if (id.isPresent()) {
            LoadoutAccess.Entry e = byId.get(id.get());
            label = e == null ? Component.literal(id.get().toString()) : e.name();
            ClientIcons.draw(f.g, e == null ? id.get() : e.icon(), box.x() + (box.w() - 16) / 2, box.y() + (box.h() - 16) / 2, 16, e == null ? 0.4f : 1f);
        } else {
            f.g.drawString(f.font(), "+", box.x() + box.w() / 2 - 2, box.y() + box.h() / 2 - 4, t.muted(), false);
        }
        f.g.drawString(f.font(), LoadoutAccess.keyLabel(i), box.x() + 3, box.y() + 3, t.accent(), false);
        String name = f.font().plainSubstrByWidth(label.getString(), box.w() + 6);
        f.g.drawString(f.font(), name, box.x() + (box.w() - f.font().width(name)) / 2, box.bottom() + 3, picked ? t.accent() : t.ink(), false);
        String target = "loadout:slot:" + i;
        f.target(target, box, List.of(label), () -> apply(model.pickSlot(i)));
        if (f.focused(target)) {
            KitDraw.focusRing(f.g, box, t.focusColor());
        }
    }

    private void known(HubScreen.Frame f, Rect box, LoadoutAccess.Entry e) {
        HubTheme t = f.theme();
        boolean picked = e.id().equals(model.pickedAction());
        boolean grey = e.greyedReason() != null;
        BookKit.outline(f.g, box, t.paper(), picked ? t.accent() : (t.muted() & 0xFFFFFF) | 0x66000000);
        ClientIcons.draw(f.g, e.icon(), box.x() + (box.w() - 16) / 2, box.y() + (box.h() - 16) / 2, 16, grey ? 0.4f : 1f);
        String name = f.font().plainSubstrByWidth(e.name().getString(), box.w() + 10);
        f.g.drawString(f.font(), name, box.x() + (box.w() - f.font().width(name)) / 2, box.bottom() + 3, grey ? t.muted() : t.ink(), false);
        List<Component> tip = new java.util.ArrayList<>(List.of(e.name()));
        if (e.note() != null) {
            tip.add(e.note());
        }
        if (grey) {
            tip.add(Component.translatable(e.greyedReason()));
        }
        String target = "loadout:action:" + e.id();
        f.target(target, box, tip, () -> apply(model.pickAction(e.id())));
        if (f.focused(target)) {
            KitDraw.focusRing(f.g, box, t.focusColor());
        }
    }

    private void detail(HubScreen.Frame f, List<LoadoutAccess.Entry> entries, Map<ResourceLocation, LoadoutAccess.Entry> byId, List<Optional<ResourceLocation>> slots) {
        Integer slot = model.pickedSlot();
        ResourceLocation action = model.pickedAction();
        Component title = slot != null ? Component.translatable(KEY + "slot", LoadoutAccess.keyLabel(slot)) : action != null && byId.get(action) != null ? byId.get(action).name()
                : Component.translatable(PageKit.HUB + "page.loadout");
        Component body = slot != null ? Component.translatable(KEY + "picked_slot") : action != null ? Component.translatable(KEY + "picked") : Component.translatable(KEY + "hint");
        Detail.Builder d = Detail.of(title).body(body);
        boolean full = slot != null && slot < slots.size() && slots.get(slot).isPresent();
        d.action(new Detail.Action("clear", Component.translatable(KEY + "clear"), false, full, null,
                new Detail.Confirm(Component.translatable(KEY + "clear_title", slot == null ? "" : LoadoutAccess.keyLabel(slot)), Component.translatable(KEY + "clear_body")), () -> apply(model.clearSlot(slot))));
        f.detail(d.build());
    }

    private static void apply(Optional<LoadoutTabModel.Assignment> assignment) {
        assignment.ifPresent(a -> LoadoutAccess.set(a.slot(), a.id()));
    }
}
