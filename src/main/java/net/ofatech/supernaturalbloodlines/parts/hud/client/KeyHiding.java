package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Takes the covered keys off the Controls screen, without a mixin (the reserved {@code KeyBindsListHideMixin} stays unused unless this fails).
 * On {@link ScreenEvent.Init.Post} it removes their entries from the screen's list. Controlling replaces the screen with a subclass whose list
 * keeps every entry in a public {@code allEntries} (read through {@code getAllEntries()}) and rebuilds the visible ones from it on each search,
 * so the covered entries are removed from that list too. A category header left with nothing under it goes as well.
 * Matched by shape, not by Controlling's class names: an entry is a vanilla {@code KeyEntry}, or anything with a public {@code getKey()} that returns a
 * {@code KeyMapping} (Controlling's {@code IKeyEntry}).
 */
public final class KeyHiding {
    private static final Map<Class<?>, Method> GET_KEY = new HashMap<>();
    private static boolean warned;

    private KeyHiding() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(KeyHiding::onInit);
    }

    private static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof KeyBindsScreen screen) || !HudClientConfig.hideCoveredKeys() || !KeyCover.covering() || screen.keyBindsList == null) {
            return;
        }
        KeyBindsList list = screen.keyBindsList;
        try {
            hide(list.children());
            Method all = publicMethod(list.getClass(), "getAllEntries");
            if (all != null && all.invoke(list) instanceof List<?> entries) {
                @SuppressWarnings("unchecked")
                List<KeyBindsList.Entry> every = (List<KeyBindsList.Entry>) entries;
                hide(every);
            }
            list.setScrollAmount(list.getScrollAmount());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (!warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("Covered keys: could not hide them on the Controls screen (they stay visible, unbound)", ex);
            }
        }
    }

    private static void hide(List<KeyBindsList.Entry> entries) {
        entries.removeIf(en -> {
            String name = keyName(en);
            return name != null && KeyCover.isCovered(name);
        });
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (isCategory(entries.get(i)) && (i == entries.size() - 1 || isCategory(entries.get(i + 1)))) {
                entries.remove(i);
            }
        }
    }

    private static boolean isCategory(KeyBindsList.Entry entry) {
        return entry.getClass().getSimpleName().equals("CategoryEntry");
    }

    private static String keyName(KeyBindsList.Entry entry) {
        if (entry instanceof KeyBindsList.KeyEntry vanilla) {
            return vanilla.key.getName();
        }
        Method getKey = GET_KEY.computeIfAbsent(entry.getClass(), c -> publicMethod(c, "getKey"));
        if (getKey != null && getKey.getReturnType() == KeyMapping.class) {
            try {
                return ((KeyMapping) getKey.invoke(entry)).getName();
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }
        return null;
    }

    private static Method publicMethod(Class<?> c, String name) {
        try {
            return c.getMethod(name);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
