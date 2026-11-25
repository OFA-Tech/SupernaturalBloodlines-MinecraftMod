package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.MapRegions;

/** The read-only Map and Code cards, independent of the theme and font. */
final class BrotherhoodPlan {
    record Line(String key, List<Object> args) {}
    record Plan(List<Line> lines, Set<MapRegions.Region> regions) {}

    private BrotherhoodPlan() {}

    private static Line line(String key, Object... args) {
        return new Line(key, List.of(args));
    }

    static Plan of(BrotherhoodView view, boolean loaded) {
        if (!loaded || !view.inBrotherhood()) {
            return new Plan(List.of(line("loading")), Set.of());
        }
        if (!view.fits() || !view.perksOn()) {
            return new Plan(List.of(line(view.fits() ? "off" : "not_fit")), Set.of());
        }
        List<Line> lines = new ArrayList<>();
        lines.add(line("arms", view.armsBrothers(), view.armsPercent()));
        if (view.sightOn()) {
            lines.add(line("sight"));
        }
        lines.add(line("map.title"));
        lines.add(line(view.anyHasMax() ? "map.complete" : "map.own"));
        lines.add(line("code.title"));
        lines.add(line("code.motto"));
        lines.add(line(view.codeOn() ? "code.cost" : "code.off"));
        if (view.strikes() > 0) {
            lines.add(line("strikes", view.strikes(), view.strikeLimit()));
        }
        if (view.breaker()) {
            lines.add(line("breaker", ClanPlan.clock(view.breakerSecondsLeft())));
        }
        return new Plan(List.copyOf(lines), Set.copyOf(MapRegions.shown(view.ownMark(), view.anyHasMax())));
    }

    static int mark(BrotherhoodView view, boolean loaded, boolean self, boolean leader) {
        if (!loaded || !view.inBrotherhood() || !view.fits() || !view.perksOn()) {
            return -1;
        }
        return self ? view.ownMark() : leader ? view.leaderMark() : -1;
    }
}
