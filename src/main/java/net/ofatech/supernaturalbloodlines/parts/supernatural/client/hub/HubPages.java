package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.EnumMap;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.AdminPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.HomePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.ProfilePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.SettingsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.GuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.LoadoutPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
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
    private static final HubPageView PROFILE = new ProfilePage();
    private static final Map<HubPage, HubPageView> SHARED = new EnumMap<>(HubPage.class);

    static {
        SHARED.put(HubPage.HOME, new HomePage());
        SHARED.put(HubPage.PATH, new PathPage());
        SHARED.put(HubPage.QUESTS, new QuestsPage());
        SHARED.put(HubPage.SKILLS, new SkillsPage());
        SHARED.put(HubPage.BLOODLINE, new BloodlinePage());
        SHARED.put(HubPage.PACK, new PackPage());
        SHARED.put(HubPage.GUIDE, new GuidePage());
        SHARED.put(HubPage.SETTINGS, new SettingsPage());
        SHARED.put(HubPage.ADMIN, new AdminPage());
        SHARED.put(HubPage.LOADOUT, new LoadoutPage());
    }

    private HubPages() {
    }

    private static final HubPageView MOON = new net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.MoonPage();

    /** The moon calendar route: one view for every theme, like the Profile. */
    public static HubPageView moon() {
        return MOON;
    }

    /** The Profile route: one view for every theme, drawn into the same regions. */
    public static HubPageView profile() {
        return PROFILE;
    }

    public static HubPageView shared(HubPage page) {
        return SHARED.getOrDefault(page, PLACEHOLDER);
    }

    /** What a page a player cannot use yet shows (a Ghoul's locked pages). */
    public static HubPageView locked() {
        return PLACEHOLDER;
    }
}
