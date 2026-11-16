package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.EnumMap;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.GuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.LoadoutPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.RitesPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.SkillsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PlaceholderPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.QuestsPage;

/**
 * The shared page views, drawn into {@link HubLayout#MAIN} and {@link HubLayout#SIDE}. They are the documented fallback of {@link HubThemeViews#view}: the
 * Grimoire, the Blood Moon and Tensura now bring a view for every page, so these are reached only by a theme that brings none (a future one, or one added
 * by another mod), and by {@code SkillsPage}, which the book's Skills page still borrows. {@code HubPagesTest} checks that every page has one.
 */
public final class HubPages {
    private static final HubPageView PLACEHOLDER = new PlaceholderPage();
    private static final Map<HubPage, HubPageView> SHARED = new EnumMap<>(Map.of(
            HubPage.PATH, new PathPage(),
            HubPage.QUESTS, new QuestsPage(),
            HubPage.SKILLS, new SkillsPage(),
            HubPage.RITES, new RitesPage(),
            HubPage.BLOODLINE, new BloodlinePage(),
            HubPage.PACK, new PackPage(),
            HubPage.GUIDE, new GuidePage(),
            HubPage.LOADOUT, new LoadoutPage()));

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
