package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import org.jetbrains.annotations.Nullable;

/** The few controls the new pages share, drawn in the theme's colours: a button, a switch, a text box and the last reply of a command. */
final class PageWidgets {
    static final int H = 14;
    static final int GAP = 4;

    private PageWidgets() {
    }

    static final int RED_DARK = 0xFFB3261E;
    static final int RED_LIGHT = 0xFFFF8A80;

    /** The warning colour that reads on the theme's paper: the dark red on a light page, the light red on a dark one (4.5:1 or more either way). */
    static int warn(HubTheme t) {
        return Contrast.ratio(RED_DARK, t.paper()) >= Contrast.ratio(RED_LIGHT, t.paper()) ? RED_DARK : RED_LIGHT;
    }

    /** The width a button needs for its label. */
    static int widthOf(Font font, Component label) {
        return font.width(label) + 12;
    }

    /** A button: {@code primary} filled in the accent; {@code why} is shown instead of the action when it is off. */
    static void button(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, @Nullable Component why, @Nullable Runnable action) {
        draw(f, id, r, label, enabled, primary, why, false, action);
    }

    /** A button whose hint is always in its tooltip and narration (not only when disabled): a bare "+" or "-" says what it changes. */
    static void hinted(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, Component hint, @Nullable Runnable action) {
        draw(f, id, r, label, enabled, primary, hint, true, action);
    }

    private static void draw(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, @Nullable Component why, boolean always, @Nullable Runnable action) {
        HubTheme t = f.theme();
        boolean hot = enabled && (f.hovered(r) || f.focused(id));
        int fill = primary ? (enabled ? t.accent() : t.muted()) : hot ? t.good() : t.paper();
        int border = enabled ? t.accent() : t.muted();
        f.g.fill(r.x(), r.y(), r.right(), r.bottom(), border);
        f.g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, fill);
        int text = primary || hot ? Contrast.bestText(fill) : enabled ? t.ink() : t.muted();
        String shown = LabelFit.cut(label.getString(), r.w() - 6, f.font()::width);
        f.g.drawString(f.font(), shown, r.x() + (r.w() - f.font().width(shown)) / 2, r.y() + (r.h() - 8) / 2, text, false);
        List<Component> tip = new ArrayList<>(List.of(label));
        if ((always || !enabled) && why != null) {
            tip.add(why);
        }
        f.target(id, r, tip, enabled ? action : null);
        if (f.focused(id)) {
            t.drawFocus(f.g, r);
        }
    }

    /** A switch with its state in words ("On" / "Off"): one press flips it. */
    static void toggle(HubScreen.Frame f, String id, Rect r, boolean on, Component tip, Runnable flip) {
        Component word = Component.translatable("supernaturalbloodlines.hub.switch." + (on ? "on" : "off"));
        button(f, id, r, word, true, on, tip, flip);
    }

    /** A box with the text being typed (a caret when {@code editing}), or the value when not. */
    static void field(HubScreen.Frame f, String id, Rect r, String text, int caret, boolean editing, boolean invalid, Component tip, Runnable open) {
        HubTheme t = f.theme();
        int border = invalid ? warn(t) : editing ? t.accent() : t.muted();
        f.g.fill(r.x(), r.y(), r.right(), r.bottom(), border);
        f.g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, 0xFF000000 | t.paper());
        String line = text.contains("\n") ? text.substring(0, text.indexOf('\n')) + " \u2026" : text;
        String shown = f.font().plainSubstrByWidth(line, r.w() - 8, true);
        f.g.drawString(f.font(), shown, r.x() + 4, r.y() + (r.h() - 8) / 2, t.ink(), false);
        if (editing && caret <= shown.length() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = r.x() + 4 + f.font().width(shown.substring(0, Math.min(caret, shown.length())));
            f.g.fill(cx, r.y() + 3, cx + 1, r.bottom() - 3, t.ink());
        }
        f.target(id, r, List.of(tip), open);
        if (f.focused(id)) {
            t.drawFocus(f.g, r);
        }
    }

    /** A tall info pane (Tensura's) has no content of its own on these pages: a title and a line say what the page is. The Blood Moon's short bar and the Grimoire's page opposite are left alone. */
    static void info(HubScreen.Frame f, Component title, Component body) {
        Rect d = f.layout().chrome().detail();
        if (d != null && d.h() >= 60) {
            f.detail(net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail.of(title).body(body).build());
        }
    }

    /** The newest reply of a command the hub ran, wrapped, in the ink colour when it worked and the warning red when not. Returns the y under it. */
    static int reply(HubScreen.Frame f, Rect r, int y) {
        var reply = AdminClient.reply();
        if (reply == null || reply.lines().isEmpty() || !net.ofatech.supernaturalbloodlines.shared.hub.ReplyScope.shownOn(reply.rowId(), f.screen().page())) {
            return y;
        }
        int color = reply.success() ? f.theme().ink() : warn(f.theme());
        for (Component line : reply.lines()) {
            y = PageKit.wrapped(f, line, r.x(), y, r.w(), color);
        }
        return y;
    }
}

