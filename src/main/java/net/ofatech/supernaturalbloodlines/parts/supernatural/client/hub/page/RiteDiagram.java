package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import net.ofatech.supernaturalbloodlines.shared.supernatural.RiteLayout;

/**
 * A rite's circle as Enchanted draws it in its own book: the gold heart glyph, then each circle of the rite in its own chalk (Enchanted's pictures, so the
 * shape and the colour are the real ones), and the items on rings around the centre ({@link RiteLayout}). It sits on a dark plate in every theme, because
 * ritual chalk is white and would vanish on the Grimoire's parchment. Enchanted's pictures exist only where Enchanted is, which is where a rite is.
 */
final class RiteDiagram {
    private static final String ENCHANTED = "enchanted";
    static final int MAX = 100;
    static final int MIN = 64;
    private static final int PLATE = 0xFF16141C;

    private RiteDiagram() {
    }

    /** The side of the picture for a region this wide. */
    static int size(int width) {
        return Math.max(MIN, Math.min(MAX, width - 4));
    }

    /** Draws the diagram centred in {@code r} from {@code y}; returns the y under it. */
    static int draw(HubScreen.Frame f, Rect r, int y, HubSocial.Rite rite) {
        HubTheme t = f.theme();
        int size = size(r.w());
        int x = r.x() + (r.w() - size) / 2;
        f.g.fill(x - 2, y - 2, x + size + 2, y + size + 2, (t.muted() & 0xFFFFFF) | 0xFF000000);
        f.g.fill(x - 1, y - 1, x + size + 1, y + size + 1, PLATE);
        blit(f, "textures/gui/gold_glyph.png", x, y, size);
        for (HubSocial.Circle c : rite.circles()) {
            blit(f, RiteLayout.shapeTexture(c), x, y, size);
        }
        float k = size / 100f;
        List<RiteLayout.Spot> spots = RiteLayout.items(rite.items().size());
        for (int i = 0; i < spots.size(); i++) {
            String entry = rite.items().get(i);
            int space = entry.indexOf(' ');
            ResourceLocation id = ResourceLocation.tryParse(space < 0 ? entry : entry.substring(space + 1));
            Item item = id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
            int cx = x + size / 2 + Math.round(spots.get(i).x() * k);
            int cy = y + size / 2 + Math.round(spots.get(i).y() * k);
            int half = Math.max(5, Math.round(8 * k));
            if (item != null) {
                f.g.pose().pushPose();
                f.g.pose().translate(cx - 8 * k, cy - 8 * k, 0);
                f.g.pose().scale(k, k, 1f);
                f.g.renderItem(new ItemStack(item), 0, 0);
                f.g.pose().popPose();
            }
            f.target("rite-item:" + rite.id() + ":" + i, new Rect(cx - half, cy - half, half * 2, half * 2), List.of(RitesPage.item(entry)), null);
        }
        return y + size + 6;
    }

    private static void blit(HubScreen.Frame f, String path, int x, int y, int size) {
        // Enchanted blits these with a 100 by 100 texture size whatever their pixels, so the whole picture fills the square
        f.g.blit(ResourceLocation.fromNamespaceAndPath(ENCHANTED, path), x, y, size, size, 0, 0, 100, 100, 100, 100);
    }

    /** The chalk each circle is drawn in as a colour, for the small rings on a rite card. */
    static int chalkColor(HubSocial.Circle c) {
        return switch (c.chalk()) {
            case "nether_chalk" -> 0xFFE0523C;
            case "otherwhere_chalk" -> 0xFFB080E8;
            default -> 0xFFE8E4D8;
        };
    }
}
