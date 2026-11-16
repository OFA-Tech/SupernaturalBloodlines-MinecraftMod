package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Loadout as a page of its own: the same list of known abilities and six slots as the Skills page's Loadout tab (which stays), drawn by the same
 * {@link LoadoutTab}, so what you assign here is what the tab shows.
 */
public final class LoadoutPage implements HubPageView {
    private final LoadoutTab tab = new LoadoutTab();

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(PageKit.HUB + "page.loadout"), r.x(), y, r.w(), t.ink());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 8;
        if (!LoadoutTab.available()) {
            f.end(PageKit.wrapped(f, Component.translatable(PageKit.HUB + "loadout.off"), r.x(), y, r.w(), t.muted()));
            return;
        }
        tab.render(f, r, y);
    }
}
