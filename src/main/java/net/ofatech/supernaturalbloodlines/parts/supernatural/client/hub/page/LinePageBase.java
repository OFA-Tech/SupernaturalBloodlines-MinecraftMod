package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import org.lwjgl.glfw.GLFW;

/**
 * The selection and the arrow keys of the Bloodline page, the same in every look: the page remembers which node is selected (the player's own
 * at first), a click or an arrow selects another, and the arrows follow {@link LineTree#move} while the keyboard focus is on the tree.
 */
public abstract class LinePageBase implements HubPageView {
    protected String selected = LineTree.YOU;
    private LineTree.Model model;
    private HubScreen screen;

    /** Call first in {@code render}: builds the model of this frame and keeps the selection valid. */
    protected final LineTree.Model prepare(HubScreen.Frame f) {
        screen = f.screen();
        model = LinePageSupport.model(f);
        if (model.node(selected).isEmpty()) {
            selected = LineTree.YOU;
        }
        return model;
    }

    protected final LineTree.Node selectedNode(LineTree.Model m) {
        return m.node(selected).orElseGet(() -> m.node(LineTree.YOU).orElseThrow());
    }

    /** Registers a node: hover tells what the narrator says, a click selects it, the focus ring follows the keyboard. */
    protected final void nodeTarget(HubScreen.Frame f, LineTree.Model m, LineTree.Node n, Rect hit) {
        f.target(n.id(), hit, List.of(LinePageSupport.narration(m, n)), () -> selected = n.id());
        if (f.focused(n.id())) {
            KitDraw.focusRing(f.g, hit, f.theme().focusColor());
        }
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (model == null || screen == null || (modifiers & (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT)) != 0) {
            return false;
        }
        String focus = screen.focusId();
        if (focus == null || !focus.startsWith("line:")) {
            return false;
        }
        LineTree.Key k = switch (key) {
            case GLFW.GLFW_KEY_UP -> LineTree.Key.UP;
            case GLFW.GLFW_KEY_DOWN -> LineTree.Key.DOWN;
            case GLFW.GLFW_KEY_LEFT -> LineTree.Key.LEFT;
            case GLFW.GLFW_KEY_RIGHT -> LineTree.Key.RIGHT;
            default -> null;
        };
        if (k == null) {
            return false;
        }
        Optional<String> to = LineTree.move(model, focus, k);
        to.ifPresent(id -> {
            selected = id;
            screen.focusOn(id);
        });
        return to.isPresent();
    }

    /** {@code text} cut to {@code width} with an ellipsis. */
    protected static String fit(HubScreen.Frame f, Component text, int width) {
        return net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(text.getString(), Math.max(8, width), f.font()::width);
    }
}
