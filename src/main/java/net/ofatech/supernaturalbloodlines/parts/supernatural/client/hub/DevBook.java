package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.Minecraft;

/** Development only (the screenshots): Enchanted's book on one of our rite entries, and its page turned to the last spread, where the circle is. */
final class DevBook {
    private DevBook() {
    }

    /** {@code <rite path>} or {@code <rite path>_last}. */
    static void open(String spec) {
        String path = spec.endsWith("_last") ? spec.substring(0, spec.length() - 5) : spec;
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.book.EnchantedBook.openRite("supernaturalbloodlines:" + path);
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
