package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import org.jetbrains.annotations.Nullable;

/** The drawing the pages share: wrapped text, task rows with their tags and stamps, and the words for the model's ids. */
final class PageKit {
    static final String HUB = "supernaturalbloodlines.hub.";

    /** The key of a skill's one-paragraph description: {@code hub.skill_desc.<id in lower case>}. */
    static String skillDescKey(String skillId) {
        return HUB + "skill_desc." + skillId.toLowerCase(java.util.Locale.ROOT);
    }
    static final int LINE = 10;

    private PageKit() {
    }

    static Component race(SupernaturalRace r) {
        return Component.translatable("supernaturalbloodlines.supernatural.race." + r.id());
    }

    static Component rung(HubModel.RungRow r) {
        return Component.translatableWithFallback(HUB + "rung." + r.id(), r.name());
    }

    static Component text(HubModel.TaskText t) {
        if (I18n.exists(t.override())) {
            return Component.translatable(t.override());
        }
        Object[] args = t.args().stream().map(PageKit::arg).toArray();
        return Component.translatableWithFallback(t.key(), t.fallback(), args);
    }

    private static Object arg(HubModel.Arg a) {
        return switch (a) {
            case HubModel.Num n -> String.valueOf(n.value());
            case HubModel.Key k -> Component.translatableWithFallback(k.key(), k.fallback());
            case HubModel.Item i -> {
                ResourceLocation id = ResourceLocation.tryParse(i.id());
                yield id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id).getDescription() : Component.literal(i.id());
            }
        };
    }

    static Component sourceTag(String source) {
        return Component.translatable(HUB + "source_tag." + source);
    }

    static Component sourceName(String source) {
        return Component.translatable(HUB + "source." + source);
    }

    /** Draws {@code c} wrapped to {@code w}; returns the y under it. */
    static int wrapped(HubScreen.Frame f, Component c, int x, int y, int w, int color) {
        for (FormattedCharSequence line : f.font().split(c, w)) {
            f.g.drawString(f.font(), line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    static int centered(HubScreen.Frame f, Component c, Rect r, int y, int color) {
        for (FormattedCharSequence line : f.font().split(c, r.w())) {
            f.g.drawString(f.font(), line, r.x() + (r.w() - f.font().width(line)) / 2, y, color, false);
            y += LINE;
        }
        return y;
    }

    /** A title at 1.5x when it fits the width, else at 1x. Returns the y under it. */
    static int title(HubScreen.Frame f, Component c, int x, int y, int w, int color) {
        Font font = f.font();
        float scale = font.width(c) * 1.5f <= w ? 1.5f : 1f;
        if (scale == 1f) {
            return wrapped(f, c, x, y, w, color);
        }
        f.g.pose().pushPose();
        f.g.pose().translate(x, y, 0);
        f.g.pose().scale(scale, scale, 1f);
        f.g.drawString(font, c, 0, 0, color, false);
        f.g.pose().popPose();
        return y + 14;
    }

    static List<Component> tooltip(HubModel.TaskRow row) {
        List<Component> out = new ArrayList<>();
        out.add(text(row.text()));
        for (HubModel.TaskRow m : row.members()) {
            out.add(Component.literal(" - ").append(text(m.text())).withStyle(ChatFormatting.GRAY));
        }
        out.add(Component.translatable(HUB + "task.source", sourceName(row.source())).withStyle(ChatFormatting.GRAY));
        if (row.counted() && row.target() > 1) {
            out.add(Component.translatable(HUB + "task.progress", row.progress(), row.target()).withStyle(ChatFormatting.GRAY));
        }
        String hint = row.text().hint();
        if (hint != null && I18n.exists(hint)) {
            out.add(Component.translatable(hint).withStyle(ChatFormatting.DARK_AQUA));
        }
        out.add(Component.translatable(HUB + "task.state." + row.state().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD));
        return out;
    }

    /**
     * One task: its box, its words (with the count when it counts), its source tag and, when it can be claimed and {@code stamps} is
     * on, the wax stamp that claims it. A group lists its members under it. Returns the y under the row.
     */
    static int taskRow(HubScreen.Frame f, HubModel.TaskRow row, int x, int y, int w, boolean stamps, @Nullable Runnable onSelect, boolean selected) {
        HubTheme t = f.theme();
        Font font = f.font();
        Component tag = sourceTag(row.source());
        int tagW = font.width(tag) + 4;
        boolean stamp = stamps && row.state() == TaskState.CLAIMABLE;
        int stampW = stamp ? HubTheme.STAMP + 2 : 0;
        int textX = x + HubTheme.BOX + 3;
        int textW = Math.max(20, x + w - stampW - tagW - 2 - textX);
        MutableComponent words = text(row.text()).copy();
        if (row.counted() && row.target() > 1 && row.state() != TaskState.CLAIMED) {
            words.append(Component.literal(" " + row.progress() + "/" + row.target()).withColor(t.muted() & 0xFFFFFF));
        }
        List<FormattedCharSequence> lines = font.split(words, textW);
        int h = Math.max(lines.size() * LINE, stamp ? HubTheme.STAMP : LINE);
        Rect rowRect = new Rect(x - 1, y - 1, w + 2, h + 1);
        if (selected) {
            f.g.fill(rowRect.x(), rowRect.y(), rowRect.right(), rowRect.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
        }
        t.drawTaskBox(f.g, x, y + 1, row.state());
        int color = row.state() == TaskState.CLAIMED ? t.muted() : t.ink();
        int ly = y + (h - lines.size() * LINE) / 2;
        for (FormattedCharSequence line : lines) {
            f.g.drawString(font, line, textX, ly, color, false);
            ly += LINE;
        }
        int tagX = x + w - stampW - tagW;
        int sc = t.sourceColor(row.source());
        f.g.renderOutline(tagX, y, tagW, 9, sc);
        f.g.drawString(font, tag, tagX + 2, y + 1, sc, false);
        String rowId = "task:" + row.task().id();
        f.target(rowId, rowRect, tooltip(row), onSelect);
        if (f.focused(rowId)) {
            t.drawFocus(f.g, rowRect);
        }
        if (stamp) {
            Rect s = new Rect(x + w - HubTheme.STAMP, y + (h - HubTheme.STAMP) / 2, HubTheme.STAMP, HubTheme.STAMP);
            String id = row.task().id();
            t.drawStamp(f.g, s.x(), s.y(), f.hovered(s) || f.focused("stamp:" + id));
            f.target("stamp:" + id, s, List.of(Component.translatable(HUB + "task.claim"), text(row.text())), () -> f.claim(id));
            if (f.focused("stamp:" + id)) {
                t.drawFocus(f.g, s);
            }
        }
        y += h + 2;
        for (HubModel.TaskRow m : row.members()) {
            y = taskRow(f, m, x + 8, y, w - 8, false, null, false);
        }
        return y;
    }
}
