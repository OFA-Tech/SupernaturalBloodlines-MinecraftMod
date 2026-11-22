package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmptyFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * What a page with nothing to show says: an empty hexagon, one plain sentence ("You run alone."), one line of what to do and one button that opens the page
 * that explains it. The copy is the language file's ({@code hub.empty.<kind>.title|body|button}); the colours are the theme's.
 */
public final class EmptyState {
    private EmptyState() {
    }

    /** Draws the state centred in {@code area} from {@code top} down and returns the y under it; the button opens {@code open}. */
    public static int render(HubScreen.Frame f, Rect area, int top, String kind, HubPage open) {
        return render(f, area, top, kind, open, null);
    }

    /**
     * As above, with a second action under the button when {@code second} is a page this player can see: its label is {@code hub.empty.<kind>.button2}
     * (a page the player cannot see, like the Pack page for a human, gets no action).
     */
    public static int render(HubScreen.Frame f, Rect area, int top, String kind, HubPage open, @org.jetbrains.annotations.Nullable HubPage second) {
        HubTheme t = f.theme();
        String key = PageKit.HUB + "empty." + kind + ".";
        int w = Math.min(area.w() - 8, 200);
        int x = area.x() + (area.w() - w) / 2;
        int y = top + 6;
        int hex = EmptyFit.HEX;
        boolean hasCaption = net.minecraft.client.resources.language.I18n.exists(key + "caption");
        int bodyLines = f.font().split(Component.translatable(key + "body"), w).size();
        // a small page (GUI scale 4) gives up the hexagon and its caption so the buttons are not cut off at the foot
        boolean art = EmptyFit.showArt(top, area.bottom(), hasCaption, bodyLines, second != null && second.visible(f.view.context()));
        if (art) {
            KitDraw.shape(f.g, Shapes.Shape.HEX, x + (w - hex) / 2, y, hex, t.muted());
            KitDraw.shape(f.g, Shapes.Shape.HEX, x + (w - hex) / 2 + 2, y + 2, hex - 4, t.paper());
            y += hex + 4;
        }
        if (art && hasCaption) {
            Component caption = Component.translatable(key + "caption");
            f.g.drawString(f.font(), caption, x + (w - f.font().width(caption)) / 2, y, t.muted(), false);
            y += 10;
        }
        y += 6;
        Component title = Component.translatable(key + "title");
        var pose = f.g.pose();
        pose.pushPose();
        pose.translate(x + (w - f.font().width(title) * 1.3f) / 2f, y, 0);
        pose.scale(1.3f, 1.3f, 1f);
        f.g.drawString(f.font(), title, 0, 0, t.ink(), false);
        pose.popPose();
        y += 16;
        for (var line : f.font().split(Component.translatable(key + "body"), w)) {
            f.g.drawString(f.font(), line, x + (w - f.font().width(line)) / 2, y, t.muted(), false);
            y += 10;
        }
        Component label = Component.translatable(key + "button");
        Rect button = new Rect(x + (w - Math.min(w, f.font().width(label) + 24)) / 2, y + 8, Math.min(w, f.font().width(label) + 24), 16);
        boolean hot = f.hovered(button) || f.focused("empty:" + kind);
        var part = PageWidgets.part(true, false, hot);
        net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit.draw(f.g, t, part, button);
        f.g.drawString(f.font(), label, button.x() + (button.w() - f.font().width(label)) / 2, button.y() + 4, net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit.label(t, part), false);
        f.target("empty:" + kind, button, List.of(label), () -> f.screen().setPage(open));
        if (f.focused("empty:" + kind)) {
            KitDraw.focusRing(f.g, button, t.focusColor());
        }
        int bottom = button.bottom();
        if (second != null && second.visible(f.view.context())) {
            Component label2 = Component.translatable(key + "button2");
            String id2 = "empty:" + kind + ":2";
            int w2 = Math.min(w, f.font().width(label2) + 24);
            Rect b2 = new Rect(x + (w - w2) / 2, bottom + 6, w2, 16);
            boolean hot2 = f.hovered(b2) || f.focused(id2);
            var part2 = PageWidgets.part(true, false, hot2);
            net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit.draw(f.g, t, part2, b2);
            f.g.drawString(f.font(), label2, b2.x() + (b2.w() - f.font().width(label2)) / 2, b2.y() + 4, net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit.label(t, part2), false);
            f.target(id2, b2, List.of(label2), () -> f.screen().setPage(second));
            if (f.focused(id2)) {
                KitDraw.focusRing(f.g, b2, t.focusColor());
            }
            bottom = b2.bottom();
        }
        return bottom + 6;
    }
}
