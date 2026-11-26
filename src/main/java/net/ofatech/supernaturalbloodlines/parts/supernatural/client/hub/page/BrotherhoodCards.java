package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodWire;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.BrotherhoodClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

final class BrotherhoodCards {
    static final String KEY = PageKit.HUB + "brotherhood.";
    private static final int MAP_HEIGHT = 38;

    private BrotherhoodCards() {}

    static boolean applies(GroupView view) {
        return view.hasGroup() && view.type() == GroupType.BROTHERHOOD;
    }

    static int since(HubScreen.Frame f, Rect r, int y, GroupView view) {
        if (!applies(view)) {
            return y;
        }
        return PageKit.wrapped(f, Component.translatable(KEY + (view.sinceDays() == 1 ? "since.one" : "since"), view.sinceDays()),
                r.x(), y, r.w(), f.theme().muted()) + 3;
    }

    static int pips(HubScreen.Frame f, Rect r, int y, GroupView group, HubSocial.Member member) {
        if (!applies(group)) {
            return y;
        }
        int mark = BrotherhoodPlan.mark(BrotherhoodClient.view(), BrotherhoodClient.loaded(),
                member.id().filter(f.view.player().getUUID()::equals).isPresent(), member.base().equals("leader"));
        if (mark < 0) {
            return y;
        }
        var text = BrotherhoodPlan.markLine(mark);
        y = PageKit.wrapped(f, Component.translatable(KEY + text.key(), text.args().toArray()), r.x() + 8, y, r.w() - 8, f.theme().muted());
        int count = BrotherhoodWire.MAX_MARK;
        int step = Math.min(4, (r.w() - 8) / count);
        if (step >= 2 && f.whole(y, 5) && y >= f.clipTop()) {
            for (int i = 0; i < count; i++) {
                int x = r.x() + 8 + i * step;
                // an empty pip is a low line, so filled and empty differ by shape whatever the theme's colours
                f.g.fill(x, i < mark ? y : y + 2, x + step - 1, y + 3, i < mark ? f.theme().ink() : f.theme().muted());
            }
        }
        return y + 5;
    }

    static int render(HubScreen.Frame f, Rect r, int y, GroupView group) {
        if (!applies(group)) {
            return y;
        }
        var plan = BrotherhoodPlan.of(BrotherhoodClient.view(), BrotherhoodClient.loaded());
        for (var line : plan.lines()) {
            Component text = Component.translatable(KEY + line.key(), line.args().toArray());
            if (line.key().endsWith(".title")) {
                y = PageKit.title(f, text, r.x(), y + 4, r.w(), f.theme().accent(), 30);
            } else {
                y = PageKit.wrapped(f, text, r.x(), y, r.w(), line.key().equals("breaker") ? PageWidgets.warn(f.theme()) : f.theme().ink());
            }
            if (line.key().startsWith("map.") && !line.key().endsWith(".title")) {
                if (f.whole(y, MAP_HEIGHT) && y >= f.clipTop()) {
                    BrotherhoodMapArt.draw(f, new Rect(r.x(), y, Math.min(76, r.w()), MAP_HEIGHT), plan.regions());
                }
                y += MAP_HEIGHT;
            }
        }
        return y;
    }
}
