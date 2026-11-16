package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.HubKeybinds;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ClaimTaskPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmModel;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusOrder;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.UiContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The hub: one screen model (the page, the selected task, the keyboard focus by target id, the scroll of each named region) drawn by a
 * {@link HubTheme}, which also picks the view of each page. A theme change in the client config swaps the theme on the next tick and
 * keeps the page. Every action is a server-validated packet.
 */
public final class HubScreen extends Screen {
    /**
     * Something on screen that has a tooltip and may be pressed. {@code region} is where it scrolls (null for fixed ones like tabs);
     * {@code shown} is false while it is scrolled out of view: it can still take the keyboard focus, which scrolls it back.
     */
    public record Target(String id, Rect rect, @Nullable String region, boolean shown, List<Component> tooltip, @Nullable Runnable action) {
    }

    private HubPage page;
    private String selectedTask;
    private String focus;
    private boolean scrollToFocus;
    private boolean keyboard;
    private final Map<String, Integer> scroll = new HashMap<>();
    private final Map<String, Integer> maxScroll = new HashMap<>();
    private HubClientConfig.ThemeId themeId;
    private HubTheme theme;
    private List<Target> targets = List.of();
    private List<HubPage> visible = List.of(HubPage.PATH);
    private HubLayout layout;
    @Nullable
    private ConfirmModel dialog;
    private Component dialogTitle = Component.empty();
    private Component dialogBody = Component.empty();
    private Component dialogYes = Component.empty();
    private Runnable dialogAction = () -> { };

    HubScreen(HubPage page) {
        super(Component.translatable("supernaturalbloodlines.hub.title"));
        this.page = page;
        applyTheme();
    }

    private void applyTheme() {
        themeId = HubClientConfig.theme();
        // the themes are registered by the parts that bring them (HubHooks.addTheme); a missing one, or one of the wrong type, is the Grimoire
        HubClientConfig.ThemeId shown = HubClientConfig.resolve(themeId, id -> HubHooks.theme(id).isPresent());
        theme = HubHooks.theme(shown.id()).map(t -> t.factory().get()).orElseGet(GrimoireTheme::new);
    }

    @Override
    public void tick() {
        if (HubClientConfig.theme() != themeId) {
            applyTheme();
            scroll.clear();
        }
    }

    public HubPage page() {
        return page;
    }

    void setPage(HubPage p) {
        if (p != page) {
            page = p;
            scroll.clear();
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
        List<HubHooks.Launcher> launchers = HubHooks.launchers();
        layout = theme.layout(width, height, visible.size(), launchers.size());
        theme.drawFrame(g, layout);
        maxScroll.clear();
        Frame f = new Frame(g, mouseX, mouseY, view);
        for (int i = 0; i < visible.size(); i++) {
            HubPage p = visible.get(i);
            Rect r = layout.tabs().get(i);
            f.targets.add(new Target("tab:" + p.name(), r, null, true, List.of(Component.translatable(p.langKey())), () -> setPage(p)));
            theme.drawTab(g, r, p, p == page, f.focused("tab:" + p.name()));
        }
        for (int i = 0; i < launchers.size(); i++) {
            HubHooks.Launcher l = launchers.get(i);
            Rect r = layout.seals().get(i);
            String id = "seal:" + l.id();
            f.targets.add(new Target(id, r, null, true, List.of(l.name(), Component.translatable("supernaturalbloodlines.hub.launcher.hint", l.name())),
                    () -> HubClient.launch(l, page)));
            theme.drawLauncher(g, r, l, r.contains(mouseX, mouseY) || f.focused(id));
            if (f.focused(id)) {
                theme.drawFocus(g, r);
            }
        }
        chrome(f, mouseX, mouseY);
        HubThemeViews views = theme instanceof HubThemeViews v ? v : null;
        if (views != null) {
            views.drawOverlay(f, page);
        }
        (page.locked(view.context()) ? HubPages.locked() : views != null ? views.view(page) : HubPages.shared(page)).render(f);
        targets = f.targets;
        for (Map.Entry<String, Integer> e : maxScroll.entrySet()) {
            if (e.getValue() > 0 && layout.regions().containsKey(e.getKey())) {
                theme.drawScrollbar(g, layout.region(e.getKey()), scroll.getOrDefault(e.getKey(), 0), e.getValue());
            }
        }
        followFocus();
        if (dialog != null) {
            ConfirmLayout cl = ConfirmLayout.of(width, height);
            KitDraw.confirm(f, theme, width, height, dialogTitle, dialogBody, Component.translatable("supernaturalbloodlines.hub.confirm.cancel"), dialogYes,
                    dialog.confirmFocused(), cl.cancel(), cl.confirm(), cl.panel(), cl.text());
            return;
        }
        tooltip(g, mouseX, mouseY);
    }

    /** The theme's own chrome: the theme switch and the close button, when its layout has them. */
    private void chrome(Frame f, int mouseX, int mouseY) {
        Rect tb = layout.chrome().themeButton();
        if (tb != null) {
            HubClientConfig.ThemeId next = HubClientConfig.next(themeId, id -> HubHooks.theme(id).isPresent());
            f.targets.add(new Target("theme", tb, null, true, List.of(Component.translatable("supernaturalbloodlines.hub.theme.switch",
                    Component.translatable("supernaturalbloodlines.hub.theme." + next.id()))), () -> HubClientConfig.setTheme(next)));
            theme.drawThemeButton(f.g, tb, tb.contains(mouseX, mouseY), f.focused("theme"));
        }
        Rect cb = layout.chrome().close();
        if (cb != null) {
            f.targets.add(new Target("close", cb, null, true, List.of(Component.translatable("supernaturalbloodlines.hub.close")), this::onClose));
            theme.drawClose(f.g, cb, cb.contains(mouseX, mouseY), f.focused("close"));
        }
    }

    /**
     * Asks before something that cannot be undone. Esc and a click outside cancel, Enter on the safe button cancels, so only a deliberate move
     * to {@code yes} confirms.
     */
    public void confirm(Component title, Component body, Component yes, Runnable action) {
        dialog = new ConfirmModel();
        dialogTitle = title;
        dialogBody = body;
        dialogYes = yes;
        dialogAction = action;
    }

    private void dialogResult(ConfirmModel.Result r) {
        if (r == ConfirmModel.Result.NONE) {
            return;
        }
        Runnable action = dialogAction;
        dialog = null;
        dialogAction = () -> { };
        if (r == ConfirmModel.Result.CONFIRM) {
            action.run();
        }
    }

    /** After a keyboard move, scrolls the focused target's region so it shows (it is drawn there from the next frame). */
    private void followFocus() {
        if (!scrollToFocus) {
            return;
        }
        scrollToFocus = false;
        Target t = target(focus);
        if (t != null && t.region() != null && layout.regions().containsKey(t.region())) {
            int now = scroll.getOrDefault(t.region(), 0);
            int next = Math.min(maxScroll.getOrDefault(t.region(), 0), HubFocus.scrollToShow(now, layout.region(t.region()), t.rect()));
            scroll.put(t.region(), next);
        }
    }

    private void tooltip(GuiGraphics g, int mouseX, int mouseY) {
        Target hovered = null;
        for (Target t : targets) {
            if (t.shown() && t.rect().contains(mouseX, mouseY)) {
                hovered = t;
            }
        }
        if (hovered != null) {
            g.renderComponentTooltip(font, hovered.tooltip(), mouseX, mouseY);
            return;
        }
        Target focused = keyboard ? target(focus) : null;
        if (focused != null && focused.shown()) {
            g.renderComponentTooltip(font, focused.tooltip(), focused.rect().x(), focused.rect().bottom() + 12);
        }
    }

    @Nullable
    private Target target(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (Target t : targets) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g); // no blur: the book is the screen, and blur makes its pixel art muddy
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialog != null) {
            ConfirmLayout cl = ConfirmLayout.of(width, height);
            if (cl.panel().contains(mouseX, mouseY) && !cl.cancel().contains(mouseX, mouseY) && !cl.confirm().contains(mouseX, mouseY)) {
                return true;
            }
            dialogResult(ConfirmModel.click(cl.confirm().contains(mouseX, mouseY)));
            return true;
        }
        if (button == 0) {
            for (Target t : targets) {
                if (t.shown() && t.action() != null && t.rect().contains(mouseX, mouseY)) {
                    focus = t.id();
                    keyboard = false; // the mouse moved the focus: no tooltip pinned to it
                    t.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        String region = regionAt(mouseX, mouseY);
        if (region == null) {
            return false;
        }
        scroll.put(region, Math.max(0, Math.min(maxScroll.getOrDefault(region, 0), scroll.getOrDefault(region, 0) - (int) Math.signum(scrollY) * 10)));
        return true;
    }

    @Nullable
    private String regionAt(double x, double y) {
        if (layout == null) {
            return null;
        }
        for (Map.Entry<String, Rect> e : layout.regions().entrySet()) {
            if (e.getValue().contains(x, y)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (dialog != null) {
            ConfirmModel.Key k = switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> ConfirmModel.Key.ESCAPE;
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> ConfirmModel.Key.ENTER;
                case GLFW.GLFW_KEY_TAB -> ConfirmModel.Key.TAB;
                case GLFW.GLFW_KEY_LEFT -> ConfirmModel.Key.LEFT;
                case GLFW.GLFW_KEY_RIGHT -> ConfirmModel.Key.RIGHT;
                default -> null;
            };
            if (k != null) {
                dialogResult(dialog.press(k));
            }
            return true; // a dialog takes every key: nothing behind it moves
        }
        var jump = FocusOrder.pageForKey(key, visible.size());
        if (jump.isPresent()) {
            setPage(visible.get(jump.getAsInt()));
            return true;
        }
        if (HubKeybinds.HUB.matches(key, scanCode)) {
            onClose();
            return true;
        }
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> moveFocus(shift ? -1 : 1);
            case GLFW.GLFW_KEY_DOWN -> moveFocus(1);
            case GLFW.GLFW_KEY_UP -> moveFocus(-1);
            case GLFW.GLFW_KEY_LEFT -> setPage(HubFocus.page(visible, page, -1));
            case GLFW.GLFW_KEY_RIGHT -> setPage(HubFocus.page(visible, page, 1));
            case GLFW.GLFW_KEY_PAGE_DOWN -> pageScroll(1);
            case GLFW.GLFW_KEY_PAGE_UP -> pageScroll(-1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                Target t = target(focus);
                if (t != null && t.action() != null) {
                    t.action().run();
                }
            }
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    private void moveFocus(int delta) {
        keyboard = true;
        focus = HubFocus.move(FocusOrder.order(targets.stream().map(Target::id).toList()), focus, delta);
        scrollToFocus = true;
    }

    /** PgUp / PgDn: the focused target's region, else the main one. */
    private void pageScroll(int direction) {
        Target t = target(focus);
        String region = t != null && t.region() != null ? t.region() : HubLayout.MAIN;
        if (layout != null && layout.regions().containsKey(region)) {
            scroll.put(region, HubFocus.pageScroll(scroll.getOrDefault(region, 0), maxScroll.getOrDefault(region, 0), layout.region(region), direction));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** What a page draws with during one frame: the theme, the named regions, and the targets it registers. */
    public final class Frame implements UiContext {
        public final GuiGraphics g;
        public final int mouseX;
        public final int mouseY;
        public final HubView view;
        final List<Target> targets = new ArrayList<>();
        @Nullable
        private String region;
        @Nullable
        private Rect clip;

        Frame(GuiGraphics g, int mouseX, int mouseY, HubView view) {
            this.g = g;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.view = view;
        }

        @Override
        public GuiGraphics g() {
            return g;
        }

        public HubTheme theme() {
            return theme;
        }

        /** Hands the selection's detail to the theme: it draws it in its own place (the opposite page, a bottom bar, an info pane). */
        public void detail(Detail d) {
            Rect r = layout.chrome().detail() != null ? layout.chrome().detail() : layout.region(HubLayout.SIDE);
            theme.drawDetail(this, r, d);
        }

        public void confirm(Component title, Component body, Component yes, Runnable action) {
            HubScreen.this.confirm(title, body, yes, action);
        }

        public HubScreen screen() {
            return HubScreen.this;
        }

        @Override
        public Font font() {
            return font;
        }

        public HubLayout layout() {
            return layout;
        }

        public Rect region(String name) {
            return layout.region(name);
        }

        /** Starts drawing into a region: clips to it and returns the top y, moved by its scroll. */
        public int begin(String name) {
            region = name;
            clip = layout.region(name);
            g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
            return clip.y() - scroll.getOrDefault(name, 0);
        }

        /** Ends the region; {@code bottom} is where its content ended, which bounds its scroll. */
        public void end(int bottom) {
            g.disableScissor();
            int now = scroll.getOrDefault(region, 0);
            int max = Math.max(0, bottom + now - clip.bottom());
            maxScroll.put(region, max);
            scroll.put(region, Math.min(now, max));
            region = null;
            clip = null;
        }

        /** Registers a target; one scrolled out of its region can take the focus but not the mouse. Ids are unique within a frame. */
        @Override
        public void target(String id, Rect r, List<Component> tooltip, @Nullable Runnable action) {
            targets.add(new Target(id, r, region, clip == null || r.overlaps(clip), tooltip, action));
        }

        @Override
        public boolean hovered(Rect r) {
            return r.contains(mouseX, mouseY) && (clip == null || clip.contains(mouseX, mouseY));
        }

        @Override
        public boolean focused(String id) {
            return id.equals(focus);
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
