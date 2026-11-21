package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.favouriteless.modopedia.util.client.AdvancementHelper;
import net.favouriteless.modopedia.util.common.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook;
import net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule;
import org.jetbrains.annotations.Nullable;

/**
 * The hub's way to a rite: all of them live in Enchanted's book (plan {@code rites-unlock}), which the player must carry. A rite they have not learned
 * opens its "???" entry. Needs Enchanted and Modopedia; every method says "no book" without them.
 */
public final class RiteLinks {
    public static final String NEEDS_BOOK = "supernaturalbloodlines.hub.launcher.needs_book";

    private RiteLinks() {
    }

    private static boolean available() {
        return ModList.get().isLoaded("modopedia") && ModList.get().isLoaded("enchanted");
    }

    /** The Art of Witchcraft is in the inventory: Modopedia's own rule, the one its study feature uses. */
    public static boolean hasBook() {
        Minecraft mc = Minecraft.getInstance();
        return available() && mc.player != null && InventoryUtils.hasBook(mc.player, EnchantedBook.BOOK);
    }

    /** Whether the player has learned the rite, as the book itself sees it (the client's advancements). A rite that is not gated is known. */
    public static boolean known(String rite) {
        if (!RiteUnlockTable.isGated(rite) || !available() || Minecraft.getInstance().getConnection() == null) {
            return true;
        }
        return AdvancementHelper.hasAdvancement(ResourceLocation.parse(RiteAdvancements.known(rite)));
    }

    /** Why a link to a rite cannot be used now, or null when it can (the book is carried). */
    @Nullable
    public static Component blocked() {
        return hasBook() ? null : Component.translatable(NEEDS_BOOK);
    }

    /** What a link does now. */
    public static RiteLinkRule.Open decide(String rite) {
        return RiteLinkRule.decide(hasBook(), RiteUnlockTable.isGated(rite), known(rite));
    }

    /** Opens the book at the rite ({@code rite} is its path or its id), or its "???" entry when it is not learned; nothing without the book. */
    public static void open(String rite) {
        String path = rite.substring(rite.indexOf(':') + 1);
        switch (decide(path)) {
            // a toast, because the action bar is behind the hub's own screen
            case NEED_BOOK -> net.minecraft.client.gui.components.toasts.SystemToast.add(Minecraft.getInstance().getToasts(),
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.translatable(NEEDS_BOOK), null);
            case ENTRY -> EnchantedBook.openRite(path);
            case SEALED -> EnchantedBook.openEntry(RiteAdvancements.sealedEntry(path));
        }
    }
}
