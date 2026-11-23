package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.ofatech.supernaturalbloodlines.core.wheel.HoldPlan;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelCursor;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Cell;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Hit;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Layout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelModel;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelNarration;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPalette;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPanelText;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelRules;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;
import net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity.InfinityCompat;
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
 * <p>
 * What a frame works out is worked out once: the layout and the page only when their inputs change, the text through {@link WheelPanelText} and {@link WheelStrings}, the
 * slots once per tick. The pointer decides which cell is hot only when it moves; a key that moves the focus takes over until it moves again.
 */
public final class WheelScreen extends Screen {
    private GestureMachine machine;
    private final HudKit kit;
    private final Look look;
    private final WheelPalette pal;
    private List<WheelEntry> list = List.of();
    private Map<ResourceLocation, WheelEntry> byId = Map.of();
    private WheelModel<WheelEntry> model = new WheelModel<>(List.of(), s -> true);
    private WheelModel.Open open = WheelModel.Open.FIRST;
    private Layout layout;
    private List<WheelEntry> page = List.of();
    private int pageTotal;
    private LayoutKey layoutKey;
    private final WheelCursor cursor = new WheelCursor();
    private int mouseX;
    private int mouseY;
    private boolean seeded;
    private WheelEntry heldByMouse;
    /** An Enter or Space press that started a held power: its release ends it. */
    private int heldByKey = -1;
    private boolean placed;
    /** The slot picker: the power it is for (found again by its reference every frame, as its entry is replaced at every catalogue revision) and the slot under the pointer. */
    private PowerRef pickerRef;
    private int pickerHover = -1;
    private List<SlotPicker.Slot> pickerSlots;
    private List<WheelEntry> pickerHolds;
    private int tabFocus = -1;

    private record LayoutKey(int w, int h, int scale, Look look, int mods, int openMod, int groups, int entries, int page, int epoch) {
    }

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
        WheelPanelText.clear();
        WheelStrings.clear();
        layoutKey = null;
        if (net.neoforged.fml.ModList.get().isLoaded("tensura")) {
            TensuraHudHold.open();
        }
        WheelActions.view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer.WHEEL, true);
        refresh();
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
        heldByKey = -1;
        WheelPanelText.clear();
        WheelStrings.clear();
        WheelInput.screenClosed(machine);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // the game stays visible behind the wheel: no blur, no dimming
    }

    // ---- state ----

    /** Brings the model, the page and the layout up to date; does nothing when none of what they are made from has changed. */
    private void refresh() {
        List<WheelEntry> now = WheelList.current();
        if (now != list) {
            list = now;
            Map<ResourceLocation, WheelEntry> ids = new HashMap<>();
            for (WheelEntry e : list) {
                ids.putIfAbsent(e.ref().id(), e);
            }
            byId = ids;
            model = new WheelModel<>(list, WheelList::installed);
            if (!placed) {
                placed = true;
                open = locate();
            }
            open = model.fit(open);
            layoutKey = null;
        }
        var mods = model.mods();
        int modIndex = mods.isEmpty() ? 0 : Math.max(0, Math.min(open.mod(), mods.size() - 1));
        List<PowerGroup> groups = mods.isEmpty() ? List.of() : model.groups(mods.get(modIndex));
        List<WheelEntry> all = model.powers(open);
        LayoutKey key = new LayoutKey(width, height, (int) minecraft.getWindow().getGuiScale(), look, mods.size(), open.mod(), groups.size(), all.size(), open.page(), WheelStrings.epoch());
        if (key.equals(layoutKey)) {
            return;
        }
        layoutKey = key;
        boolean rehit = layout != null;
        pageTotal = all.size();
        int from = Math.min(all.size(), open.page() * WheelLayout.PAGE_SIZE);
        page = all.subList(from, Math.min(all.size(), from + WheelLayout.PAGE_SIZE));
        layout = WheelLayout.compute(width, height, key.scale(), look, mods.size(), open.mod(), groups.size(), all.size(), open.page());
        if (groups.size() >= 2) {
            int[] widths = new int[groups.size()];
            for (int i = 0; i < widths.length; i++) {
                widths[i] = WheelPanelText.width(font, WheelRenderer.text("group." + groups.get(i).name().toLowerCase(Locale.ROOT)), font::width) + 10;
            }
            layout = layout.withLabelWidths(widths);
        }
        if (rehit && pickerRef == null && !DevWheel.active) {
            cursor.rehit(layout.hit(mouseX, mouseY).orElse(null)); // the list changed under a resting pointer
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
        int pageNo = 0;
        for (WheelEntry e : list) {
            if (e.ref().equals(ref)) {
                group = Math.max(0, groups.indexOf(e.group()));
                List<WheelEntry> inGroup = model.powers(ref.source(), groups.isEmpty() ? PowerGroup.MAIN : groups.get(group));
                pageNo = Math.max(0, inGroup.indexOf(e)) / WheelLayout.PAGE_SIZE;
            }
        }
        return new WheelModel.Open(mod, group, pageNo);
    }

    private WheelEntry entryAt(Hit h) {
        if (h == null || h.ring() != Ring.OUTER) {
            return null;
        }
        return h.index() >= 0 && h.index() < page.size() ? page.get(h.index()) : null;
    }

    private Hit active() {
        return cursor.active().orElse(null);
    }

    private static int indexOfRef(List<WheelEntry> entries, PowerRef ref) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).ref().equals(ref)) {
                return i;
            }
        }
        return -1;
    }

    private WheelEntry pickerEntry() {
        if (pickerRef == null) {
            return null;
        }
        int i = indexOfRef(page, pickerRef);
        return i < 0 ? null : page.get(i);
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refresh();
        if (!seeded) {
            seeded = true;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            if (!DevWheel.active) {
                mouseMoved(mouseX, mouseY);
            }
        }
        WheelEntry picked = pickerEntry();
        if (pickerRef != null && picked == null) {
            closePicker(); // the power left the page (the list changed under the picker)
        }
        Hit shown = picked != null ? new Hit(Ring.OUTER, indexOfRef(page, pickerRef)) : active();
        WheelEntry power = entryAt(shown);
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        WheelRenderer.Picker picker = picked != null ? picker(picked, mouseX, mouseY) : null;
        WheelRenderer.draw(g, font, new WheelRenderer.Frame(layout, model, open, page, shown, power, hasAltDown() || DevWheel.alt, pal, kit, now, picker, WheelActions::slotOf));
    }

    private void closePicker() {
        pickerRef = null;
        pickerHover = -1;
        pickerSlots = null;
        pickerHolds = null;
    }

    private WheelRenderer.Picker picker(WheelEntry entry, int mx, int my) {
        WheelRules.SlotTarget target = WheelActions.target(entry);
        Cell anchor = layout.pickerAnchor(indexOfRef(page, entry.ref()));
        if (target == WheelRules.SlotTarget.NONE) {
            return new WheelRenderer.Picker(entry, anchor, List.of(), List.of(), -1, true);
        }
        if (pickerSlots == null) {
            List<ResourceLocation> holdIds = WheelActions.holds(target);
            List<String> ids = new ArrayList<>();
            List<WheelEntry> holds = new ArrayList<>();
            for (ResourceLocation id : holdIds) {
                ids.add(id == null ? "" : id.toString());
                holds.add(id == null ? null : find(id));
            }
            pickerSlots = SlotPicker.slotsFor(target, ids, WheelActions.activePreset(), WheelActions.presetCount());
            pickerHolds = holds;
        }
        pickerHover = -1;
        for (int i = 0; i < pickerSlots.size(); i++) {
            int sx = anchor.x() + 8 + i * 31;
            int sy = anchor.y() + 17;
            if (mx >= sx && mx < sx + 28 && my >= sy && my < sy + 28) {
                pickerHover = i;
            }
        }
        return new WheelRenderer.Picker(entry, anchor, pickerSlots, pickerHolds, pickerHover, false);
    }

    /** What a slot holds, as the wheel lists it; a made-up entry (its id as the name) for a skill that is no wheel power. */
    private WheelEntry find(ResourceLocation id) {
        WheelEntry e = byId.get(id);
        if (e != null) {
            return e;
        }
        return new WheelEntry(new PowerRef(PowerSource.TENSURA, id), PowerGroup.SKILLS, PowerKind.INSTANT, Component.literal(id.getPath().replace('_', ' ')), Component.empty(),
                List.of(), ClientIcons.UNKNOWN, PowerState.READY, true, -1f);
    }

    // ---- what a power does ----

    private int modeOf(WheelEntry e) {
        return e.state().mode();
    }

    /** Uses the power under the pointer (or focus); true when something was started. */
    private boolean useActive(HoldPlan.Trigger trigger) {
        WheelEntry e = entryAt(active());
        if (e == null) {
            return false;
        }
        if (net.ofatech.supernaturalbloodlines.core.wheel.WheelNarration.enter(e.state()).isPresent()) {
            minecraft.getNarrator().sayNow(Component.literal(WheelRenderer.reason(e.state().lock())));
            return false;
        }
        boolean started = WheelActions.use(e, modeOf(e), secondOption);
        if (started && HoldPlan.releaseAtOnce(e.kind(), trigger)) {
            WheelActions.release(); // nothing to hold it with: a G that was let go of, a tap
        } else if (started && HoldPlan.heldUntilInputUp(e.kind(), trigger)) {
            if (trigger == HoldPlan.Trigger.MOUSE_CLICK) {
                heldByMouse = e;
            } else {
                heldByKey = lastKey;
            }
        }
        if (started && (e.kind() == PowerKind.INSTANT || e.kind() == PowerKind.AIMED)) {
            onClose();
        } else if (started) {
            machine = machine.sticky();
        }
        return started;
    }

    private int lastKey = -1;
    /** Shift + left click: the second option of a power that has one. Set only while that one click is being handled. */
    private boolean secondOption;

    private void feed(Input in, boolean overPower) {
        GestureMachine.Step s = machine.step(in, Util.getMillis(), HudClientConfig.wheelTapMs(), overPower);
        machine = s.machine();
        switch (s.effect()) {
            case CLOSE -> onClose();
            case USE_HOVERED -> useActive(in == Input.G_UP ? HoldPlan.Trigger.G_RELEASE : HoldPlan.Trigger.MOUSE_CLICK);
            default -> { }
        }
    }

    // ---- input ----

    @Override
    public void tick() {
        WheelActions.nextTick();
        pickerSlots = null;
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
            if (pickerRef != null) {
                closePicker();
                return true;
            }
            feed(Input.ESC, false);
            onClose();
            return true;
        }
        if (pickerRef != null) {
            WheelEntry picked = pickerEntry();
            for (int i = 0; i < HudKeys.SLOTS.length; i++) {
                if (HudKeys.SLOTS[i].matches(key, scan)) {
                    if (picked != null) {
                        WheelActions.assign(picked, i);
                    }
                    closePicker();
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
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (heldByKey == key) {
                    return true; // key repeat of the press that is holding a power
                }
                lastKey = key;
                useActive(HoldPlan.Trigger.KEY_PRESS);
            }
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (HudKeys.WHEEL.matches(key, scan)) {
            gUp();
            return true;
        }
        if (heldByKey == key) {
            heldByKey = -1;
            WheelActions.release(); // the Enter or Space that started a held power is let go
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    private void gUp() {
        feed(Input.G_UP, entryAt(active()) != null);
    }

    /** A key moved the focus: the pointer does not decide again until it moves. */
    private void focusTo(Hit to) {
        cursor.keyFocus(to);
        narrate();
    }

    private void arrow(Dir dir) {
        Hit from = cursor.focus().orElse(cursor.hover().orElse(new Hit(Ring.INNER, open.mod())));
        if (cursor.focus().isEmpty() && cursor.hover().isEmpty() && layout.ring(Ring.INNER).isEmpty()) {
            return;
        }
        Ring ring = from.ring();
        int next = layout.neighbour(ring, from.index(), dir);
        if (next == WheelLayout.TURN_NEXT || next == WheelLayout.TURN_PREVIOUS) {
            page(next == WheelLayout.TURN_NEXT ? 1 : -1);
            focusTo(new Hit(Ring.OUTER, next == WheelLayout.TURN_NEXT ? 0 : Math.max(0, page.size() - 1)));
        } else if (next >= 0) {
            Ring target = layout.ringAfter(ring, dir);
            if (target == Ring.INNER) {
                open = model.openMod(next);
                refresh();
            } else if (target == Ring.MIDDLE) {
                open = model.openGroup(open, next);
                refresh();
            }
            focusTo(new Hit(target, next));
        } else {
            cursor.keyFocus(from);
        }
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
            refresh();
            focusTo(new Hit(Ring.INNER, tabFocus));
        } else {
            open = model.openGroup(open, tabFocus - mods.size());
            refresh();
            focusTo(new Hit(Ring.MIDDLE, tabFocus - mods.size()));
        }
    }

    private void page(int delta) {
        int pages = Math.max(1, (pageTotal + WheelLayout.PAGE_SIZE - 1) / WheelLayout.PAGE_SIZE);
        open = new WheelModel.Open(open.mod(), open.group(), Math.max(0, Math.min(pages - 1, open.page() + delta)));
        refresh();
        cursor.clearHover(); // the cell under the pointer is another power now: the next move says which
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseMoved((int) mx, (int) my);
    }

    private void mouseMoved(int x, int y) {
        mouseX = x;
        mouseY = y;
        if (pickerRef != null || layout == null) {
            return;
        }
        Hit h = layout.hit(x, y).orElse(null);
        cursor.mouseMoved(h);
        if (h != null && h.ring() == Ring.INNER && h.index() != open.mod()) {
            open = model.openMod(h.index());
            refresh();
        } else if (h != null && h.ring() == Ring.MIDDLE && h.index() != open.group()) {
            open = model.openGroup(open, h.index());
            refresh();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx;
        int y = (int) my;
        if (button > 1 && HudKeys.WHEEL.matchesMouse(button)) {
            feed(Input.G_DOWN, false);
            return true;
        }
        if (pickerRef != null) {
            WheelEntry picked = pickerEntry();
            if (pickerHover >= 0 && button == 0 && picked != null) {
                WheelActions.assign(picked, pickerHover);
            }
            closePicker();
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
                cursor.mouseMoved(h);
                secondOption = hasShiftDown();
                feed(Input.CLICK_LEFT, true);
                secondOption = false;
            }
            case SLOT_PICKER -> {
                pickerRef = e.ref();
                pickerSlots = null;
            }
            case MODE_NEXT -> WheelActions.changeMode(e, 1);
            case MODE_PREVIOUS -> WheelActions.changeMode(e, -1);
            default -> { }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button > 1 && HudKeys.WHEEL.matchesMouse(button)) {
            gUp();
            return true;
        }
        if (heldByMouse != null && button == 0) {
            WheelActions.release();
            heldByMouse = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (pickerRef != null) {
            return true;
        }
        Hit h = layout.hit((int) mx, (int) my).orElse(null);
        WheelEntry e = entryAt(h);
        boolean outer = h == null ? isOnOuterBand((int) mx, (int) my) : h.ring() == Ring.OUTER;
        var target = new ClickPlan.ScrollTarget(outer, e != null, e != null && InfinityCompat.owns(e) && InfinityCompat.hasLevels(e), e != null && e.modeNames().size() > 1,
                layout.pageCount() > 1);
        switch (ClickPlan.scroll(sy, hasAltDown(), target)) {
            case LEVEL_UP -> WheelActions.changeLevel(e, 1);
            case LEVEL_DOWN -> WheelActions.changeLevel(e, -1);
            case PAGE_NEXT -> {
                page(1);
                mouseMoved((int) mx, (int) my);
            }
            case PAGE_PREVIOUS -> {
                page(-1);
                mouseMoved((int) mx, (int) my);
            }
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
        if (layout == null) {
            refresh();
        }
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
        if (e == null || h == null) {
            out.add(NarratedElementType.TITLE, Component.translatable("supernaturalbloodlines.wheel.hint.open"));
            return;
        }
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        String state = WheelNarration.stateWord(e.state().on(), e.state().cooldownLeft(now), (key, args) -> Component.translatable(key, args).getString());
        String lock = e.state().locked() ? WheelRenderer.text("state.locked", WheelRenderer.reason(e.state().lock())) : "";
        List<String> lines = WheelNarration.lines(WheelRenderer.modName(source), groups.isEmpty() ? "" : WheelRenderer.text("group." + e.group().name().toLowerCase(Locale.ROOT)),
                e.name().getString(), e.coverNote().getString(), WheelDescriptions.of(e), e.modeNames().size() > 1 ? e.modeName().getString() : "", state, lock, open.page() * WheelLayout.PAGE_SIZE + h.index() + 1, pageTotal,
                open.page() + 1, layout.pageCount(), (key, args) -> Component.translatable(key, args).getString());
        out.add(NarratedElementType.TITLE, Component.literal(String.join(", ", lines)));
    }

    /** For the development screenshots: the state of a frame (the pointer and the picker) without a real mouse. */
    void devHover(Ring ring, int index) {
        refresh();
        cursor.mouseMoved(new Hit(ring, index));
    }

    void devPicker(int outerIndex) {
        refresh();
        if (outerIndex >= 0 && outerIndex < page.size()) {
            pickerRef = page.get(outerIndex).ref();
        }
    }

    void devOpen(WheelModel.Open path) {
        refresh();
        this.open = path;
        this.placed = true;
        this.layoutKey = null;
        refresh();
    }
}
