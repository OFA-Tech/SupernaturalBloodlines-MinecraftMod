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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitFaces;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
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

    /** A button {@code w} wide at the right edge of {@code region}: its picture draws {@code margin} px past its box, so the box is that far in and the border is not clipped. */
    static Rect rightBox(Rect region, int top, int w) {
        return new Rect(region.right() - w - Part.BUTTON_NORMAL.margin(), top, w, H);
    }

    /** A button: {@code primary} filled in the accent; {@code why} is shown instead of the action when it is off. */
    static void button(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, @Nullable Component why, @Nullable Runnable action) {
        draw(f, id, r, label, enabled, primary, why, Tip.WHEN_OFF, action);
    }

    /** A symbol button ("+", "-"): the hint says what it changes ("Lower Max health") and is its name, in the tooltip and in the narration. */
    static void stepper(HubScreen.Frame f, String id, Rect r, Component symbol, boolean enabled, Component hint, @Nullable Runnable action) {
        draw(f, id, r, symbol, enabled, false, hint, Tip.NAME, action);
    }

    /** A button with a line that is always shown under its name (what it will do), not only when it is off. */
    static void described(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, @Nullable Component what, @Nullable Runnable action) {
        draw(f, id, r, label, enabled, primary, what, Tip.ALWAYS, action);
    }

    /** The kit part of a button in a state ({@link KitFaces#button} gives a button that is off its disabled picture whatever the pointer does). */
    static Part part(boolean enabled, boolean primary, boolean hot) {
        return KitFaces.button(primary, enabled, hot, false);
    }

    private enum Tip { WHEN_OFF, ALWAYS, NAME }

    private static void draw(HubScreen.Frame f, String id, Rect r, Component label, boolean enabled, boolean primary, @Nullable Component why, Tip mode, @Nullable Runnable action) {
        HubTheme t = f.theme();
        boolean drawn = f.whole(r.y(), r.h());
        if (drawn) {
            Part part = part(enabled, primary, f.hovered(r) || f.focused(id));
            UiKit.draw(f.g, t, part, r);
            int text = UiKit.label(t, part);
            String shown = net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.cut(f.font(), label.getString(), r.w() - 6, f.font()::width);
            f.g.drawString(f.font(), shown, r.x() + (r.w() - f.font().width(shown)) / 2, r.y() + (r.h() - 8) / 2, text, false);
        }
        boolean byHint = mode == Tip.NAME && why != null;
        List<Component> tip = new ArrayList<>(List.of(byHint ? why : label));
        if (!byHint && why != null && (mode == Tip.ALWAYS || !enabled)) {
            tip.add(why);
        }
        f.target(id, r, tip, enabled ? action : null);
        if (drawn && f.focused(id)) {
            t.drawFocus(f.g, r);
        }
    }

    /** A switch with its state in words ("On" / "Off"): one press flips it. */
    static void toggle(HubScreen.Frame f, String id, Rect r, boolean on, Component setting, Runnable flip) {
        Component word = Component.translatable("supernaturalbloodlines.hub.switch." + (on ? "on" : "off"));
        draw(f, id, r, word, true, on, toggleName(setting, on), Tip.NAME, flip);
    }

    /** What a switch is called in the tooltip and the narration: the setting and its state ("Protect my True Alpha path: On"), not a bare "On". */
    static Component toggleName(Component setting, boolean on) {
        return Component.empty().append(setting).append(": ").append(Component.translatable("supernaturalbloodlines.hub.switch." + (on ? "on" : "off")));
    }

    /** A box with the text being typed (a caret when {@code editing}), or the value when not. */
    static void field(HubScreen.Frame f, String id, Rect r, String text, int caret, boolean editing, boolean invalid, Component tip, Runnable open) {
        HubTheme t = f.theme();
        boolean drawn = f.whole(r.y(), r.h());
        if (!drawn) {
            f.target(id, r, List.of(tip), open);
            return;
        }
        UiKit.draw(f.g, t, Part.INSET, r);
        // a refused value is outlined in the warning red, a box being typed in in the accent: a 1 px line over the inset
        String line = text.contains("\n") ? text.substring(0, text.indexOf('\n')) + " \u2026" : text;
        String shown = f.font().plainSubstrByWidth(line, r.w() - 8, true);
        f.g.drawString(f.font(), shown, r.x() + 4, r.y() + (r.h() - 8) / 2, t.ink(), false);
        if (editing && caret <= shown.length() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = r.x() + 4 + f.font().width(shown.substring(0, Math.min(caret, shown.length())));
            f.g.fill(cx, r.y() + 3, cx + 1, r.bottom() - 3, t.ink());
        }
        int edge = net.ofatech.supernaturalbloodlines.shared.hub.kit.FieldEdge.colour(editing, invalid, warn(t), t.accent());
        if (edge != 0) {
            f.g.renderOutline(r.x(), r.y(), r.w(), r.h(), edge);
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

