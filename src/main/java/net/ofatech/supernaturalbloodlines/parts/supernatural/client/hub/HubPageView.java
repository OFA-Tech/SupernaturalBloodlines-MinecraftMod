package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

/**
 * One page's content. It draws into the theme's regions through the frame and registers what can be hovered or pressed. A page that uses
 * the keyboard or a drag (a radial tree, a ring, a list) answers {@link #keyPressed} and {@link #mouseDragged} first; what it leaves
 * (false) goes to the hub: the arrows, Tab and the number keys.
 */
public interface HubPageView {
    void render(HubScreen.Frame f);

    /** True when the page used the key ({@code modifiers} are GLFW's); the screen then does nothing with it. */
    default boolean keyPressed(int key, int modifiers) {
        return false;
    }

    /** A typed character, for a page with a text field; true when it was taken. */
    default boolean charTyped(char c, int modifiers) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, double dx, double dy, int button) {
        return false;
    }
}
