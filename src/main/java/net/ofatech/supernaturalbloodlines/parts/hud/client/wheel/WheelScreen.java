package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.ClickPlan;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Cell;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Hit;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Layout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelModel;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelNarration;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPalette;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelRules;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Radial.Dir;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import org.lwjgl.glfw.GLFW;

/**
 * The skill wheel. Hold G to open it, let go over a power to use it, or leave it open and click: left click uses, right click opens the slot picker, Alt + click cycles the
 * mode, scroll turns the page. Every use goes to the server, which checks it again; this screen only draws and sends. It does not pause the game. A held power is released
 * wherever the pointer is, and whenever the screen goes away (closed, replaced, focus lost, death).
 */
public final class WheelScreen extends Screen {
    private GestureMachine machine;
    private final HudKit kit;
    private final Look look;
    private final WheelPalette pal;
    private List<WheelEntry> list = List.of();
    private WheelModel<WheelEntry> model = new WheelModel<>(List.of(), s -> true);
    private WheelModel.Open open = WheelModel.Open.FIRST;
    private Layout layout;
    private Hit focus;
    private Hit hover;
    private WheelEntry heldByMouse;
    private boolean placed;
    /** The slot picker: the power it is for (an entry of the current page) and the slot under the pointer. */
    private WheelEntry pickerFor;
    private int pickerHover = -1;
    private int tabFocus = -1;

    public WheelScreen(GestureMachine machine) {
        super(Component.translatable("supernaturalbloodlines.wheel.hint.open"));
        this.machine = machine;
        HudStyle.Style style = HudStyle.current();
        this.kit = HudKit.of(style);
        this.look = switch (style) {
            case GRIMOIRE -> Look.GRIMOIRE;
            case BLOOD_MOON -> Look.BLOOD_MOON;
            case TENSURA -> Look.TENSURA;
        };
        this.pal = WheelPalette.of(look);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (net.neoforged.fml.ModList.get().isLoaded("tensura")) {
            TensuraHudHold.open();
        }
        WheelActions.view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer.WHEEL, true);
    }

    @Override
    public void removed() {
        if (net.neoforged.fml.ModList.get().isLoaded("tensura")) {
            TensuraHudHold.close();
        }
        DevWheel.active = false;
        WheelDescriptions.demo = null;
        DevWheel.alt = false;
        net.ofatech.supernaturalbloodlines.parts.hud.net.ClientCatalogue.frozen = false;
        WheelActions.release();
        WheelActions.view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer.WHEEL, false);
        heldByMouse = null;
        WheelInput.screenClosed(machine);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // the game stays visible behind the wheel: no blur, no dimming
    }

    // ---- state ----

    private void refresh() {
        List<WheelEntry> now = WheelList.current();
        if (now != list || model == null) {
            list = now;
            model = new WheelModel<>(list, WheelList::installed);
            if (!placed) {
                placed = true;
                open = locate();
            }
            open = model.fit(open);
        }
        List<WheelEntry> powers = model.powers(open);
        var mods = model.mods();
        int groups = mods.isEmpty() ? 0 : model.groups(mods.get(Math.max(0, Math.min(open.mod(), mods.size() - 1)))).size();
        layout = WheelLayout.compute(width, height, (int) minecraft.getWindow().getGuiScale(), look, mods.size(), open.mod(), groups, powers.size(), open.page());
        if (groups >= 2) {
            var gs = model.groups(mods.get(Math.max(0, Math.min(open.mod(), mods.size() - 1))));
            int[] widths = new int[gs.size()];
            for (int i = 0; i < widths.length; i++) {
                widths[i] = font.width(WheelRenderer.text("group." + gs.get(i).name().toLowerCase(Locale.ROOT))) + 10;
            }
            layout = layout.withLabelWidths(widths);
        }
    }

    /** The open path of the power the tap repeats, so the wheel opens on what the player used last; the first mod when there is none. */
    private WheelModel.Open locate() {
        var last = WheelActions.last().get();
        if (last.isEmpty()) {
            return WheelModel.Open.FIRST;
        }
        PowerRef ref = last.get().ref();
        var mods = model.mods();
        int mod = mods.indexOf(ref.source());
        if (mod < 0) {
            return WheelModel.Open.FIRST;
        }
        var groups = model.groups(ref.source());
        int group = 0;
        int page = 0;
        for (WheelEntry e : list) {
            if (e.ref().equals(ref)) {
                group = Math.max(0, groups.indexOf(e.group()));
                List<WheelEntry> inGroup = model.powers(ref.source(), groups.isEmpty() ? PowerGroup.MAIN : groups.get(group));
                page = Math.max(0, inGroup.indexOf(e)) / WheelLayout.PAGE_SIZE;
            }
        }
        return new WheelModel.Open(mod, group, page);
    }

    private List<WheelEntry> pageEntries() {
        List<WheelEntry> all = model.powers(open);
        int from = Math.min(all.size(), open.page() * WheelLayout.PAGE_SIZE);
        return all.subList(from, Math.min(all.size(), from + WheelLayout.PAGE_SIZE));
    }

    private WheelEntry entryAt(Hit h) {
        if (h == null || h.ring() != Ring.OUTER) {
            return null;
        }
        List<WheelEntry> page = pageEntries();
        return h.index() >= 0 && h.index() < page.size() ? page.get(h.index()) : null;
    }

    private Hit active() {
        return hover != null ? hover : focus;
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refresh();
        boolean pickerOpen = pickerFor != null;
        if (!pickerOpen && !DevWheel.active) {
            hover = layout.hit(mouseX, mouseY).orElse(null);
            if (hover != null && hover.ring() == Ring.INNER && hover.index() != open.mod()) {
                open = model.openMod(hover.index());
                refresh();
                hover = layout.hit(mouseX, mouseY).orElse(null);
            } else if (hover != null && hover.ring() == Ring.MIDDLE && hover.index() != open.group()) {
                open = model.openGroup(open, hover.index());
                refresh();
                hover = layout.hit(mouseX, mouseY).orElse(null);
            }
        }
        Hit shown = pickerOpen ? hitOf(pickerFor) : active();
        WheelEntry power = entryAt(shown);
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        WheelRenderer.Picker picker = pickerOpen ? picker(mouseX, mouseY) : null;
        WheelRenderer.draw(g, font, new WheelRenderer.Frame(layout, model, open, pageEntries(), shown, power, hasAltDown() || DevWheel.alt, pal, kit, now, picker, WheelActions::slotOf));
    }

    private Hit hitOf(WheelEntry e) {
        List<WheelEntry> page = pageEntries();
        int i = page.indexOf(e);
        return i < 0 ? null : new Hit(Ring.OUTER, i);
    }

    private WheelRenderer.Picker picker(int mouseX, int mouseY) {
        int idx = pageEntries().indexOf(pickerFor);
        WheelRules.SlotTarget target = WheelActions.target(pickerFor);
        Cell anchor = layout.pickerAnchor(Math.max(0, idx));
        if (target == WheelRules.SlotTarget.NONE) {
            return new WheelRenderer.Picker(pickerFor, anchor, List.of(), List.of(), -1, true);
        }
        List<ResourceLocation> holdIds = WheelActions.holds(target);
        List<String> ids = new ArrayList<>();
        List<WheelEntry> holds = new ArrayList<>();
        for (ResourceLocation id : holdIds) {
            ids.add(id == null ? "" : id.toString());
            holds.add(id == null ? null : find(id));
        }
        var slots = SlotPicker.slotsFor(target, ids, WheelActions.activePreset(), WheelActions.presetCount());
        pickerHover = -1;
        for (int i = 0; i < slots.size(); i++) {
            int sx = anchor.x() + 8 + i * 31;
            int sy = anchor.y() + 17;
            if (mouseX >= sx && mouseX < sx + 28 && mouseY >= sy && mouseY < sy + 28) {
                pickerHover = i;
            }
        }
        return new WheelRenderer.Picker(pickerFor, anchor, slots, holds, pickerHover, false);
    }

    /** What a slot holds, as the wheel lists it; a made-up entry (its id as the name) for a skill that is no wheel power. */
    private WheelEntry find(ResourceLocation id) {
        for (WheelEntry e : list) {
            if (e.ref().id().equals(id)) {
                return e;
            }
        }
        return new WheelEntry(new PowerRef(PowerSource.TENSURA, id), PowerGroup.SKILLS, PowerKind.INSTANT, Component.literal(id.getPath().replace('_', ' ')), Component.empty(),
                List.of(), ClientIcons.UNKNOWN, PowerState.READY, true, -1f);
    }

    // ---- what a power does ----

    private int modeOf(WheelEntry e) {
        return e.state().mode();
    }

    /** Uses the power under the pointer (or focus); true when something was started. */
    private boolean useActive() {
        WheelEntry e = entryAt(active());
        if (e == null) {
            return false;
        }
        if (WheelNarration.enter(e.state()).isPresent()) {
            minecraft.getNarrator().sayNow(Component.literal(WheelRenderer.reason(e.state().lock())));
            return false;
        }
        boolean started = WheelActions.use(e, modeOf(e));
        if (started && e.kind() == PowerKind.HELD) {
            heldByMouse = e;
        }
        if (started && (e.kind() == PowerKind.INSTANT || e.kind() == PowerKind.AIMED)) {
            onClose();
        } else if (started) {
            machine = machine.sticky();
        }
        return started;
    }

    private void feed(Input in, boolean overPower) {
        GestureMachine.Step s = machine.step(in, Util.getMillis(), HudClientConfig.wheelTapMs(), overPower);
        machine = s.machine();
        apply(s.effect());
    }

    private void apply(Effect effect) {
        switch (effect) {
            case CLOSE -> onClose();
            case USE_HOVERED -> useActive();
            default -> { }
        }
    }

    // ---- input ----

    @Override
    public void tick() {
        if (DevWheel.active) {
            return; // the screenshot driver: a hidden window is never focused
        }
        if (!minecraft.isWindowActive() || minecraft.player == null || !minecraft.player.isAlive()) {
            feed(Input.FOCUS_LOST, false);
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (pickerFor != null) {
                pickerFor = null;
                return true;
            }
            feed(Input.ESC, false);
            onClose();
            return true;
        }
        if (pickerFor != null) {
            for (int i = 0; i < HudKeys.SLOTS.length; i++) {
                if (HudKeys.SLOTS[i].matches(key, scan)) {
                    WheelActions.assign(pickerFor, i);
                    pickerFor = null;
                    return true;
                }
            }
            return true;
        }
        if (HudKeys.WHEEL.matches(key, scan)) {
            feed(Input.G_DOWN, false);
            return true;
        }
        for (int i = 0; i < HudKeys.SLOTS.length; i++) {
            if (HudKeys.SLOTS[i].matches(key, scan) && entryAt(active()) != null) {
                WheelActions.assign(entryAt(active()), i);
                return true;
            }
        }
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> arrow(Dir.LEFT);
            case GLFW.GLFW_KEY_RIGHT -> arrow(Dir.RIGHT);
            case GLFW.GLFW_KEY_UP -> arrow(Dir.UP);
            case GLFW.GLFW_KEY_DOWN -> arrow(Dir.DOWN);
            case GLFW.GLFW_KEY_TAB -> tab(hasShiftDown());
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> useActive();
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (HudKeys.WHEEL.matches(key, scan)) {
            feed(Input.G_UP, entryAt(active()) != null);
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    private void arrow(Dir dir) {
        Hit from = focus != null ? focus : hover != null ? hover : new Hit(Ring.INNER, open.mod());
        if (focus == null && hover == null && layout.ring(Ring.INNER).isEmpty()) {
            return;
        }
        Ring ring = from.ring();
        int next = layout.neighbour(ring, from.index(), dir);
        if (next == WheelLayout.TURN_NEXT || next == WheelLayout.TURN_PREVIOUS) {
            page(next == WheelLayout.TURN_NEXT ? 1 : -1);
            List<WheelEntry> now = pageEntries();
            focus = new Hit(Ring.OUTER, next == WheelLayout.TURN_NEXT ? 0 : Math.max(0, now.size() - 1));
        } else if (next >= 0) {
            Ring target = layout.ringAfter(ring, dir);
            focus = new Hit(target, next);
            if (target == Ring.INNER) {
                open = model.openMod(next);
            } else if (target == Ring.MIDDLE) {
                open = model.openGroup(open, next);
            }
        }
        hover = null;
        narrate();
    }

    private void tab(boolean back) {
        var mods = model.mods();
        int groups = mods.isEmpty() ? 0 : model.groups(mods.get(Math.max(0, Math.min(open.mod(), mods.size() - 1)))).size();
        tabFocus = WheelNarration.tabStep(tabFocus, mods.size(), groups, back);
        if (tabFocus < 0) {
            return;
        }
        if (tabFocus < mods.size()) {
            open = model.openMod(tabFocus);
            focus = new Hit(Ring.INNER, tabFocus);
        } else {
            open = model.openGroup(open, tabFocus - mods.size());
            focus = new Hit(Ring.MIDDLE, tabFocus - mods.size());
        }
        hover = null;
        narrate();
    }

    private void page(int delta) {
        int pages = Math.max(1, (model.powers(open).size() + WheelLayout.PAGE_SIZE - 1) / WheelLayout.PAGE_SIZE);
        open = new WheelModel.Open(open.mod(), open.group(), Math.max(0, Math.min(pages - 1, open.page() + delta)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx;
        int y = (int) my;
        if (pickerFor != null) {
            if (pickerHover >= 0 && button == 0) {
                WheelActions.assign(pickerFor, pickerHover);
            }
            pickerFor = null;
            return true;
        }
        int arrow = layout.arrowAt(x, y);
        if (arrow >= 0 && button == 0) {
            page(arrow == 0 ? -1 : 1);
            return true;
        }
        Hit h = layout.hit(x, y).orElse(null);
        WheelEntry e = entryAt(h);
        if (h != null && h.ring() != Ring.OUTER) {
            return true;
        }
        if (e == null) {
            if (button == 0) {
                feed(Input.CLICK_LEFT, false);
            }
            return true;
        }
        switch (ClickPlan.of(button, hasAltDown(), hasShiftDown())) {
            case USE, USE_SECOND_OPTION -> {
                hover = h;
                feedClickUse();
            }
            case SLOT_PICKER -> pickerFor = e;
            case MODE_NEXT -> WheelActions.changeMode(e, 1);
            case MODE_PREVIOUS -> WheelActions.changeMode(e, -1);
            default -> { }
        }
        return true;
    }

    private void feedClickUse() {
        feed(Input.CLICK_LEFT, true);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (heldByMouse != null && button == 0) {
            WheelActions.release();
            heldByMouse = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (pickerFor != null) {
            return true;
        }
        Hit h = layout.hit((int) mx, (int) my).orElse(null);
        WheelEntry e = entryAt(h);
        boolean outer = h == null ? isOnOuterBand((int) mx, (int) my) : h.ring() == Ring.OUTER;
        var target = new ClickPlan.ScrollTarget(outer, e != null, false, e != null && e.modeNames().size() > 1, layout.pageCount() > 1);
        switch (ClickPlan.scroll(sy, hasAltDown(), target)) {
            case PAGE_NEXT -> page(1);
            case PAGE_PREVIOUS -> page(-1);
            case MODE_NEXT -> WheelActions.changeMode(e, 1);
            case MODE_PREVIOUS -> WheelActions.changeMode(e, -1);
            default -> { }
        }
        return true;
    }

    private boolean isOnOuterBand(int x, int y) {
        double d = Math.hypot(x - layout.centreX(), y - layout.centreY());
        return d >= 88 && d <= 126;
    }

    // ---- narration ----

    private void narrate() {
        triggerImmediateNarration(false);
    }

    @Override
    protected void updateNarratedWidget(NarrationElementOutput out) {
        Hit h = active();
        var mods = model.mods();
        if (mods.isEmpty()) {
            return;
        }
        PowerSource source = mods.get(Math.max(0, Math.min(open.mod(), mods.size() - 1)));
        if (h != null && h.ring() == Ring.INNER && h.index() < mods.size()) {
            out.add(NarratedElementType.TITLE, Component.literal(WheelRenderer.modName(mods.get(h.index()))));
            return;
        }
        var groups = model.groups(source);
        if (h != null && h.ring() == Ring.MIDDLE && h.index() < groups.size()) {
            out.add(NarratedElementType.TITLE, Component.literal(WheelRenderer.text("group." + groups.get(h.index()).name().toLowerCase(Locale.ROOT))));
            return;
        }
        WheelEntry e = entryAt(h);
        if (e == null) {
            out.add(NarratedElementType.TITLE, Component.translatable("supernaturalbloodlines.wheel.hint.open"));
            return;
        }
        int pages = layout.pageCount();
        String state = e.state().on() ? WheelRenderer.text("state.on") : e.state().cooldownLeft(minecraft.level == null ? 0 : minecraft.level.getGameTime()) > 0
                ? WheelRenderer.text("state.cooling", (e.state().cooldownLeft(minecraft.level.getGameTime()) + 19) / 20) : WheelRenderer.text("state.ready");
        String lock = e.state().locked() ? WheelRenderer.text("state.locked", WheelRenderer.reason(e.state().lock())) : "";
        List<String> lines = WheelNarration.lines(WheelRenderer.modName(source), groups.isEmpty() ? "" : WheelRenderer.text("group." + e.group().name().toLowerCase(Locale.ROOT)),
                e.name().getString(), e.modeNames().size() > 1 ? e.modeName().getString() : "", state, lock, open.page() * WheelLayout.PAGE_SIZE + h.index() + 1,
                model.powers(open).size(), open.page() + 1, pages, (key, args) -> Component.translatable(key, args).getString());
        out.add(NarratedElementType.TITLE, Component.literal(String.join(", ", lines)));
    }

    /** For the development screenshots: the state of a frame (the pointer and the picker) without a real mouse. */
    void devHover(Ring ring, int index) {
        refresh();
        this.hover = new Hit(ring, index);
    }

    void devPicker(int outerIndex) {
        refresh();
        List<WheelEntry> page = pageEntries();
        if (outerIndex >= 0 && outerIndex < page.size()) {
            pickerFor = page.get(outerIndex);
        }
    }

    void devOpen(WheelModel.Open path) {
        refresh();
        this.open = path;
        this.placed = true;
        refresh();
    }
}
