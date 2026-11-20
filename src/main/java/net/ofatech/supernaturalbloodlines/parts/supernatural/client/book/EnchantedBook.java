package net.ofatech.supernaturalbloodlines.parts.supernatural.client.book;

import net.favouriteless.enchanted.integrations.modopedia.client.page_components.RitePageComponent;
import net.favouriteless.modopedia.api.Lookup;
import net.favouriteless.modopedia.api.book.Book;
import net.favouriteless.modopedia.api.book.page_components.BookRenderContext;
import net.favouriteless.modopedia.api.registries.client.PageComponentRegistry;
import net.favouriteless.modopedia.client.BookOpenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

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

    /** The entry of one of our rites ({@code supernaturalbloodlines:alpha_trial}) in the book, by its path in {@code entries/}. */
    public static String entryOf(String riteId) {
        return "supernaturalbloodlines/rites/" + ResourceLocation.parse(riteId).getPath();
    }

    public static void openRite(String riteId) {
        BookOpenHandler.tryOpenEntry(BOOK, entryOf(riteId));
    }

    private static final class RitePage extends RitePageComponent {
        private boolean off;

        @Override
        public void init(Book book, Lookup lookup, Level level) {
            try {
                super.init(book, lookup, level);
            } catch (IllegalArgumentException notThere) {
                off = true;
            }
        }

        @Override
        public void render(GuiGraphics graphics, BookRenderContext context, int mouseX, int mouseY, float partialTicks) {
            if (!off) {
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
