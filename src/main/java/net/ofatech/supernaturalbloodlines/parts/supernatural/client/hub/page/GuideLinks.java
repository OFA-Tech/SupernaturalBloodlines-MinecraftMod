package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** The links a Guide chapter ends with: the bosses chapter points at the Bloodline page, where the line and its Original are. Shared by the three Guide pages. */
public final class GuideLinks {
    static final String CHAPTER = "bosses";
    /** Development only (the dev screenshots): the chapter to open, taken once. */
    private static volatile String devChapter;

    public static void devChapter(String id) {
        devChapter = id;
    }

    /** The chapter a dev screenshot asked for, once; otherwise the one the page has. */
    static String chapterOr(String current) {
        String dev = devChapter;
        devChapter = null;
        return dev != null ? dev : current;
    }

    private GuideLinks() {
    }

    /** Draws the link below the chapter's text when the chapter has one; returns the y below it. */
    static int after(HubScreen.Frame f, String chapter, Rect s, int y) {
        if (!CHAPTER.equals(chapter)) {
            return y;
        }
        Component label = Component.translatable(PageKit.HUB + "guide.bosses.link");
        Rect b = new Rect(s.x(), y + 6, Math.min(s.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.button(f, "stage:guide:link:bloodline", b, label, true, false, null, () -> f.screen().setPage(HubPage.BLOODLINE));
        return b.bottom() + 2;
    }
}
