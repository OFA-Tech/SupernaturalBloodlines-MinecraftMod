package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Development only (the screenshots): NeoForge's mod list with this mod found and selected, so the mod icon (logoFile) is shown at the size the game draws it. */
final class DevModList {
    private DevModList() {
    }

    static void open() {
        Minecraft.getInstance().setScreen(new ModListScreen(new TitleScreen()));
    }

    /** Types the mod's name in the search box, then selects the one entry left. Called a few ticks after {@link #open}, once the screen has built its list. */
    static void select() {
        if (!(Minecraft.getInstance().screen instanceof ModListScreen screen)) {
            return;
        }
        try {
            Field search = ModListScreen.class.getDeclaredField("search");
            search.setAccessible(true);
            ((EditBox) search.get(screen)).setValue("Supernatural");
            screen.tick();
            Field list = ModListScreen.class.getDeclaredField("modList");
            list.setAccessible(true);
            ModListWidget widget = (ModListWidget) list.get(screen);
            if (!widget.children().isEmpty()) {
                screen.setSelected(widget.children().get(0));
            }
        } catch (ReflectiveOperationException e) {
            SupernaturalBloodlines.LOGGER.warn("DEVSHOTS: the mod list could not be driven", e);
        }
    }
}
