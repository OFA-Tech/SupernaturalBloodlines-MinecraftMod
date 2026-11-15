package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;

/** A page a Ghoul cannot use yet ("Complete your turning"), or one whose chapter is not written yet (Skills, Rites, Bloodline, Pack: 12e Tasks 3 and 4). */
public final class PlaceholderPage implements HubPageView {
    @Override
    public void render(HubScreen.Frame f) {
        boolean locked = f.screen().page().locked(f.view.context());
        Rect r = f.region(0);
        int y = r.y() + r.h() / 3;
        y = PageKit.centered(f, Component.translatable(f.screen().page().langKey()), r, y, f.theme().accent());
        f.theme().drawRule(f.g, r.x(), y + 2, r.w());
        PageKit.centered(f, Component.translatable(PageKit.HUB + (locked ? "locked_ghoul" : "unwritten")), r, y + 8, f.theme().muted());
    }
}
