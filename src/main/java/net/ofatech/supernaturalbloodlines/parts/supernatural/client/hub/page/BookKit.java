package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import org.jetbrains.annotations.Nullable;

/**
 * The book's text and action furniture, shared by the Grimoire's pages: a drop cap, dotted leaders, a wax seal for the one primary action, a red
 * underlined link for the secondary ones, a footnote, a page head (title, subtitle, rule) and a player face. Everything takes its colours from the
 * theme, so a page asking the kit never picks one.
 */
public final class BookKit {
    static final int SEAL = 44;
    private static final int CAP = 16;

    private BookKit() {
    }

    /** The page head: the title, a muted subtitle and a rule. Returns the y under it. */
    static int head(HubScreen.Frame f, String region, Component title, @Nullable Component subtitle) {
        int y = headNoRule(f, region, title, subtitle);
        Rect r = f.region(region);
        f.theme().drawRule(f.g, r.x(), y + 2, r.w());
        return y + 8;
    }

    /** The head without its rule: a page that puts something (filters, tabs) between the title and the rule draws the rule itself. */
    static int headNoRule(HubScreen.Frame f, String region, Component title, @Nullable Component subtitle) {
        HubTheme t = f.theme();
        Rect r = f.region(region);
        int y = f.begin(region);
        y = PageKit.title(f, title, r.x(), y, r.w(), t.ink());
        if (subtitle != null) {
            y = small(f, subtitle, r.x(), y, r.w(), t.muted());
        }
        return y;
    }

    /** Muted text at the normal size (nothing in the hub is under 8 GUI pixels), wrapped to {@code w}: subtitles, captions and legends. Returns the y under it. */
    static int small(HubScreen.Frame f, Component text, int x, int y, int w, int color) {
        int ly = y;
        for (FormattedCharSequence line : f.font().split(text, Math.max(8, w))) {
            f.g.drawString(f.font(), line, x, ly, color, false);
            ly += 9;
        }
        return ly + 1;
    }

    /** The width of {@code text} drawn by {@link #small}. */
    static int smallWidth(HubScreen.Frame f, String text) {
        return f.font().width(text);
    }

    /** A paragraph with a drop cap: the first letter in a dark tile, the rest beside it. Returns the y under it. */
    static int dropCap(HubScreen.Frame f, Component text, int x, int y, int w, int color) {
        HubTheme t = f.theme();
        String s = text.getString();
        if (s.isEmpty()) {
            return y;
        }
        String first = s.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        GuiGraphics g = f.g;
        g.fill(x, y, x + CAP, y + CAP, t.accent());
        g.renderOutline(x + 1, y + 1, CAP - 2, CAP - 2, t.paper());
        g.drawString(f.font(), first, x + (CAP - f.font().width(first)) / 2 + 1, y + (CAP - 8) / 2 + 1, Contrast.bestText(t.accent()), false);
        int ty = y;
        int lines = 0;
        for (FormattedCharSequence line : f.font().split(Component.literal(s.substring(1).stripLeading()), Math.max(10, w - CAP - 4))) {
            g.drawString(f.font(), line, x + CAP + 4, ty, color, false);
            ty += 9;
            lines++;
        }
        return Math.max(y + CAP, ty) + 3;
    }

    /** "Label ........ value": dotted leaders between a label and a value. Returns the y under it. */
    static int leader(HubScreen.Frame f, Component label, Component value, int x, int y, int w, int valueColor) {
        HubTheme t = f.theme();
        Font font = f.font();
        int lw = font.width(label);
        int vw = Math.min(font.width(value), Math.max(w / 3, w - lw - 8));
        String v = font.plainSubstrByWidth(value.getString(), vw);
        int room = w - font.width(v) - 8;
        String l = lw > room ? font.plainSubstrByWidth(label.getString(), Math.max(8, room)) : label.getString();
        f.g.drawString(font, l, x, y, t.ink(), false);
        f.g.drawString(font, v, x + w - font.width(v), y, valueColor, false);
        int from = x + font.width(l) + 3;
        int to = x + w - font.width(v) - 3;
        for (int dx = from; dx < to; dx += 3) {
            f.g.fill(dx, y + 7, dx + 1, y + 8, t.muted() & 0x80FFFFFF | 0x80000000);
        }
        return y + 11;
    }

    /** The wax seal: the one primary action of a page. Registers its target; the label sits inside. */
    static Rect seal(HubScreen.Frame f, String id, Component label, boolean enabled, int x, int y, @Nullable Runnable action, List<Component> tooltip) {
        HubTheme t = f.theme();
        Rect r = new Rect(x, y, SEAL, SEAL);
        boolean hot = enabled && (f.hovered(r) || f.focused(id));
        int dark = t.accent();
        int fill = !enabled ? t.muted() : hot ? lighten(dark) : dark;
        KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x(), r.y(), SEAL, enabled ? dark : t.muted());
        KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x() + 2, r.y() + 2, SEAL - 4, fill);
        String text = label.getString().toUpperCase(java.util.Locale.ROOT);
        // the label is at the normal size; the disc is wide enough for the longest of them, and one that is not is cut with an ellipsis
        text = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(text, SEAL - 6, f.font()::width);
        f.g.drawString(f.font(), text, r.x() + (SEAL - f.font().width(text)) / 2, r.y() + (SEAL - 8) / 2, Contrast.bestText(fill), false);
        f.target(id, r, tooltip, enabled ? action : null);
        if (f.focused(id)) {
            KitDraw.focusRing(f.g, r, t.focusColor());
        }
        return r;
    }

    /** The "Track on HUD" (or "Stop tracking") link of a task: working with the HUD part, a grey link that says why without it. Returns its width. */
    static int trackLink(HubScreen.Frame f, String id, String taskId, boolean claimed, int x, int y) {
        var a = TrackHud.action(taskId, claimed);
        return link(f, id, a.label(), x, y, List.of(a.enabled() || a.disabledReason() == null ? a.label() : a.disabledReason()), a.enabled() ? a.run() : null);
    }

    /** A secondary action: red text with an underline. Returns its width. */
    static int link(HubScreen.Frame f, String id, Component label, int x, int y, List<Component> tooltip, @Nullable Runnable action) {
        HubTheme t = f.theme();
        int w = f.font().width(label);
        Rect r = new Rect(x, y - 1, w, 11);
        boolean hot = f.hovered(r) || f.focused(id);
        int color = action == null ? t.muted() : hot ? lighten(t.accent()) : t.accent();
        f.g.drawString(f.font(), label, x, y, color, false);
        f.g.fill(x, y + 9, x + w, y + 10, color);
        f.target(id, r, tooltip, action);
        if (f.focused(id)) {
            KitDraw.focusRing(f.g, r, t.focusColor());
        }
        return w;
    }

    /** A footnote: a mark and a muted line under a thin rule. Returns the y under it. */
    static int footnote(HubScreen.Frame f, Component text, int x, int y, int w) {
        f.theme().drawRule(f.g, x, y, w);
        return PageKit.wrapped(f, Component.literal("* ").append(text), x, y + 4, w, f.theme().muted());
    }

    /** A small muted heading in capitals. */
    /** A filled box with a one pixel border: the cards and tiles of the pill styles. */
    static void outline(net.minecraft.client.gui.GuiGraphics g, Rect r, int fill, int border) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), border);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, fill);
    }

    static int caps(HubScreen.Frame f, Component text, int x, int y, int color) {
        return small(f, Component.literal(text.getString().toUpperCase(java.util.Locale.ROOT)), x, y, 1000, color) + 2;
    }

    /** The face of a player by name: their own skin when they are known to the client, the default skin otherwise. */
    public static void face(GuiGraphics g, String name, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(name);
        var skin = info != null ? info.getSkin() : DefaultPlayerSkin.get(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        PlayerFaceRenderer.draw(g, skin, x, y, size);
    }

    /** A face in a ring of {@code border}. */
    public static void portrait(GuiGraphics g, String name, int cx, int cy, int size, int border, int paper) {
        KitDraw.shape(g, Shapes.Shape.ROUND, cx - size / 2 - 2, cy - size / 2 - 2, size + 4, border);
        KitDraw.shape(g, Shapes.Shape.ROUND, cx - size / 2 - 1, cy - size / 2 - 1, size + 2, paper);
        face(g, name, cx - size / 2 + 1, cy - size / 2 + 1, Math.max(6, size - 2));
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 36);
        int gr = Math.min(255, ((argb >> 8) & 0xFF) + 20);
        int b = Math.min(255, (argb & 0xFF) + 20);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /** The Blood Moon's page head, top left: the title large and a muted line under it. */
    static void moonHead(HubScreen.Frame f, Component title, @Nullable Component subtitle) {
        if (f.theme() instanceof net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubThemeViews v && v.takesHead(title, subtitle)) {
            return;
        }
        Rect h = f.region("head");
        // the title is large while it fits beside the points and the look button; a narrow window gets it at normal size, then cut with an ellipsis
        float scale = f.font().width(title) * 1.5f <= h.w() ? 1.5f : 1f;
        Component shown = scale == 1f && f.font().width(title) > h.w() ? Component.literal(net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(title.getString(), h.w(), f.font()::width)) : title;
        var pose = f.g.pose();
        pose.pushPose();
        pose.translate(h.x(), h.y() + 1, 0);
        pose.scale(scale, scale, 1f);
        f.g.drawString(f.font(), shown, 0, 0, f.theme().ink(), true);
        pose.popPose();
        if (subtitle != null) {
            small(f, subtitle, h.x(), h.y() + 17, h.w(), f.theme().muted());
        }
    }

    /** A row of pills, the chosen one filled: the Blood Moon's tabs. Returns the y under the row. */
    static int pillRow(HubScreen.Frame f, List<String> labels, int chosen, int x, int y, int w, java.util.function.IntConsumer pick) {
        HubTheme t = f.theme();
        int total = 0;
        for (String s : labels) {
            total += f.font().width(s) + 18;
        }
        int cx = x + Math.max(0, (w - total) / 2);
        for (int i = 0; i < labels.size(); i++) {
            String s = labels.get(i);
            int pw = f.font().width(s) + 14;
            Rect pill = new Rect(cx, y, pw, 14);
            boolean on = i == chosen;
            boolean hot = f.hovered(pill);
            int fill = on ? t.accent() : hot ? t.muted() & 0xFFFFFF | 0x40000000 : 0;
            f.g.fill(pill.x(), pill.y(), pill.right(), pill.bottom(), on ? t.accent() : t.muted());
            f.g.fill(pill.x() + 1, pill.y() + 1, pill.right() - 1, pill.bottom() - 1, on ? fill : t.paper());
            f.g.drawString(f.font(), s, pill.x() + 7, pill.y() + 3, on ? Contrast.bestText(t.accent()) : t.ink(), false);
            int idx = i;
            String id = "pill:" + i;
            f.target(id, pill, List.of(Component.literal(s)), () -> pick.accept(idx));
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, pill, t.focusColor());
            }
            cx += pw + 4;
        }
        return y + 20;
    }

    /** A row of filters (All, SB, VMP...): the chosen one underlined. Returns the chosen index, and the y under the row. */
    static int filterRow(HubScreen.Frame f, List<String> labels, int chosen, int x, int y, int w, java.util.function.IntConsumer pick) {
        HubTheme t = f.theme();
        int cx = x;
        for (int i = 0; i < labels.size(); i++) {
            String s = labels.get(i);
            int sw = f.font().width(s);
            if (cx + sw > x + w) {
                break;
            }
            int color = i == chosen ? t.accent() : t.muted();
            f.g.drawString(f.font(), s, cx, y, color, false);
            if (i == chosen) {
                f.g.fill(cx, y + 9, cx + sw, y + 10, color);
            }
            int idx = i;
            Rect hit = new Rect(cx - 1, y - 1, sw + 2, 11);
            String id = "filter:" + i;
            f.target(id, hit, List.of(Component.literal(s)), () -> pick.accept(idx));
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            cx += sw + 8;
        }
        return y + 14;
    }

    static final String MOON = "moon";
    /** The whole area between the rails and above the bar: the pill styles' Quests, Rites and Loadout draw their cards and grids here. */
    static final String FULL = "full";
    static final String MAIN = HubLayout.MAIN;
    static final String SIDE = HubLayout.SIDE;
}
