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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TitleFont;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PortraitGeometry;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import org.jetbrains.annotations.Nullable;

/**
 * The book's text and action furniture, shared by the Grimoire's pages: a drop cap, dotted leaders, a wax seal for the one primary action, a red
 * underlined link for the secondary ones, a footnote, a page head (title, subtitle, rule) and a player face. Everything takes its colours from the
 * theme, so a page asking the kit never picks one.
 */
public final class BookKit {
    static final int SEAL = 44;

    /** The line that says what the pack seal accepts stands this far over it. */
    static final int SEAL_NOTE_ABOVE = 10;
    static final int SEAL_NOTE_H = 9;

    /** The y of the note over a seal at {@code sealY}. */
    static int sealNoteY(int sealY) {
        return sealY - SEAL_NOTE_ABOVE;
    }

    /** What the scrolled column gives up at its foot for the seal, the gap over it, the note and a pixel of air under the "more below" marker. */
    static int sealReserve() {
        return SEAL + 8 + SEAL_NOTE_ABOVE + 2;
    }
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
        List<FormattedCharSequence> split = f.font().split(text, Math.max(8, w));
        int whole = net.ofatech.supernaturalbloodlines.shared.hub.ScrollHint.wholeLines(y, 9, split.size(), f.clipBottom());
        for (int i = 0; i < split.size(); i++) {
            if (i < whole) {
                f.g.drawString(f.font(), split.get(i), x, ly, color, false);
            }
            ly += 9;
        }
        return ly + 1;
    }

    /** The width of {@code text} drawn by {@link #small}. */
    static int smallWidth(HubScreen.Frame f, String text) {
        return f.font().width(text);
    }

    /** A paragraph with a drop cap: the first letter large in the accent, the rest of its word beside it on the same line, then the text, two lines of it beside the capital. Returns the y under it. */
    static int dropCap(HubScreen.Frame f, Component text, int x, int y, int w, int color) {
        HubTheme t = f.theme();
        String s = text.getString();
        if (s.isEmpty()) {
            return y;
        }
        String first = s.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        GuiGraphics g = f.g;
        int capW = f.font().width(first) * 2;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(2f, 2f, 1f);
        g.drawString(f.font(), first, 0, 0, t.accent(), false);
        pose.popPose();
        int ty = y;
        List<net.ofatech.supernaturalbloodlines.shared.hub.kit.DropCap.Line> lines = net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.dropCap(f.font(), s.substring(1), Math.max(10, w), capW + 1, CAP_LINES, f.font()::width);
        for (var line : lines) {
            g.drawString(f.font(), line.text(), x + line.indent(), ty, color, false);
            ty += 9;
        }
        return Math.max(y + CAP_LINES * 9, ty) + 3;
    }

    /** The capital is as tall as this many lines of text (twice the font's 8 px, in 9 px lines). */
    private static final int CAP_LINES = 2;
    /** "Label ........ value": dotted leaders between a label and a value. Returns the y under it. */
    static int leader(HubScreen.Frame f, Component label, Component value, int x, int y, int w, int valueColor) {
        HubTheme t = f.theme();
        Font font = f.font();
        int lw = font.width(label);
        int vw = Math.min(font.width(value), Math.max(w / 3, w - lw - 8));
        // both end in an ellipsis when they do not fit, like every other label in the hub
        String v = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(value.getString(), vw, font::width);
        int room = w - font.width(v) - 8;
        String l = lw > room ? net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(label.getString(), Math.max(8, room), font::width) : label.getString();
        if (!f.whole(y, 10)) {
            return y + 11;
        }
        f.g.drawString(font, l, x, y, t.ink(), false);
        f.g.drawString(font, v, x + w - font.width(v), y, valueColor, false);
        int from = x + font.width(l) + 3;
        int to = x + w - font.width(v) - 3;
        int dotColor = t.muted() & 0x80FFFFFF | 0x80000000;
        KitDraw.batch(f.g, () -> {
            for (int dx = from; dx < to; dx += 3) {
                f.g.fill(dx, y + 7, dx + 1, y + 8, dotColor);
            }
        });
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
        boolean drawn = f.whole(y - 1, 11);
        if (drawn) {
            f.g.drawString(f.font(), label, x, y, color, false);
            f.g.fill(x, y + 9, x + w, y + 10, color);
        }
        f.target(id, r, tooltip, action);
        if (drawn && f.focused(id)) {
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
    /** A card or tile of the pill styles: the kit's card surface (inset), with a 1 px line in {@code edge} inside it for the one that is chosen or ready ({@code 0} for none). */
    static void card(HubScreen.Frame f, Rect r, int edge) {
        UiKit.draw(f.g, f.theme(), net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.CARD, r);
        if (edge != 0) {
            f.g.renderOutline(r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2, edge);
        }
    }

    /** A card whose height is known only after its content is drawn: the surface is washed over the content at {@link net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites#CARD_OVER_ALPHA}, so the content stays readable under it. */
    static void cardOver(HubScreen.Frame f, Rect r, int edge) {
        UiKit.draw(f.g, f.theme(), net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.CARD, r.x(), r.y(), r.w(), r.h(), net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.CARD_OVER_ALPHA);
        if (edge != 0) {
            f.g.renderOutline(r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2, edge);
        }
    }

    /** A small label chip (a filter, an identity tag): the kit's chip, the lit one when {@code on}. Returns the colour of its label. */
    static int chip(HubScreen.Frame f, Rect r, boolean on) {
        return chip(f, r, on, false);
    }

    /** The same, with the pointer over it: a chip that is not the chosen one gets an outline, never the lit picture. */
    static int chip(HubScreen.Frame f, Rect r, boolean on, boolean hovered) {
        var look = net.ofatech.supernaturalbloodlines.shared.hub.kit.ChipLook.of(on, hovered);
        UiKit.draw(f.g, f.theme(), look.part(), r);
        if (look.outline()) {
            f.g.renderOutline(r.x(), r.y(), r.w(), r.h(), f.theme().accent());
        }
        return UiKit.label(f.theme(), look.part());
    }

    /** A box that is a field of text (a text area): the kit's inset. */
    static void inset(HubScreen.Frame f, Rect r) {
        UiKit.draw(f.g, f.theme(), Part.INSET, r);
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

    /** A face of {@code size} in a square frame of {@code border} ({@link PortraitGeometry}): Home, Profile and every ring of faces. */
    public static void portrait(GuiGraphics g, String name, int cx, int cy, int size, int border, int paper) {
        PortraitGeometry.Boxes b = PortraitGeometry.of(cx, cy, size);
        KitDraw.shape(g, Shapes.Shape.SOFT, b.frame().x(), b.frame().y(), b.frame().w(), border);
        KitDraw.shape(g, Shapes.Shape.SOFT, b.paper().x(), b.paper().y(), b.paper().w(), paper);
        face(g, name, b.face().x(), b.face().y(), b.face().w());
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
        if (PageKit.themedTitle(f, title, h.x(), h.y() + 1, h.w(), f.theme().ink(), true)) {
            if (subtitle != null) {
                small(f, subtitle, h.x(), h.y() + 1 + TitleFont.HEIGHT + 1, h.w(), f.theme().muted());
            }
            return;
        }
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
            int text = chip(f, pill, on, f.hovered(pill));
            f.g.drawString(f.font(), s, pill.x() + 7, pill.y() + 3, text, false);
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
