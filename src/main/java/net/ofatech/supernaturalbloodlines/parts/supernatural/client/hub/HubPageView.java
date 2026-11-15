package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

/** One page's content. It draws into the theme's regions through the frame and registers what can be hovered or pressed. */
public interface HubPageView {
    void render(HubScreen.Frame f);
}
