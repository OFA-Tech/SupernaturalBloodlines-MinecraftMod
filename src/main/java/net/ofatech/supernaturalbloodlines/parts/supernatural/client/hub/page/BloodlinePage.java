package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * Bloodline: the sire (and whether the bond is severed), the sired, the Five for a hunter, on the left; the genes on the right, as the
 * server lets this client know them under {@code gene_visibility} (Unknown until discovered, or always shown), and Tensura's live EP.
 */
public final class BloodlinePage implements HubPageView {
    private static final String KEY = PageKit.HUB + "bloodline.";

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        HubSnapshotPayload snap = f.view.snapshot();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y = heading(f, r, y + 6, KEY + "sire");
        boolean hasSire = f.view.bloodline().sire() != null;
        Component sire = !hasSire ? Component.translatable(KEY + "no_sire")
                : snap.sire().map(n -> (Component) Component.literal(n)).orElse(Component.translatable(KEY + "sire_unknown"));
        y = PageKit.wrapped(f, sire, r.x() + 4, y, r.w() - 4, t.ink());
        if (hasSire && snap.severed()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "severed"), r.x() + 4, y, r.w() - 4, t.muted());
        }
        y = heading(f, r, y + 4, KEY + "sired");
        if (snap.sired().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x() + 4, y, r.w() - 4, t.muted());
        }
        for (String name : snap.sired()) {
            y = PageKit.wrapped(f, Component.literal("\u2022 " + name), r.x() + 4, y, r.w() - 4, t.ink());
        }
        if (f.view.races().get(0) == SupernaturalRace.HUNTER) {
            y = heading(f, r, y + 4, KEY + "five");
            y = PageKit.wrapped(f, Component.translatable(KEY + "five_count", snap.fiveCount()), r.x() + 4, y, r.w() - 4, t.ink());
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.title(f, Component.translatable(KEY + "genes"), s.x(), sy, s.w(), t.accent());
        t.drawRule(f.g, s.x(), sy + 2, s.w());
        sy += 6;
        for (String gene : Genes.ALL) {
            Genes.Knowledge k = snap.genes().getOrDefault(gene, Genes.Knowledge.UNKNOWN);
            Component name = Component.translatable(KEY + "gene." + gene);
            Component value = Component.translatable(KEY + "gene_value." + k.name().toLowerCase(Locale.ROOT));
            int vw = f.font().width(value);
            f.g.drawString(f.font(), name, s.x(), sy, t.ink(), false);
            f.g.drawString(f.font(), value, s.right() - vw, sy, k == Genes.Knowledge.TRUE ? t.good() : k == Genes.Knowledge.FALSE ? t.ink() : t.muted(), false);
            Rect hit = new Rect(s.x() - 1, sy - 1, s.w() + 2, PageKit.LINE + 1);
            String id = "gene:" + gene;
            f.target(id, hit, List.of(name.copy().append(": ").append(value), Component.translatable(KEY + "gene_hint").withStyle(ChatFormatting.GRAY)), null);
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
            sy += PageKit.LINE + 1;
        }
        sy = PageKit.wrapped(f, Component.translatable(KEY + "gene_hint"), s.x(), sy + 3, s.w(), t.muted());
        HubHooks.TensuraInfo info = HubHooks.tensura().orElse(null);
        if (info != null) {
            t.drawRule(f.g, s.x(), sy + 3, s.w());
            sy = PageKit.wrapped(f, Component.translatable(KEY + "tensura", info.race()), s.x(), sy + 7, s.w(), t.ink());
            sy = PageKit.wrapped(f, Component.translatable(KEY + "ep", String.format("%,d", (long) info.ep()), String.format("%,d", (long) info.magicule())),
                    s.x(), sy, s.w(), t.ink());
        }
        f.end(sy);
    }

    private static int heading(HubScreen.Frame f, Rect r, int y, String key) {
        return PageKit.wrapped(f, Component.translatable(key).withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), f.theme().accent());
    }
}
