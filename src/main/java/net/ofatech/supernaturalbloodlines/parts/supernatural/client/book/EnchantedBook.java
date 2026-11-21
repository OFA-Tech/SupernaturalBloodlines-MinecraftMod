package net.ofatech.supernaturalbloodlines.parts.supernatural.client.book;

import net.favouriteless.enchanted.integrations.modopedia.client.page_components.RitePageComponent;
import net.favouriteless.modopedia.api.Lookup;
import net.favouriteless.modopedia.api.book.Book;
import net.favouriteless.modopedia.api.book.page_components.BookRenderContext;
import net.favouriteless.modopedia.api.registries.client.BookContentRegistry;
import net.favouriteless.modopedia.api.registries.client.PageComponentRegistry;
import net.favouriteless.modopedia.client.BookOpenHandler;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.RiteLayout;
import net.ofatech.supernaturalbloodlines.shared.supernatural.RitePlate;

/**
 * Our chapter of Enchanted's book (The Art of Witchcraft, a Modopedia book). Its rite pages are Enchanted's own circle diagram; this is the same
 * component, except that a rite the server has switched off (its part or a mod is off, so it is not in the rite registry) shows a note
 * in place of the circle, where Enchanted's would drop the page with an error. Loaded only when Enchanted and Modopedia are.
 */
public final class EnchantedBook {
    public static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("enchanted", "art_of_witchcraft");
    public static final ResourceLocation RITE_PAGE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "rite");
    public static final String OFF = "supernaturalbloodlines.book.rite_off";

    private EnchantedBook() {
    }

    public static void register() {
        PageComponentRegistry.get().register(RITE_PAGE, RitePage::new);
    }

    public static final String ENTRY_MISSING = "supernaturalbloodlines.book.entry_missing";

    /** Opens the book on a rite's entry; when the book has no such entry (an old resource pack, another language) says so on the action bar and opens nothing. */
    public static boolean openRite(String riteId) {
        String entry = RiteLayout.bookEntry(riteId);
        var content = BookContentRegistry.get().getContent(BOOK, Minecraft.getInstance().options.languageCode);
        // getContent(id, lang) is the language's own content, or en_us when the language has none: the entry must be in the one the book will open
        if (content == null || !content.hasEntry(entry)) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(ENTRY_MISSING), false);
            return false;
        }
        BookOpenHandler.tryOpenEntry(BOOK, entry);
        return true;
    }

    private static final class RitePage extends RitePageComponent {
        private boolean off;
        private String chalk = "ritual_chalk";

        @Override
        public void init(Book book, Lookup lookup, Level level) {
            try {
                super.init(book, lookup, level);
                RiteType type = level.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY).get(lookup.get("rite").as(ResourceLocation.class));
                if (type != null && !type.getShapes().isEmpty()) {
                    chalk = BuiltInRegistries.BLOCK.getKey(type.getShapes().values().iterator().next()).getPath();
                }
            } catch (IllegalArgumentException notThere) {
                off = true;
            }
        }

        @Override
        public void render(GuiGraphics graphics, BookRenderContext context, int mouseX, int mouseY, float partialTicks) {
            if (!off) {
                // the pictures are chalk on nothing, and ritual chalk is white on parchment: a plate under them that reads (RitePlate, 3:1 or better for each chalk)
                graphics.fill(x - 50, y, x + 50, y + 100, RitePlate.plate(chalk));
                if (RitePlate.glyphNeedsBacking(chalk)) {
                    graphics.fill(x - 4, y + 46, x + 4, y + 54, RitePlate.DARK);
                }
                super.render(graphics, context, mouseX, mouseY, partialTicks);
                return;
            }
            int line = Minecraft.getInstance().font.lineHeight + 1;
            int top = y + 8;
            for (FormattedCharSequence text : Minecraft.getInstance().font.split(Component.translatable(OFF), 96)) {
                graphics.drawString(Minecraft.getInstance().font, text, x - 48, top, 0x6B4A2B, false);
                top += line;
            }
        }
    }
}
