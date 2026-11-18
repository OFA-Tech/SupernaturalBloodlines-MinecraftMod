package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.HubKeybinds;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEdit;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.ofatech.supernaturalbloodlines.shared.admin.PlayerSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingEntry;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingKind;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;

/**
 * The player's own client settings: the look (theme, which page opens), the HUD, and the key binds, read-only with a shortcut to Controls.
 * These are CLIENT config keys: they are never sent to the server. A change saves at once.
 */
public final class SettingsPage implements HubPageView {
    static final String KEY = PageKit.HUB + "settings.";

    /** Whether a key belongs on this player's page: the Tensura ones need Tensura (and the blood spot Vampirism too). */
    static boolean applies(SettingEntry e) {
        return switch (e.dottedPath()) {
            case "hide_tensura_skill_hud" -> ModList.get().isLoaded("tensura");
            case "blood_in_food_spot" -> ModList.get().isLoaded("tensura") && ModList.get().isLoaded("vampirism");
            default -> true;
        };
    }

    static Component label(SettingEntry e) {
        return Component.translatableWithFallback(KEY + "key." + e.dottedPath(), AdminEdit.humanize(e.id()));
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        String section = "";
        for (SettingEntry e : PlayerSettings.ENTRIES) {
            if (!applies(e) || AdminSpecs.value(e).isEmpty()) {
                continue;
            }
            if (!e.section().equals(section)) {
                section = e.section();
                y = PageKit.wrapped(f, Component.translatable(KEY + "section." + section).withStyle(ChatFormatting.BOLD), r.x(), y + 3, r.w(), t.accent());
            }
            y = row(f, r, y, e);
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "section.keys").withStyle(ChatFormatting.BOLD), r.x(), y + 6, r.w(), t.accent());
        y = key(f, r, y, "key.supernaturalbloodlines.hub", HubKeybinds.HUB.getTranslatedKeyMessage());
        y = key(f, r, y, "key.supernaturalbloodlines.answer_request", HubKeybinds.ANSWER.getTranslatedKeyMessage());
        Component controls = Component.translatable(KEY + "controls");
        Rect b = new Rect(r.x(), y + 3, Math.min(r.w(), PageWidgets.widthOf(f.font(), controls)), PageWidgets.H);
        PageWidgets.button(f, "stage:settings:controls", b, controls, true, false, null, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new ControlsScreen(f.screen(), mc.options));
        });
        f.end(b.bottom() + 2);

        PageWidgets.info(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "about_text"));
        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.title(f, Component.translatable(KEY + "about"), s.x(), sy, s.w(), t.accent());
        t.drawRule(f.g, s.x(), sy + 2, s.w());
        sy = PageKit.wrapped(f, Component.translatable(KEY + "about_text"), s.x(), sy + 6, s.w(), t.ink());
        sy = PageKit.wrapped(f, Component.translatable(KEY + "loadout_note"), s.x(), sy + 4, s.w(), t.muted());
        f.end(sy);
    }

    private int key(HubScreen.Frame f, Rect r, int y, String lang, Component bound) {
        HubTheme t = f.theme();
        Component name = Component.translatable(lang);
        f.g.drawString(f.font(), name, r.x() + 4, y, t.ink(), false);
        f.g.drawString(f.font(), bound, r.right() - f.font().width(bound), y, t.muted(), false);
        f.target("stage:settings:keybind:" + lang, new Rect(r.x(), y - 1, r.w(), PageKit.LINE + 1), List.of(name, bound), null);
        return y + PageKit.LINE + 1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int row(HubScreen.Frame f, Rect r, int y, SettingEntry e) {
        HubTheme t = f.theme();
        ModConfigSpec.ConfigValue cv = AdminSpecs.value(e).orElseThrow();
        Component label = label(e);
        int top = y + 1;
        y = PageKit.wrapped(f, label, r.x() + 4, top, e.kind() == SettingKind.BOOL ? r.w() - 44 : r.w() - 4, t.ink());
        String id = "stage:settings:" + e.dottedPath();
        if (e.kind() == SettingKind.BOOL) {
            Rect b = new Rect(r.right() - 36, top - 1, 36, PageWidgets.H);
            boolean on = (Boolean) cv.get();
            PageWidgets.toggle(f, id, b, on, label, () -> {
                cv.set(!on);
                cv.save();
            });
            return Math.max(y, b.bottom()) + 2;
        }
        Object current = cv.get();
        Object[] options = current instanceof Enum<?> en ? en.getDeclaringClass().getEnumConstants() : new Object[0];
        List<Integer> widths = new ArrayList<>();
        List<Component> labels = new ArrayList<>();
        for (Object o : options) {
            Component c = Component.translatableWithFallback(KEY + "choice." + e.dottedPath() + "." + ((Enum<?>) o).name().toLowerCase(Locale.ROOT),
                    net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.pretty(((Enum<?>) o).name()));
            labels.add(c);
            widths.add(PageWidgets.widthOf(f.font(), c));
        }
        Flow.Result flow = Flow.wrap(new Rect(r.x() + 4, y, r.w() - 4, 0), widths, PageWidgets.H, PageWidgets.GAP);
        for (int i = 0; i < options.length; i++) {
            Enum<?> o = (Enum<?>) options[i];
            boolean needsTensura = e.dottedPath().equals("hub_theme") && o.name().equals("TENSURA") && HubHooks.theme("tensura").isEmpty();
            PageWidgets.button(f, id + ":" + o.name(), flow.rects().get(i), labels.get(i), !needsTensura, o == current,
                    needsTensura ? Component.translatable(PageKit.HUB + "theme.needs.tensura") : null, () -> {
                        cv.set(o);
                        cv.save();
                    });
        }
        return flow.bottom() + 2;
    }
}

