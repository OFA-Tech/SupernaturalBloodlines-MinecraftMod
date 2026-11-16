package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusOrder;

/**
 * What a kit widget needs from the screen that hosts it: the graphics, the font, the mouse, the keyboard focus, a way to register
 * something that can be hovered or pressed, and the confirm dialog. The hub screen implements it; a widget never knows which screen it is on.
 */
public interface UiContext {
    GuiGraphics g();

    Font font();

    boolean hovered(Rect r);

    boolean focused(String id);

    /** Registers a target: it gets a tooltip, the mouse, and the keyboard focus order of its zone ({@link FocusOrder}). A null action is shown but not pressable. */
    void target(String id, Rect r, List<Component> tooltip, Runnable action);

    /** Asks before {@code action}: Esc and {@code safe} (the left button) keep things as they are, {@code yes} runs it. */
    void confirm(Component title, Component body, Component safe, Component yes, Runnable action);
}
