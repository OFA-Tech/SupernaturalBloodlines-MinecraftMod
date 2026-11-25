package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.MapRegions.Region;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** Placeholder tattoo: six fragments, not a route or a clue to a location. */
final class BrotherhoodMapArt {
    private BrotherhoodMapArt() {}

    static void draw(HubScreen.Frame f, Rect r, Set<Region> shown) {
        int ink = f.theme().ink();
        int muted = f.theme().muted();
        fill(f, r, 0, 0, 76, 36, muted);
        fill(f, r, 1, 1, 75, 35, f.theme().paper());
        for (Region region : shown) {
            int x = 8 + region.ordinal() % 3 * 22;
            int y = 5 + region.ordinal() / 3 * 16;
            fill(f, r, x + 6, y, x + 8, y + 12, ink);
            fill(f, r, x + 2, y + 5, x + 12, y + 7, ink);
            if (region.ordinal() % 2 == 0) {
                fill(f, r, x, y + 2, x + 2, y + 10, ink);
                fill(f, r, x + 12, y + 2, x + 14, y + 10, ink);
            } else {
                fill(f, r, x + 3, y + 2, x + 11, y + 4, ink);
                fill(f, r, x + 3, y + 8, x + 11, y + 10, ink);
            }
        }
    }

    private static void fill(HubScreen.Frame f, Rect r, int left, int top, int right, int bottom, int color) {
        f.g.fill(r.x() + left * r.w() / 76, r.y() + top, r.x() + right * r.w() / 76, r.y() + bottom, color);
    }
}
