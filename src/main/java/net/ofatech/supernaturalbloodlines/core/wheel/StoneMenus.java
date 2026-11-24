package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * The menus of Infinity Stones' powers and the inputs that reach them from our wheel. His own wheel opens them with the buttons of its screen; ours keeps left = use and
 * right = slot picker, so his extras sit on inputs ours does not use: middle click (or Ctrl + Enter) is his middle click, Shift-click (or Shift + Enter) is his right click.
 * The rules follow his {@code WheelScreen.mouseClicked} and {@code WheelActions.activate}. Pure: the reading of his classes is {@code parts/hud/client/wheel/infinity}.
 */
public final class StoneMenus {
    /** The menu a power has: his destination screen (the target one moves what you aim at), Will Construct's two screens, or a server-sent choice menu. */
    public enum Kind { NONE, DESTINATION, DESTINATION_TARGET, WILL, CHOICE }

    /** What the player did on a stone power: use (left), the second option (Shift-click / right click of his wheel), his middle click. */
    public enum Input { USE, SECOND, MENU }

    public enum Act {
        /** The usual use of the power (aimed first when it needs aim). */
        PLAIN,
        OPEN_DESTINATION,
        OPEN_CONSTRUCT,
        OPEN_QUICK_SLOTS,
        /** Asks his server for the choice menu; it answers by opening it. */
        OPEN_CHOICE,
        /** The second option sent at once: a menu power's right click skips the aiming. */
        SECOND_AT_ONCE,
        NOTHING
    }

    /** A side-panel help row: lang suffixes under {@code supernaturalbloodlines.wheel.help.} for the key and what it does, and the narration line ("" when the row has none). */
    public record Help(String key, String what, String narrate) {
    }

    public static final String WILL_MODE = "reality.will_construct";

    private StoneMenus() {
    }

    /** His order: Will Construct first, then a destination power, then the choice menu. */
    public static Kind kind(boolean isWill, boolean hasDestination, boolean movesTarget, boolean hasChoiceMenu) {
        if (isWill) {
            return Kind.WILL;
        }
        if (hasDestination) {
            return movesTarget ? Kind.DESTINATION_TARGET : Kind.DESTINATION;
        }
        return hasChoiceMenu ? Kind.CHOICE : Kind.NONE;
    }

    public static boolean isDestination(Kind kind) {
        return kind == Kind.DESTINATION || kind == Kind.DESTINATION_TARGET;
    }

    /** What an input does on a power of this kind. {@code menuOnLeft}: the power's plain use is the destination menu itself (his Teleport GUI). */
    public static Act route(Kind kind, boolean menuOnLeft, Input input) {
        return switch (input) {
            case USE -> isDestination(kind) && menuOnLeft ? Act.OPEN_DESTINATION : Act.PLAIN;
            case SECOND -> kind == Kind.WILL ? Act.OPEN_CONSTRUCT : kind == Kind.CHOICE ? Act.SECOND_AT_ONCE : Act.PLAIN;
            case MENU -> switch (kind) {
                case DESTINATION, DESTINATION_TARGET -> Act.OPEN_DESTINATION;
                case WILL -> Act.OPEN_QUICK_SLOTS;
                case CHOICE -> Act.OPEN_CHOICE;
                case NONE -> Act.NOTHING;
            };
        };
    }

    /** The lang suffix of what the plain click row says: a power whose use is its menu says so. */
    public static String clickWhat(Kind kind, boolean menuOnLeft) {
        return isDestination(kind) && menuOnLeft ? "click_destination" : "click_use";
    }

    /**
     * The extra help rows of a power, in order: the second option (Shift-click) when it has one, his middle click when it has a menu, and the keys of both when there is a
     * menu. Empty for a power with neither (the closing row stays).
     */
    public static List<Help> help(Kind kind, boolean dual) {
        List<Help> out = new ArrayList<>();
        boolean second = kind == Kind.WILL || kind == Kind.CHOICE || dual;
        boolean menu = kind != Kind.NONE;
        if (second) {
            String what = switch (kind) {
                case WILL -> "second_construct";
                case CHOICE -> "second_at_once";
                default -> "dual_what";
            };
            out.add(new Help("dual_key", what, kind == Kind.WILL ? "narrate_second_construct" : kind == Kind.CHOICE ? "narrate_second_at_once" : "dual"));
        }
        if (menu) {
            String what = switch (kind) {
                case WILL -> "menu_quick";
                case CHOICE -> "menu_choice";
                case DESTINATION_TARGET -> "menu_destination_target";
                default -> "menu_destination";
            };
            out.add(new Help("menu_key", what, "narrate_" + what));
            out.add(new Help("keys_key", second ? "keys_both" : "keys_menu", ""));
        }
        return out;
    }
}
