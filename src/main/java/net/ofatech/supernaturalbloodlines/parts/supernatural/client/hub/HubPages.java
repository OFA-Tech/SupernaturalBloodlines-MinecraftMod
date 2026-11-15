package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.EnumMap;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PlaceholderPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.QuestsPage;

/** The shared page views, drawn into {@link HubLayout#MAIN} and {@link HubLayout#SIDE}; any theme may replace one (see {@link HubTheme#view}). */
public final class HubPages {
    private static final HubPageView PLACEHOLDER = new PlaceholderPage();
    private static final Map<HubPage, HubPageView> SHARED = new EnumMap<>(Map.of(
            HubPage.PATH, new PathPage(),
            HubPage.QUESTS, new QuestsPage()));

    private HubPages() {
    }

    public static HubPageView shared(HubPage page) {
        return SHARED.getOrDefault(page, PLACEHOLDER);
    }

    /** What a page a player cannot use yet shows (a Ghoul's locked pages). */
    public static HubPageView locked() {
        return PLACEHOLDER;
    }
}
