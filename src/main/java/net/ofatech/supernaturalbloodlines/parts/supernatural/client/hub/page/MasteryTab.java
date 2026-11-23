package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.MasteryAccess;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;

/**
 * The Mastery tab of the Skills page: where a player picks the one skill the Rite of Mastery will master for them. It lists the skills they hold with their
 * tier; a skill that cannot be chosen is greyed with the reason the client knows (mastered, lent). Choose sends {@code bloodline mastery choose <skill>}
 * and the server checks it again, saying in chat what it refuses (an addon's own skills, a retired one, the wrong tier is the rite's, not the picker's).
 * What was last chosen in this session is marked; "Show" asks the server for the stored choice.
 */
public final class MasteryTab {
    private static final String KEY = PageKit.HUB + "skills.mastery.";
    private static final int ROW = 12;

    private String selected;

    public static boolean available() {
        return MasteryAccess.available();
    }

    void reset() {
        selected = null;
    }

    void render(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "hint"), r.x(), y, r.w(), t.ink()) + 2;
        List<MasteryAccess.Entry> entries = MasteryAccess.entries();
        for (MasteryAccess.Entry e : entries) {
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, ROW);
            boolean grey = e.greyed() != null;
            boolean picked = e.id().equals(selected);
            if (picked) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            String mark = e.id().equals(MasteryAccess.chosen()) ? "> " : "";
            String text = LabelFit.cut(mark + e.name().getString() + " \u00b7 " + e.tier().getString(), r.w() - 4, f.font()::width);
            f.g.drawString(f.font(), text, r.x(), y + 2, grey ? t.muted() : t.ink(), false);
            List<Component> tip = new ArrayList<>(List.of(e.name()));
            if (grey) {
                tip.add(e.greyed());
            }
            String id = "mastery:skill:" + e.id();
            // a row the client already knows is refused cannot be selected: it shows why on hover and nothing else
            f.target(id, hit, tip, () -> {
                if (!grey) {
                    selected = e.id();
                }
            });
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
            y += ROW;
        }
        if (entries.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        y += 4;
        y = button(f, r, y, "choose", selected != null, () -> {
            HubClient.runCommand("B12", List.of(selected));
        });
        y = button(f, r, y, "clear", true, () -> {
            HubClient.runCommand("B13", List.of());
        });
        y = button(f, r, y, "show", true, () -> HubClient.runCommand("B11", List.of()));
        f.end(y);
    }

    private static int button(HubScreen.Frame f, Rect r, int y, String name, boolean enabled, Runnable action) {
        HubTheme t = f.theme();
        Component label = Component.translatable(KEY + name);
        Rect hit = new Rect(r.x() - 1, y - 1, f.font().width(label) + 4, ROW);
        f.g.drawString(f.font(), label, r.x() + 1, y + 2, !enabled ? t.muted() : f.hovered(hit) ? t.accent() : t.ink(), false);
        String id = "mastery:" + name;
        if (enabled) {
            f.target(id, hit, List.of(Component.translatable(KEY + name + ".tip")), action);
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
        }
        return y + ROW + 2;
    }
}
