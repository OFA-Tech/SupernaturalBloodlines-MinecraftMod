package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;

/** What the Path pages of the three themes build the same way: the next rung's details, its Track and Claim actions, and the almanac's line. */
final class PathActions {
    private static final String KEY = PageKit.HUB + "path.";

    private PathActions() {
    }

    /** The next rung's name and pips with its reward as the body's end: the start of a detail of the Path, before the theme adds its own lines. */
    static Detail.Builder rung(HubModel.LadderView ladder, Component kicker) {
        HubModel.RungRow active = ladder.active().orElseThrow();
        Detail.Builder d = Detail.of(PageKit.rung(active)).pips(active.claimed(), Math.max(active.tasks(), 1)).reward(PathPage.reward(active));
        return kicker == null ? d : d.kicker(kicker);
    }

    /** The first task of the ladder that can be claimed now. */
    static Optional<HubModel.TaskRow> claimable(HubModel.LadderView ladder) {
        return ladder.tasks().stream().filter(r -> r.state() == TaskState.CLAIMABLE).findFirst();
    }

    /** Adds "Track on HUD" and the primary claim button (named {@code claimLabel}) to a detail of the Path. */
    static void addActions(Detail.Builder d, HubScreen.Frame f, HubModel.LadderView ladder, Component claimLabel) {
        Optional<HubModel.TaskRow> trackable = PathPage.trackable(ladder);
        d.action(TrackHud.action(trackable.map(r -> r.task().id()).orElse(""), trackable.isEmpty()));
        Optional<HubModel.TaskRow> claimable = claimable(ladder);
        d.action("claim", claimLabel, true, claimable.isPresent(), () -> claimable.ifPresent(r -> f.claim(r.task().id())));
    }

    /** The hover target of one rung of a ladder drawn as a row of marks (the Blood Moon's and Tensura's Path): its tooltip, and the focus ring when the keyboard is on it. */
    static void rungTarget(HubScreen.Frame f, HubModel.LadderView ladder, HubModel.RungRow row, net.ofatech.supernaturalbloodlines.shared.hub.Rect hit) {
        String id = "phase:" + ladder.race().id() + ":" + row.id();
        f.target(id, hit, PathPage.rungTooltip(ladder, row), null);
        if (f.focused(id)) {
            net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw.focusRing(f.g, hit, f.theme().focusColor());
        }
    }

    /** "The full moon rises tonight" or "The full moon in N days". */
    static Component almanacLine(int moonPhase) {
        int days = HubAlmanac.daysToFull(moonPhase);
        return days == 0 ? Component.translatable(KEY + "full_moon_today") : Component.translatable(KEY + "full_moon_in", days);
    }
}
