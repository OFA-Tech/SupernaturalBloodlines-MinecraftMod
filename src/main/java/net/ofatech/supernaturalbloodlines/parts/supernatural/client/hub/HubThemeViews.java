package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;

/**
 * What a {@link HubTheme} of another shape adds: its own view of some pages and what it draws from the frame's data before the page
 * (Blood Moon's moon). A theme that fits the shared views (the Grimoire, Tensura's) does not implement it.
 */
public interface HubThemeViews {
    /** The view of a page in this theme; the shared one unless the theme brings its own. */
    default HubPageView view(HubPage page) {
        return HubPages.shared(page);
    }

    default void drawOverlay(HubScreen.Frame f, HubPage page) {
    }

    /** A theme with its own title bar takes the page's title and line (Tensura's "Status · Path"); the others draw them on the page. */
    default boolean takesHead(net.minecraft.network.chat.Component title, @org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component subtitle) {
        return false;
    }
}
