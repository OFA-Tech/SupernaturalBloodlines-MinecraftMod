package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Narration;
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
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmModel;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusOrder;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Counters;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ModelDrag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;
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
    private Component dialogSafe = Component.empty();
    /** The emblem's turn, owned here so it survives a theme or page change and every theme's model answers the same drag. */
    private final ModelDrag modelDrag = new ModelDrag();
    @Nullable
    private Rect emblemRect;
    private boolean dragEmblem;
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

    public void setPage(HubPage p) {
        if (p != page) {
            page = p;
            scroll.clear();
            HubClient.remember(p);
            triggerImmediateNarration(true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (HubDev.quiet()) {
            mouseX = -1000;
            mouseY = -1000;
        }
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
        reserved.clear();
        emblemRect = null;
        Frame f = new Frame(g, mouseX, mouseY, view);
        for (int i = 0; i < visible.size(); i++) {
            HubPage p = visible.get(i);
            Rect r = layout.tabs().get(i);
            boolean locked = p.locked(view.context());
            f.targets.add(new Target("tab:" + p.name(), r, null, true, locked ? List.of(Component.translatable(p.langKey()), Component.translatable("supernaturalbloodlines.hub.locked.tab"))
                    : List.of(Component.translatable(p.langKey())), () -> setPage(p)));
            theme.drawTab(g, r, p, p == page, f.focused("tab:" + p.name()));
            if (locked) {
                // dimmed, with a lock over it: the page opens (to say what it needs) but the tab reads as shut
                g.fill(r.x(), r.y(), r.right(), r.bottom(), (theme.paper() & 0xFFFFFF) | 0x99000000);
                boolean row = r.w() > 2 * r.h(); // a wide list row gets its lock at the end, a round or square button in the middle
                KitDraw.icon(g, net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons.LOCK, row ? r.right() - 13 : r.x() + (r.w() - 10) / 2, r.y() + (r.h() - 10) / 2, 10, theme.muted());
            }
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
        viewOf(page, view).render(f);
        targets = f.targets;
        for (Map.Entry<String, Integer> e : maxScroll.entrySet()) {
            if (e.getValue() > 0 && layout.regions().containsKey(e.getKey())) {
                theme.drawScrollbar(g, viewport(e.getKey()), scroll.getOrDefault(e.getKey(), 0), e.getValue());
            }
        }
        theme.drawFooter(f, layout, page);
        followFocus();
        if (menu) {
            drawThemeMenu(g, mouseX, mouseY);
            return; // no tooltip behind an open menu
        }
        if (dialog != null) {
            KitDraw.confirm(f, theme, width, height, dialogTitle, dialogBody, dialogSafe, dialogYes, dialog.confirmFocused(), dialogLayout());
            return;
        }
        tooltip(g, mouseX, mouseY);
    }

    /** Pixels at the bottom of a region that a page keeps for something pinned (a wax seal), set by {@link Frame#reserve} before {@link Frame#begin}. */
    private final Map<String, Integer> reserved = new java.util.HashMap<>();

    /** The part of a region that scrolls: the region less what the page reserved at its bottom. */
    private Rect viewport(String name) {
        Rect r = layout.region(name);
        int keep = reserved.getOrDefault(name, 0);
        return keep <= 0 ? r : new Rect(r.x(), r.y(), r.w(), Math.max(0, r.h() - keep));
    }

    /** The view of a page in the current theme; the page answers keys and drags first, see {@link #keyPressed}. */
    private HubPageView viewOf(HubPage p, HubView view) {
        HubThemeViews views = theme instanceof HubThemeViews v ? v : null;
        return p.locked(view.context()) ? HubPages.locked() : views != null ? views.view(p) : HubPages.shared(p);
    }

    @Nullable
    private HubPageView currentView() {
        return minecraft == null || minecraft.player == null ? null : viewOf(page, HubView.of(minecraft.player));
    }

    private ConfirmLayout dialogLayout() {
        return ConfirmLayout.of(width, height, font.split(dialogBody, ConfirmLayout.textWidth(width)).size());
    }

    /** The theme's own chrome: the theme switch, the close button and the counters, when its layout has them. */
    private void chrome(Frame f, int mouseX, int mouseY) {
        Rect cr = layout.chrome().counters();
        if (cr != null) {
            SupernaturalState st = f.view.state();
            theme.drawCounters(f, cr, new Counters(st.sp(), HubHooks.tensura().map(t -> (long) t.ep()).orElse(null)));
        }
        Rect tb = layout.chrome().themeButton();
        if (tb != null) {
            f.targets.add(new Target("theme", tb, null, true, List.of(Component.translatable("supernaturalbloodlines.hub.theme.switch")), () -> openThemeMenu()));
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
    /** Opens another mod's screen from the hub; Esc out of it brings the hub back on this page. */
    public void launch(HubHooks.Launcher launcher) {
        HubClient.launch(launcher, page);
    }

    private boolean menu;
    private int menuIndex;
    private int ladderChoice;

    /** Which ladder of several the Path shows (0 with one). */
    public int ladderIndex(int count) {
        return Math.max(0, Math.min(count - 1, ladderChoice));
    }

    public void setLadder(int index) {
        ladderChoice = index;
    }

    /** Development only (the dev screenshots): opens the look menu. */
    void devOpenMenu() {
        openThemeMenu();
    }

    private void openThemeMenu() {
        menu = true;
        menuIndex = themeId.ordinal();
    }

    private boolean themeAvailable(HubClientConfig.ThemeId id) {
        return id == HubClientConfig.ThemeId.GRIMOIRE || (HubHooks.theme(id.id()).isPresent() && !(HubDev.noTensura() && id == HubClientConfig.ThemeId.TENSURA));
    }

    /** The menu's rows, under the theme button and right-aligned to it, kept on the screen. */
    private List<Rect> menuRows() {
        Rect tb = layout.chrome().themeButton();
        int n = HubClientConfig.ThemeId.values().length;
        int w = 176;
        int x = tb == null ? width - w - 6 : Math.max(4, Math.min(width - w - 4, tb.right() - w));
        int y = tb == null ? 24 : tb.bottom() + 3;
        List<Rect> rows = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new Rect(x + 3, Math.min(y + 3 + i * 18, height - 21 * (n - i)), w - 6, 16));
        }
        return rows;
    }

    private void chooseTheme(int index) {
        HubClientConfig.ThemeId id = HubClientConfig.ThemeId.values()[index];
        if (themeAvailable(id)) {
            HubClientConfig.setTheme(id);
        }
        menu = false;
    }

    /** The look menu: every theme, the one in use ticked, one that cannot be drawn greyed with what it needs. */
    private void drawThemeMenu(GuiGraphics g, int mouseX, int mouseY) {
        List<Rect> rows = menuRows();
        Rect first = rows.get(0);
        Rect last = rows.get(rows.size() - 1);
        Rect box = new Rect(first.x() - 3, first.y() - 3, first.w() + 6, last.bottom() - first.y() + 6);
        g.fill(box.x(), box.y(), box.right(), box.bottom(), 0xFF000000 | theme.paper());
        g.renderOutline(box.x(), box.y(), box.w(), box.h(), theme.accent());
        HubClientConfig.ThemeId[] all = HubClientConfig.ThemeId.values();
        for (int i = 0; i < all.length; i++) {
            Rect r = rows.get(i);
            boolean ok = themeAvailable(all[i]);
            boolean hot = i == menuIndex || r.contains(mouseX, mouseY);
            if (hot) {
                g.fill(r.x(), r.y(), r.right(), r.bottom(), (theme.accent() & 0xFFFFFF) | 0x33000000);
                if (i == menuIndex) {
                    g.renderOutline(r.x(), r.y(), r.w(), r.h(), theme.focusColor());
                }
            }
            Component name = Component.translatable("supernaturalbloodlines.hub.theme." + all[i].id());
            g.drawString(font, name, r.x() + 6, r.y() + 4, ok ? theme.ink() : theme.muted(), false);
            if (!ok) {
                Component need = Component.translatable("supernaturalbloodlines.hub.theme.needs." + all[i].id());
                g.drawString(font, need, r.right() - font.width(need) - 5, r.y() + 4, theme.muted(), false);
            } else if (all[i] == themeId) {
                g.drawString(font, "\u2713", r.right() - 12, r.y() + 4, theme.accent(), false);
            }
        }
    }
    public void confirm(Component title, Component body, Component safe, Component yes, Runnable action) {
        dialog = new ConfirmModel();
        dialogSafe = safe;
        dialogTitle = title;
        dialogBody = body;
        dialogYes = yes;
        dialogAction = action;
        triggerImmediateNarration(true);
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
            int next = Math.min(maxScroll.getOrDefault(t.region(), 0), HubFocus.scrollToShow(now, viewport(t.region()), t.rect()));
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
        if (menu && dialog == null) {
            List<Rect> rows = menuRows();
            for (int i = 0; i < rows.size(); i++) {
                if (button == 0 && rows.get(i).contains(mouseX, mouseY)) {
                    chooseTheme(i);
                    return true;
                }
            }
            menu = false; // a click anywhere else closes it
            return true;
        }
        if (dialog != null) {
            if (button != 0) {
                return true; // only the left mouse button answers a dialog: a right click on Leave pack does not leave
            }
            ConfirmLayout cl = dialogLayout();
            if (cl.panel().contains(mouseX, mouseY) && !cl.safe().contains(mouseX, mouseY) && !cl.confirm().contains(mouseX, mouseY)) {
                return true;
            }
            dialogResult(ConfirmModel.click(cl.confirm().contains(mouseX, mouseY)));
            return true;
        }
        dragEmblem = button == 0 && emblemRect != null && emblemRect.contains(mouseX, mouseY);
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dialog != null) {
            return true;
        }
        HubPageView v = currentView();
        if (v != null && v.mouseDragged(mouseX, mouseY, dx, dy, button)) {
            return true;
        }
        if (dragEmblem && button == 0) {
            modelDrag.drag(dx);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragEmblem = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog != null) {
            return true; // nothing scrolls behind a dialog
        }
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
        if (menu && dialog == null) {
            if (HubKeybinds.HUB.matches(key, scanCode)) {
                onClose(); // the hub key closes the hub even with the look menu open
                return true;
            }
            int n = HubClientConfig.ThemeId.values().length;
            switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> menu = false;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> menuIndex = (menuIndex + 1) % n;
                case GLFW.GLFW_KEY_UP -> menuIndex = (menuIndex + n - 1) % n;
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> chooseTheme(menuIndex);
                default -> { }
            }
            return true; // the menu takes every key while it is open
        }
        if (dialog != null) {
            // the hub key cancels, like Escape: the dialog never traps a player who pressed the key that opened the hub
            ConfirmModel.Key k = HubKeybinds.HUB.matches(key, scanCode) ? ConfirmModel.Key.ESCAPE : switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> ConfirmModel.Key.ESCAPE;
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> ConfirmModel.Key.ENTER;
                case GLFW.GLFW_KEY_TAB -> ConfirmModel.Key.TAB;
                case GLFW.GLFW_KEY_LEFT -> ConfirmModel.Key.LEFT;
                case GLFW.GLFW_KEY_RIGHT -> ConfirmModel.Key.RIGHT;
                default -> null;
            };
            if (k != null) {
                dialogResult(dialog.press(k));
                if (dialog != null) {
                    triggerImmediateNarration(false); // the focus moved to the other button
                }
            }
            return true; // a dialog takes every key: nothing behind it moves
        }
        if (HubKeybinds.HUB.matches(key, scanCode)) {
            onClose(); // the hub key always wins, even bound to a digit
            return true;
        }
        // the page asks first: a radial tree or a ring uses the arrows, a list may use Enter; what it leaves goes to the hub
        HubPageView v = currentView();
        if (v != null && v.keyPressed(key, modifiers)) {
            return true;
        }
        boolean plain = (modifiers & (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0;
        var jump = plain ? FocusOrder.pageForKey(key, visible.size()) : java.util.OptionalInt.empty();
        if (jump.isPresent()) {
            setPage(visible.get(jump.getAsInt()));
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
        triggerImmediateNarration(false);
    }

    /** What a screen reader says: the page and the focused target, or, over a confirm dialog, the dialog and its focused button (see {@link Narration}). */
    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        Target t = target(focus);
        List<String> tip = t == null ? null : t.tooltip().stream().map(Component::getString).toList();
        String focused = dialog == null ? null : (dialog.confirmFocused() ? dialogYes : dialogSafe).getString();
        for (String line : Narration.lines(Component.translatable(page.langKey()).getString(), tip,
                dialog == null ? null : dialogTitle.getString(), dialog == null ? null : dialogBody.getString(), focused)) {
            output.add(NarratedElementType.TITLE, line);
        }
    }

    /** PgUp / PgDn: the focused target's region, else the main one. */
    private void pageScroll(int direction) {
        Target t = target(focus);
        String region = t != null && t.region() != null ? t.region() : HubLayout.MAIN;
        if (layout != null && layout.regions().containsKey(region)) {
            scroll.put(region, HubFocus.pageScroll(scroll.getOrDefault(region, 0), maxScroll.getOrDefault(region, 0), viewport(region), direction));
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

        /**
         * Hands the selection's detail to the theme: it draws it in its own place (the opposite page, a bottom bar, an info pane). The current
         * region and its clip are set aside while it draws and put back after, so a place outside MAIN is seen and clickable.
         */
        public void detail(Detail d) {
            Rect r = layout.chrome().detail() != null ? layout.chrome().detail() : layout.region(HubLayout.SIDE);
            String savedRegion = region;
            Rect savedClip = clip;
            if (savedClip != null) {
                g.disableScissor();
            }
            region = null;
            clip = null;
            theme.drawDetail(this, r, d);
            region = savedRegion;
            clip = savedClip;
            if (savedClip != null) {
                g.enableScissor(savedClip.x(), savedClip.y(), savedClip.right(), savedClip.bottom());
            }
        }

        /** The emblem in {@code r} (a page picks the place; {@code Chrome.emblem} is the theme's default): the live model, turned by dragging. */
        public void emblem(Rect r) {
            List<HubModel.LadderView> ladders = view.ladders();
            float progress = ladders.isEmpty() ? 0f : HubModel.progress(ladders.get(ladderIndex(ladders.size())));
            List<EmblemData.Rung> rungs = ladders.isEmpty() ? List.of()
                    : ladders.get(ladderIndex(ladders.size())).rungs().stream().map(rr -> new EmblemData.Rung(rr.name(), rr.status())).toList();
            emblemRect = r;
            theme.drawEmblem(this, r, new EmblemData(progress, rungs, view.player(), modelDrag));
        }

        public void emblem() {
            Rect r = layout.chrome().emblem();
            if (r != null) {
                emblem(r);
            }
        }

        @Override
        public void confirm(Component title, Component body, Component safe, Component yes, Runnable action) {
            HubScreen.this.confirm(title, body, safe, yes, action);
        }

        /** Asks first with the standard "Stay" as the safe button. */
        public void confirm(Component title, Component body, Component yes, Runnable action) {
            HubScreen.this.confirm(title, body, Component.translatable("supernaturalbloodlines.hub.confirm.stay"), yes, action);
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

        /** Keeps {@code pixels} at the bottom of a region out of its scrolling content, so something pinned there (a seal) is never cut; call before {@link #begin}. */
        public void reserve(String name, int pixels) {
            reserved.put(name, pixels);
        }

        /** Starts drawing into a region: clips to it and returns the top y, moved by its scroll. */
        public int begin(String name) {
            region = name;
            clip = viewport(name);
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
