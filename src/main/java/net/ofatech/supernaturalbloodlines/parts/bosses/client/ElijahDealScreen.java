package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealChoicePayload;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ChoiceLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ChoiceModel;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Narration;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Elijah's offer (R167): a choice dialog in the hub's own look (the player's theme, the kit's confirm style), not a hub page. It says what you pick, the
 * oath, for how many in-game days, and what breaking it costs; one button for each NPC Original he may give and "No deal". The list scrolls when it does
 * not fit. It sends the id of the one chosen, nothing else; the server decides whether it was on offer.
 */
public final class ElijahDealScreen extends Screen {
    private static final int FALLBACK_PAPER = 0xFF1B1B22;
    private static final int FALLBACK_ACCENT = 0xFFB02A3A;
    private static final int FALLBACK_INK = 0xFFE8E8EE;
    private static final int FALLBACK_FOCUS = 0xFFFFD84A;

    private final DealOfferPayload offer;
    @Nullable
    private final String themeId;
    private int paper = FALLBACK_PAPER;
    private int accent = FALLBACK_ACCENT;
    private int ink = FALLBACK_INK;
    private int focusColor = FALLBACK_FOCUS;
    @Nullable
    private HubTheme theme;
    private List<FormattedCharSequence> lines = List.of();
    private ChoiceLayout layout;
    private ChoiceModel model;

    private ElijahDealScreen(DealOfferPayload offer, @Nullable String themeId) {
        super(Component.translatable("supernaturalbloodlines.boss.elijah.title"));
        this.offer = offer;
        this.themeId = themeId;
    }

    public static void open(DealOfferPayload offer) {
        Minecraft.getInstance().setScreen(new ElijahDealScreen(offer, null));
    }

    /** A screen for the screenshots: no network, the same widgets, in the named hub theme ({@code themeId} null: the player's own). */
    public static Screen demo(List<String> originals, int days, @Nullable String themeId) {
        return new ElijahDealScreen(new DealOfferPayload(0, days, originals), themeId);
    }

    private Component pick() {
        return Component.translatable("supernaturalbloodlines.boss.elijah.offer.pick");
    }

    private Component oath() {
        return Component.translatable("supernaturalbloodlines.boss.elijah.offer.oath", Deals.days(offer.days()));
    }

    private Component penalty() {
        return Component.translatable("supernaturalbloodlines.boss.elijah.offer.penalty");
    }

    private Component label(int index) {
        return Component.translatable("supernaturalbloodlines.original." + offer.originals().get(index));
    }

    @Override
    protected void init() {
        theme = HubHooks.currentTheme(themeId).orElse(null);
        if (theme != null) {
            paper = theme.paper();
            accent = theme.accent();
            ink = theme.ink();
            focusColor = theme.focusColor();
        }
        int w = ConfirmLayout.textWidth(width);
        List<FormattedCharSequence> all = new ArrayList<>();
        for (Component paragraph : List.of(pick(), oath(), penalty())) {
            all.addAll(font.split(paragraph, w));
        }
        lines = all;
        layout = ChoiceLayout.of(width, height, lines.size(), offer.originals().size());
        model = new ChoiceModel(offer.originals().size(), layout.rows());
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(title).append(". ").append(pick()).append(" ").append(oath()).append(" ").append(penalty());
    }

    /** The title and the offer, then the button that has the focus: the same lines as the hub's own dialogs (see {@link Narration}). */
    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        int n = offer.originals().size();
        Component focused = model.focus() < n ? label(model.focus()) : Component.translatable("supernaturalbloodlines.boss.elijah.no_deal");
        String body = pick().getString() + " " + oath().getString() + " " + penalty().getString();
        for (String line : Narration.lines(title.getString(), null, title.getString(), body, focused.getString())) {
            output.add(NarratedElementType.TITLE, line);
        }
    }

    private static String plain(FormattedCharSequence line) {
        StringBuilder sb = new StringBuilder();
        line.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        Rect panel = layout.panel();
        KitDraw.dialogFrame(g, width, height, panel, paper, accent);
        g.drawString(font, LabelFit.cut(title.getString(), panel.w() - 2 * ConfirmLayout.PAD, font::width), panel.x() + ConfirmLayout.PAD, panel.y() + ConfirmLayout.PAD, accent, false);
        int shown = Math.min(lines.size(), layout.textLines());
        int y = layout.text().y();
        for (int i = 0; i < shown; i++) {
            boolean cutOff = i == shown - 1 && lines.size() > shown;
            if (cutOff) {
                g.drawString(font, LabelFit.cut(plain(lines.get(i)) + "...", layout.text().w(), font::width), layout.text().x(), y, ink, false);
            } else {
                g.drawString(font, lines.get(i), layout.text().x(), y, ink, false);
            }
            y += ConfirmLayout.LINE;
        }
        int n = offer.originals().size();
        boolean scrolls = model.scrolls();
        for (int row = 0; row < layout.rows(); row++) {
            int index = model.scroll() + row;
            Rect r = layout.row(row, scrolls);
            KitDraw.dialogButton(g, font, r, label(index), true, model.focus() == index, paper, accent, ink, focusColor);
        }
        if (scrolls && theme != null) {
            theme.drawScrollbar(g, layout.list(), model.scroll(), n - layout.rows());
        }
        KitDraw.dialogButton(g, font, layout.decline(), Component.translatable("supernaturalbloodlines.boss.elijah.no_deal"), false,
                model.focus() >= n, paper, accent, ink, focusColor);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        ChoiceModel.Key mapped = switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> ChoiceModel.Key.ESCAPE;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> ChoiceModel.Key.ENTER;
            case GLFW.GLFW_KEY_UP -> ChoiceModel.Key.UP;
            case GLFW.GLFW_KEY_DOWN -> ChoiceModel.Key.DOWN;
            case GLFW.GLFW_KEY_TAB -> ChoiceModel.Key.TAB;
            default -> null;
        };
        if (mapped == null) {
            return super.keyPressed(key, scan, modifiers);
        }
        ChoiceModel.Answer a = model.press(mapped);
        answer(a);
        if (a.type() == ChoiceModel.Type.NONE) {
            triggerImmediateNarration(false); // the focus moved: say which button has it now
        }
        return true;
    }

    /** The mouse moves the focus, so what is lit is always what Enter would do (and what the narrator last said). */
    @Override
    public void mouseMoved(double mx, double my) {
        boolean moved = false;
        if (layout.decline().contains(mx, my)) {
            moved = model.focusDecline();
        } else {
            for (int row = 0; row < layout.rows(); row++) {
                if (layout.row(row, model.scrolls()).contains(mx, my)) {
                    moved = model.focusRow(row);
                    break;
                }
            }
        }
        if (moved) {
            triggerImmediateNarration(false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        if (layout.decline().contains(mx, my)) {
            answer(model.click(-1));
            return true;
        }
        for (int row = 0; row < layout.rows(); row++) {
            if (layout.row(row, model.scrolls()).contains(mx, my)) {
                answer(model.click(row));
                return true;
            }
        }
        return layout.panel().contains(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dy != 0 && layout.list().contains(mx, my)) {
            model.wheel(dy < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private void answer(ChoiceModel.Answer a) {
        switch (a.type()) {
            case PICK -> send(offer.originals().get(a.index()));
            case DECLINE -> send(Deals.NO_DEAL);
            case CLOSE -> onClose();
            case NONE -> { }
        }
    }

    private void send(String original) {
        if (offer.entityId() != 0) {
            PacketDistributor.sendToServer(new DealChoicePayload(offer.entityId(), original));
        }
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // the dialog dims the world itself
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
