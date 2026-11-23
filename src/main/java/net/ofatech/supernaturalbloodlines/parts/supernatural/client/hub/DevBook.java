package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.Minecraft;

/** Development only (the screenshots): Enchanted's book on one of our rite entries, and its page turned to the last spread, where the circle is. */
final class DevBook {
    private DevBook() {
    }

    /**
     * {@code book:<rite>[:last]} the rite's own entry; {@code book:sealed-<rite>} its "???" entry; {@code book:category-<id>} a category of our chapter (the list shows
     * which entries are sealed); {@code -Dsupernaturalbloodlines.devshots.learn=<lore|rite|all>} teaches that first, as the command does.
     */
    static void open(String spec) {
        if (spec.equals("modlist")) {
            DevModList.open(); // not a book: NeoForge's mod list (phase 8, the mod icon)
            return;
        }
        learn();
        String path = spec.endsWith("_last") ? spec.substring(0, spec.length() - 5) : spec;
        if (path.startsWith("category-")) {
            net.favouriteless.modopedia.client.BookOpenHandler.tryOpenCategory(net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook.BOOK,
                    "supernaturalbloodlines/rites/" + path.substring(9));
        } else if (path.startsWith("sealed-")) {
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook.openEntry(
                    net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements.sealedEntry(path.substring(7)));
        } else {
            net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook.openRite("supernaturalbloodlines:" + path);
        }
    }

    private static void learn() {
        String what = System.getProperty("supernaturalbloodlines.devshots.learn");
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (what == null || what.isBlank() || server == null || Minecraft.getInstance().player == null) {
            return;
        }
        java.util.UUID id = Minecraft.getInstance().player.getUUID();
        server.execute(() -> {
            var p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                for (String rite : net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlockCommand.resolve(what)) {
                    net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks.learn(p, rite, false);
                }
            }
        });
    }

    /** Modopedia's screen turns its pages through a protected method; a screenshot has no mouse to click the arrow. */
    static void turnToTheEnd() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }
        try {
            for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    var m = c.getDeclaredMethod("turnPageRight", int.class);
                    m.setAccessible(true);
                    for (int i = 0; i < 8; i++) {
                        m.invoke(screen, 1);
                    }
                    return;
                } catch (NoSuchMethodException next) {
                    // look in the parent
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Modopedia's turnPageRight is not there", e);
        }
    }
}
