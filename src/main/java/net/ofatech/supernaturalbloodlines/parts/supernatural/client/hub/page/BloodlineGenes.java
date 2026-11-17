package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** The genes in stoppered vials, as the server lets this client know them (Unknown until discovered). Shared by the three Bloodline pages. */
public final class BloodlineGenes {
    public static final int HEIGHT = 72;

    private BloodlineGenes() {
    }

    /** Draws one vial per gene across {@code area} from {@code y}; returns the y under them. */
    public static int draw(HubScreen.Frame f, Rect area, int y, HubSnapshotPayload snap) {
        HubTheme t = f.theme();
        int n = Genes.ALL.size();
        int step = area.w() / Math.max(1, n);
        for (int i = 0; i < n; i++) {
            String gene = Genes.ALL.get(i);
            Genes.Knowledge k = snap.genes().getOrDefault(gene, Genes.Knowledge.UNKNOWN);
            int vx = area.x() + step * i + (step - 26) / 2;
            vial(f, vx, y, k);
            Component name = Component.translatable(LinePageSupport.KEY + "gene." + gene);
            Component value = Component.translatable(LinePageSupport.KEY + "gene_value." + k.name().toLowerCase(Locale.ROOT));
            int mid = vx + 13;
            String shown = f.font().plainSubstrByWidth(name.getString().replace(" gene", ""), step);
            f.g.drawString(f.font(), shown, mid - f.font().width(shown) / 2, y + 52, t.ink(), false);
            String v = f.font().plainSubstrByWidth(value.getString(), step);
            f.g.drawString(f.font(), v, mid - f.font().width(v) / 2, y + 61, k == Genes.Knowledge.TRUE ? t.good() : t.muted(), false);
            f.target("gene:" + gene, new Rect(vx - 4, y, 34, HEIGHT), List.of(name.copy().append(": ").append(value),
                    Component.translatable(LinePageSupport.KEY + "gene_hint").withStyle(ChatFormatting.GRAY)), null);
        }
        return y + HEIGHT + 4;
    }

    /** A stoppered vial: the cork, a glass body, and a liquid that is full and gold when known, low and pale when not. */
    private static void vial(HubScreen.Frame f, int x, int y, Genes.Knowledge k) {
        HubTheme t = f.theme();
        f.g.fill(x + 8, y, x + 18, y + 6, 0xFF7A5238);
        f.g.fill(x, y + 6, x + 26, y + 48, t.ink());
        f.g.fill(x + 1, y + 7, x + 25, y + 47, t.paper());
        int level = k == Genes.Knowledge.TRUE ? 34 : k == Genes.Knowledge.FALSE ? 10 : 8;
        int liquid = k == Genes.Knowledge.TRUE ? 0xFFB8903A : 0xFFC8BBA0;
        f.g.fill(x + 1, y + 47 - level, x + 25, y + 47, liquid);
        if (k == Genes.Knowledge.UNKNOWN) {
            f.g.drawString(f.font(), "?", x + 13 - f.font().width("?") / 2, y + 22, t.muted(), false);
        }
    }

    /** The genes as plain lines, for a place too narrow for vials. */
    public static List<Component> lines(HubSnapshotPayload snap) {
        return Genes.ALL.stream().map(g -> (Component) Component.translatable(LinePageSupport.KEY + "gene." + g).append(": ")
                .append(Component.translatable(LinePageSupport.KEY + "gene_value." + snap.genes().getOrDefault(g, Genes.Knowledge.UNKNOWN).name().toLowerCase(Locale.ROOT)))).toList();
    }
}
