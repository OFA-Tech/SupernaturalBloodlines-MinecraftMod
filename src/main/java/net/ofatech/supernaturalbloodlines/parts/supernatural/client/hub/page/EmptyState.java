package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * What a page with nothing to show says: an empty hexagon, one plain sentence ("You run alone."), one line of what to do and one button that opens the page
 * that explains it. The copy is the language file's ({@code hub.empty.<kind>.title|body|button}); the colours are the theme's.
 */
final class EmptyState {
    private EmptyState() {
    }

    /** Draws the state centred in {@code area} from {@code top} down and returns the y under it; the button opens {@code open}. */
    static int render(HubScreen.Frame f, Rect area, int top, String kind, HubPage open) {
        HubTheme t = f.theme();
        String key = PageKit.HUB + "empty." + kind + ".";
        int w = Math.min(area.w() - 8, 200);
        int x = area.x() + (area.w() - w) / 2;
        int y = top + 6;
        int hex = 34;
        KitDraw.shape(f.g, Shapes.Shape.HEX, x + (w - hex) / 2, y, hex, t.muted());
        KitDraw.shape(f.g, Shapes.Shape.HEX, x + (w - hex) / 2 + 2, y + 2, hex - 4, t.paper());
        y += hex + 10;
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
        f.g.fill(button.x(), button.y(), button.right(), button.bottom(), t.accent());
        f.g.fill(button.x() + 1, button.y() + 1, button.right() - 1, button.bottom() - 1, hot ? (t.accent() & 0xFFFFFF) | 0x44000000 : t.paper());
        f.g.drawString(f.font(), label, button.x() + (button.w() - f.font().width(label)) / 2, button.y() + 4, t.accent(), false);
        f.target("empty:" + kind, button, List.of(label), () -> f.screen().setPage(open));
        if (f.focused("empty:" + kind)) {
            KitDraw.focusRing(f.g, button, t.focusColor());
        }
        return button.bottom() + 6;
    }
}
