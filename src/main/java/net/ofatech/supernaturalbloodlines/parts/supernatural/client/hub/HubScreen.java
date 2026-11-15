package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PlaceholderPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.QuestsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ClaimTaskPayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The hub: one screen model (the page, the selected task, the keyboard focus, the scroll of each region) drawn by a {@link HubTheme}.
 * A theme change in the client config swaps the theme on the next tick and keeps the page. Every action is a server-validated packet.
 */
public final class HubScreen extends Screen {
    /** Something on screen that has a tooltip and may be pressed. */
    public record Target(Rect rect, List<Component> tooltip, @Nullable Runnable action) {
    }

    private static final Map<HubPage, HubPageView> PAGES = new EnumMap<>(Map.of(HubPage.PATH, new PathPage(), HubPage.QUESTS, new QuestsPage()));
    private static final HubPageView PLACEHOLDER = new PlaceholderPage();

    private HubPage page;
    private String selectedTask;
    private int focus = -1;
    private final int[] scroll = new int[2];
    private final int[] maxScroll = new int[2];
    private HubClientConfig.ThemeId themeId;
    private HubTheme theme;
    private List<Target> targets = List.of();
    private List<HubPage> visible = List.of(HubPage.PATH);
    private HubLayout layout;

    HubScreen(HubPage page) {
        super(Component.translatable("supernaturalbloodlines.hub.title"));
        this.page = page;
        applyTheme();
    }

    private void applyTheme() {
        themeId = HubClientConfig.theme();
        theme = switch (HubClientConfig.resolve(themeId, ModList.get().isLoaded("tensura"))) {
            case GRIMOIRE, BLOOD_MOON, TENSURA -> new GrimoireTheme();
        };
    }

    @Override
    public void tick() {
        if (HubClientConfig.theme() != themeId) {
            applyTheme();
        }
    }

    public HubPage page() {
        return page;
    }

    void setPage(HubPage p) {
        if (p != page) {
            page = p;
            scroll[0] = scroll[1] = 0;
            focus = -1;
            HubClient.remember(p);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        HubView view = HubView.of(minecraft.player);
        visible = HubPage.visiblePages(view.context());
        setPage(HubPage.orFirst(page, view.context()));
        layout = theme.layout(width, height, visible.size());
        theme.drawFrame(g, layout);
        Frame f = new Frame(g, mouseX, mouseY, view);
        for (int i = 0; i < visible.size(); i++) {
            HubPage p = visible.get(i);
            Rect r = layout.tabs().get(i);
            f.targets.add(new Target(r, List.of(Component.translatable(p.langKey())), () -> setPage(p)));
            theme.drawTab(g, r, p, p == page, f.focused(r));
        }
        (page.locked(view.context()) ? PLACEHOLDER : PAGES.getOrDefault(page, PLACEHOLDER)).render(f);
        targets = f.targets;
        Target tip = null;
        for (Target t : targets) {
            if (t.rect().contains(mouseX, mouseY)) {
                tip = t;
            }
        }
        if (tip == null && focus >= 0 && focus < targets.size()) {
            tip = targets.get(focus);
            g.renderComponentTooltip(font, tip.tooltip(), tip.rect().x(), tip.rect().bottom() + 12);
        } else if (tip != null) {
            g.renderComponentTooltip(font, tip.tooltip(), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g); // no blur: the book is the screen, and blur makes its pixel art muddy
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Target t : targets) {
                if (t.action() != null && t.rect().contains(mouseX, mouseY)) {
                    t.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (layout == null) {
            return false;
        }
        int region = layout.left().contains(mouseX, mouseY) ? 0 : layout.right().contains(mouseX, mouseY) ? 1 : -1;
        if (region < 0) {
            return false;
        }
        scroll[region] = Math.max(0, Math.min(maxScroll[region], scroll[region] - (int) Math.signum(scrollY) * 10));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> focus = HubFocus.move(focus, targets.size(), shift ? -1 : 1);
            case GLFW.GLFW_KEY_DOWN -> focus = HubFocus.move(focus, targets.size(), 1);
            case GLFW.GLFW_KEY_UP -> focus = HubFocus.move(focus, targets.size(), -1);
            case GLFW.GLFW_KEY_LEFT -> setPage(HubFocus.page(visible, page, -1));
            case GLFW.GLFW_KEY_RIGHT -> setPage(HubFocus.page(visible, page, 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (focus >= 0 && focus < targets.size() && targets.get(focus).action() != null) {
                    targets.get(focus).action().run();
                }
            }
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** What a page draws with during one frame: the theme, the regions, and the targets it registers. */
    public final class Frame {
        public final GuiGraphics g;
        public final int mouseX;
        public final int mouseY;
        public final HubView view;
        final List<Target> targets = new ArrayList<>();
        private Rect clip;
        private final Rect focusedRect;

        Frame(GuiGraphics g, int mouseX, int mouseY, HubView view) {
            this.g = g;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.view = view;
            this.focusedRect = focus >= 0 && focus < HubScreen.this.targets.size() ? HubScreen.this.targets.get(focus).rect() : null;
        }

        public HubTheme theme() {
            return theme;
        }

        public HubScreen screen() {
            return HubScreen.this;
        }

        public Font font() {
            return font;
        }

        public HubLayout layout() {
            return layout;
        }

        public Rect region(int i) {
            return i == 0 ? layout.left() : layout.right();
        }

        /** Starts drawing into region {@code i} (0 left, 1 right): clips to it and returns the top y, moved by its scroll. */
        public int begin(int i) {
            clip = region(i);
            g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
            return clip.y() - scroll[i];
        }

        /** Ends region {@code i}; {@code bottom} is where its content ended, which bounds its scroll. */
        public void end(int i, int bottom) {
            g.disableScissor();
            maxScroll[i] = Math.max(0, bottom + scroll[i] - region(i).bottom());
            scroll[i] = Math.min(scroll[i], maxScroll[i]);
            clip = null;
        }

        /** Registers a target; one outside the visible part of the region is not hoverable or pressable. */
        public void target(Rect r, List<Component> tooltip, @Nullable Runnable action) {
            if (clip == null || r.overlaps(clip)) {
                targets.add(new Target(r, tooltip, action));
            }
        }

        public boolean hovered(Rect r) {
            return r.contains(mouseX, mouseY) && (clip == null || clip.contains(mouseX, mouseY));
        }

        public boolean focused(Rect r) {
            return r.equals(focusedRect);
        }

        public String selectedTask() {
            return selectedTask;
        }

        public void select(String taskId) {
            selectedTask = taskId;
        }

        /** Asks the server to claim; it re-checks everything and answers with the sync (and a message on a refusal). */
        public void claim(String taskId) {
            PacketDistributor.sendToServer(new ClaimTaskPayload(taskId));
        }
    }
}
