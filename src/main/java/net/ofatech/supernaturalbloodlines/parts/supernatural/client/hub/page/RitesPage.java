package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubRites;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * Rites: your rite tasks (with their claim stamps), then this mod's rites the server has loaded, yours first ({@link HubRites}); the
 * chosen rite in full on the right: its power, circle and items, as the server's rite data has them.
 */
public final class RitesPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "rites.";

    @Nullable
    private String selected = devSelected;
    private static String devSelected;

    /** Development only (the dev screenshots): the rite the next Rites page starts on. */
    public static void devSelect(String id) {
        devSelected = id;
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.TaskRow> tasks = new ArrayList<>();
        f.view.ladders().forEach(l -> l.tasks().stream().filter(RitesPage::isRite).forEach(tasks::add));
        Set<String> asked = HubRites.fromLadders(LadderTable.clientCopy(), f.view.races());
        List<HubRites.Listed> rites = HubRites.list(f.view.snapshot().rites(), f.view.races(), asked);
        HubRites.Listed chosen = rites.stream().filter(l -> l.rite().id().equals(selected)).findFirst().orElse(rites.isEmpty() ? null : rites.get(0));

        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        for (HubModel.TaskRow row : tasks) {
            y = PageKit.taskRow(f, row, r.x(), y, r.w(), true, null, false);
        }
        Boolean header = null;
        for (HubRites.Listed l : rites) {
            if (header == null || header != l.forYou()) {
                header = l.forYou();
                y = PageKit.wrapped(f, Component.translatable(KEY + (l.forYou() ? "for_you" : "others")).withStyle(ChatFormatting.BOLD),
                        r.x(), y + 3, r.w(), t.accent());
            }
            Component power = Component.translatable(KEY + "power_short", l.rite().power());
            int pw = f.font().width(power);
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, PageKit.LINE + 1);
            if (l == chosen) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            String name = LabelFit.cut(name(l.rite()).getString(), r.w() - pw - 6, f.font()::width);
            f.g.drawString(f.font(), name, r.x(), y, t.ink(), false);
            f.g.drawString(f.font(), power, r.right() - pw, y, t.muted(), false);
            String id = "rite:" + l.rite().id();
            f.target(id, hit, List.of(name(l.rite()), power), () -> selected = l.rite().id());
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
            y += PageKit.LINE + 1;
        }
        if (tasks.isEmpty() && rites.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        if (chosen != null) {
            HubSocial.Rite rite = chosen.rite();
            sy = PageKit.wrapped(f, name(rite), s.x(), sy, s.w(), t.accent());
            t.drawRule(f.g, s.x(), sy + 2, s.w());
            sy = PageKit.wrapped(f, Component.translatable(KEY + "power", String.format("%,d", rite.power())), s.x(), sy + 6, s.w(), t.ink());
            for (String shape : rite.shapes()) {
                sy = PageKit.wrapped(f, Component.translatable(KEY + "circle", shapeName(shape)), s.x(), sy, s.w(), t.ink());
            }
            if (!rite.items().isEmpty()) {
                sy = PageKit.wrapped(f, Component.translatable(KEY + "items"), s.x(), sy + 3, s.w(), t.ink());
                for (String item : rite.items()) {
                    sy = PageKit.wrapped(f, Component.literal("\u2022 ").append(item(item)), s.x() + 4, sy, s.w() - 4, t.ink());
                }
            }
            sy = PageKit.wrapped(f, Component.translatable(chosen.forYou() ? KEY + "yours" : KEY + "anyone"), s.x(), sy + 3, s.w(),
                    chosen.forYou() ? t.good() : t.muted());
            for (Component note : trialNotes(rite, f.view.bloodline().wolf())) {
                sy = PageKit.wrapped(f, note, s.x(), sy + 3, s.w(), t.accent());
            }
        }
        boolean witch = f.view.races().contains(SupernaturalRace.WITCH) || f.view.races().contains(SupernaturalRace.SIPHONER);
        t.drawRule(f.g, s.x(), sy + 3, s.w());
        sy = PageKit.wrapped(f, Component.translatable(KEY + (witch ? "witch" : "not_witch")), s.x(), sy + 7, s.w(), t.muted());
        sy = PageKit.wrapped(f, Component.translatable(KEY + "book"), s.x(), sy + 3, s.w(), t.muted());
        f.end(sy);
    }

    /** The extra lines of the Alpha Trial card (what it asks, the warning or the reason), empty for any other rite. */
    static List<Component> trialNotes(HubSocial.Rite rite, net.ofatech.supernaturalbloodlines.core.bloodline.WolfState wolf) {
        if (!rite.id().equals(HubRites.TRIAL)) {
            return List.of();
        }
        List<Component> out = new ArrayList<>();
        for (String key : HubRites.trialNotes(wolf)) {
            out.add(Component.translatable(KEY + "trial." + key));
        }
        return out;
    }

    static Component name(HubSocial.Rite rite) {
        ResourceLocation id = ResourceLocation.parse(rite.id());
        return Component.translatableWithFallback("rite." + id.getNamespace() + "." + id.getPath(), HubModel.pretty(id.getPath()));
    }

    static Component shapeName(String shape) {
        ResourceLocation id = ResourceLocation.tryParse(shape);
        String path = id == null ? shape : id.getPath();
        return Component.translatableWithFallback(KEY + "shape." + path, HubModel.pretty(path));
    }

    /** "count id" as "2 x Item Name". */
    static Component item(String entry) {
        int space = entry.indexOf(' ');
        String count = space < 0 ? "1" : entry.substring(0, space);
        ResourceLocation id = ResourceLocation.tryParse(space < 0 ? entry : entry.substring(space + 1));
        Component name = id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id).getDescription() : Component.literal(entry);
        return Component.literal(count + " x ").append(name);
    }

    static boolean isRite(HubModel.TaskRow row) {
        return row.task().leaves().stream().anyMatch(l -> l.detector() == TaskSpec.Detector.RITE);
    }
}
