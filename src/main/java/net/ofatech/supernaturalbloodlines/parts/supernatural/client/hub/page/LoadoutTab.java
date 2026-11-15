package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.hud.LoadoutTabModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;

/**
 * The Loadout tab of the Skills page: where the ability HUD's six Supernatural-mode slots are filled. It sees the HUD only through {@link LoadoutAccess}
 * (parts never import each other), so it is shown only when the HUD part is installed. The left page lists the actions the player can put in a slot (Bite
 * first, with what it does for their form); the right page shows the six slots in HUD order with their keys. Pick an action and then a slot, or a slot
 * and then an action ({@link LoadoutTabModel}); each slot has a clear mark. The server checks what is sent.
 */
final class LoadoutTab {
    private static final String KEY = PageKit.HUB + "skills.loadout.";
    private static final int ROW = 18;

    private final LoadoutTabModel model = new LoadoutTabModel();

    static boolean available() {
        return LoadoutAccess.available();
    }

    void reset() {
        model.reset();
    }

    void render(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        List<LoadoutAccess.Entry> entries = LoadoutAccess.assignable();
        y = PageKit.wrapped(f, Component.translatable(KEY + "hint"), r.x(), y, r.w(), t.ink()) + 2;
        if (model.pickedAction() != null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "picked"), r.x(), y, r.w(), t.accent());
        } else if (model.pickedSlot() != null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "picked_slot"), r.x(), y, r.w(), t.accent());
        }
        y += 2;
        for (LoadoutAccess.Entry e : entries) {
            y = action(f, r, y, e);
        }
        if (entries.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);
        slots(f, entries);
    }

    private int action(HubScreen.Frame f, Rect r, int y, LoadoutAccess.Entry e) {
        HubTheme t = f.theme();
        Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, ROW);
        boolean picked = e.id().equals(model.pickedAction());
        if (picked) {
            f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
        }
        boolean grey = e.greyedReason() != null;
        ClientIcons.draw(f.g, e.icon(), r.x(), y, 16, grey ? 0.4f : 1f);
        String text = f.font().plainSubstrByWidth(e.name().getString(), r.w() - 22);
        f.g.drawString(f.font(), text, r.x() + 20, y + 4, grey ? t.muted() : t.ink(), false);
        List<Component> tip = new java.util.ArrayList<>(List.of(e.name()));
        if (e.note() != null) {
            tip.add(e.note());
        }
        if (grey) {
            tip.add(Component.translatable(e.greyedReason()));
        }
        String id = "loadout:action:" + e.id();
        f.target(id, hit, tip, () -> apply(model.pickAction(e.id())));
        if (f.focused(id)) {
            t.drawFocus(f.g, hit);
        }
        return y + ROW + 1;
    }

    private void slots(HubScreen.Frame f, List<LoadoutAccess.Entry> entries) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.SIDE);
        int y = f.begin(HubLayout.SIDE);
        y = PageKit.title(f, Component.translatable(KEY + "slots"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        Map<ResourceLocation, LoadoutAccess.Entry> byId = new HashMap<>();
        entries.forEach(e -> byId.put(e.id(), e));
        List<Optional<ResourceLocation>> slots = LoadoutAccess.get();
        for (int i = 0; i < Loadout.SLOTS && i < slots.size(); i++) {
            y = slot(f, r, y, i, slots.get(i), byId);
        }
        f.end(y);
    }

    private int slot(HubScreen.Frame f, Rect r, int y, int i, Optional<ResourceLocation> id, Map<ResourceLocation, LoadoutAccess.Entry> byId) {
        HubTheme t = f.theme();
        Rect hit = new Rect(r.x() - 1, y - 1, r.w() - 14, ROW);
        boolean picked = Integer.valueOf(i).equals(model.pickedSlot());
        if (picked) {
            f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
        }
        String key = LoadoutAccess.keyLabel(i);
        f.g.drawString(f.font(), f.font().plainSubstrByWidth(key, 22), r.x(), y + 4, t.accent(), false);
        Component label = Component.translatable(KEY + "empty");
        int color = t.muted();
        if (id.isPresent()) {
            LoadoutAccess.Entry e = byId.get(id.get());
            label = e == null ? Component.literal(id.get().toString()) : e.name();
            color = e == null || e.greyedReason() != null ? t.muted() : t.ink();
            ClientIcons.draw(f.g, e == null ? id.get() : e.icon(), r.x() + 24, y, 16, e == null ? 0.4f : 1f);
        }
        f.g.drawString(f.font(), f.font().plainSubstrByWidth(label.getString(), r.w() - 70), r.x() + 44, y + 4, color, false);
        String slotId = "loadout:slot:" + i;
        f.target(slotId, hit, List.of(label), () -> apply(model.pickSlot(i)));
        if (f.focused(slotId)) {
            t.drawFocus(f.g, hit);
        }
        if (id.isPresent()) {
            Rect clear = new Rect(r.right() - 12, y + 2, 12, 12);
            String clearId = "loadout:clear:" + i;
            f.g.drawString(f.font(), "x", clear.x() + 3, clear.y() + 2, f.hovered(clear) ? t.accent() : t.muted(), false);
            f.target(clearId, clear, List.of(Component.translatable(KEY + "clear")), () -> apply(model.clearSlot(i)));
            if (f.focused(clearId)) {
                t.drawFocus(f.g, clear);
            }
        }
        return y + ROW + 1;
    }

    private static void apply(Optional<LoadoutTabModel.Assignment> assignment) {
        assignment.ifPresent(a -> LoadoutAccess.set(a.slot(), a.id()));
    }
}
