package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * The hub's tab in Tensura's menu: a fifth tab on the right, under Tensura's three "coming soon" tabs. Added with
 * {@code ScreenEvent.Init.Post}, no mixin. Tensura's menu screens are matched by package name, and the right-hand tab column is found
 * from the screen's own buttons (Tensura's {@code SimpleButton}s of 21 x 24 at one x, 26 apart): the tab goes 26 below the lowest. If a
 * menu screen has no such column, no tab is added and that is logged once.
 */
public final class TensuraMenuTab {
    private static final String MENU_PACKAGE = "io.github.manasmods.tensura.client.screen.";
    private static final String MAIN_SCREEN = MENU_PACKAGE + "MainScreen";
    private static final String TAB_CLASS = "io.github.manasmods.tensura.client.screen.widgets.SimpleButton";
    private static final int TAB_W = 21;
    private static final int TAB_H = 24;
    private static final int STEP = 26;
    // Tensura's own tab frame, drawn by resource location (never copied): normal state on top, hovered below
    private static final ResourceLocation RIGHT_TAB = ResourceLocation.fromNamespaceAndPath("tensura", "textures/gui/right_tab.png");
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hub/grimoire/widgets.png");
    private static boolean warned;

    private TensuraMenuTab() {
    }

    static void onInit(ScreenEvent.Init.Post e) {
        String cls = e.getScreen().getClass().getName();
        if (!cls.startsWith(MENU_PACKAGE) || !HubHooks.hubAvailable()) {
            return;
        }
        List<AbstractWidget> tabs = e.getListenersList().stream()
                .filter(l -> l instanceof AbstractWidget w && w.getClass().getName().equals(TAB_CLASS) && w.getWidth() == TAB_W && w.getHeight() == TAB_H)
                .map(l -> (AbstractWidget) l).toList();
        Integer x = tabs.stream().map(AbstractWidget::getX).reduce((a, b) -> a.equals(b) ? a : Integer.MIN_VALUE).orElse(null);
        if (tabs.isEmpty() || x == null || x == Integer.MIN_VALUE) {
            if (cls.equals(MAIN_SCREEN) && !warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("Tensura's menu has no right-hand tab column this mod recognises; the hub tab is not added (open the hub with its key)");
            }
            return;
        }
        int y = tabs.stream().mapToInt(AbstractWidget::getY).max().orElseThrow() + STEP;
        e.addListener(new HubTab(x, y));
    }

    /** The tab: Tensura's frame with our crescent; it opens the hub. */
    private static final class HubTab extends AbstractButton {
        HubTab(int x, int y) {
            super(x, y, TAB_W, TAB_H, Component.translatable("supernaturalbloodlines.hub.tensura_tab"));
            setTooltip(Tooltip.create(Component.translatable("supernaturalbloodlines.hub.tensura_tab")));
        }

        @Override
        public void onPress() {
            HubHooks.openHub();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.blit(RIGHT_TAB, getX(), getY(), 0, isHoveredOrFocused() ? TAB_H : 0, TAB_W, TAB_H, TAB_W, TAB_H * 2);
            g.setColor(0.91f, 0.69f, 0.69f, 1f);
            g.blit(ICONS, getX() + 3, getY() + 4, 16, 16, 0, 24, 8, 8, 64, 32);
            g.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }
}
