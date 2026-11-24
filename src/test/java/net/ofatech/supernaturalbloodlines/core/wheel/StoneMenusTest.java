package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Act;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Help;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Input;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Kind;
import org.junit.jupiter.api.Test;

/** What each input does on a stone power with a menu: his own rules (his WheelScreen.mouseClicked and WheelActions.activate) on our inputs. */
class StoneMenusTest {
    @Test
    void willConstructIsHisThreeWays() {
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.WILL, false, Input.USE), "left: grip mode on or off, the plain use");
        assertEquals(Act.OPEN_CONSTRUCT, StoneMenus.route(Kind.WILL, false, Input.SECOND), "his right click");
        assertEquals(Act.OPEN_QUICK_SLOTS, StoneMenus.route(Kind.WILL, false, Input.MENU), "his middle click");
    }

    @Test
    void aDestinationPowerOpensHisDestinationScreenOnTheMiddleClick() {
        for (Kind k : new Kind[] {Kind.DESTINATION, Kind.DESTINATION_TARGET}) {
            assertEquals(Act.OPEN_DESTINATION, StoneMenus.route(k, false, Input.MENU), k.name());
            assertEquals(Act.PLAIN, StoneMenus.route(k, false, Input.USE), k + ": the plain click still uses it");
            assertEquals(Act.PLAIN, StoneMenus.route(k, false, Input.SECOND), k + ": the second option is the power's own");
        }
    }

    @Test
    void theTeleportGuiIsItsMenuOnTheLeftClickToo() {
        assertEquals(Act.OPEN_DESTINATION, StoneMenus.route(Kind.DESTINATION, true, Input.USE));
        assertEquals(Act.OPEN_DESTINATION, StoneMenus.route(Kind.DESTINATION, true, Input.MENU));
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.WILL, true, Input.USE), "menuOnLeft only means something for a destination");
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.CHOICE, true, Input.USE));
    }

    @Test
    void aChoiceMenuPowerOpensItOnTheMiddleClickAndItsRightClickSkipsTheAim() {
        assertEquals(Act.OPEN_CHOICE, StoneMenus.route(Kind.CHOICE, false, Input.MENU));
        assertEquals(Act.SECOND_AT_ONCE, StoneMenus.route(Kind.CHOICE, false, Input.SECOND));
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.CHOICE, false, Input.USE));
    }

    @Test
    void aPowerWithoutAMenuHasNoMiddleClickAndItsOtherInputsAreUnchanged() {
        assertEquals(Act.NOTHING, StoneMenus.route(Kind.NONE, false, Input.MENU));
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.NONE, false, Input.USE));
        assertEquals(Act.PLAIN, StoneMenus.route(Kind.NONE, false, Input.SECOND), "a dual power's second option is the usual phase 2");
    }

    @Test
    void theKindFollowsHisOrderWillThenDestinationThenChoice() {
        assertEquals(Kind.WILL, StoneMenus.kind(true, true, true, true));
        assertEquals(Kind.DESTINATION_TARGET, StoneMenus.kind(false, true, true, true));
        assertEquals(Kind.DESTINATION, StoneMenus.kind(false, true, false, true));
        assertEquals(Kind.CHOICE, StoneMenus.kind(false, false, true, true), "movesTarget without a destination means nothing");
        assertEquals(Kind.NONE, StoneMenus.kind(false, false, false, false));
        assertEquals("reality.will_construct", StoneMenus.WILL_MODE);
    }

    @Test
    void theClickRowSaysDestinationOnlyWhereTheClickIsTheMenu() {
        assertEquals("click_destination", StoneMenus.clickWhat(Kind.DESTINATION, true));
        assertEquals("click_use", StoneMenus.clickWhat(Kind.DESTINATION, false));
        assertEquals("click_use", StoneMenus.clickWhat(Kind.DESTINATION_TARGET, false));
        assertEquals("click_use", StoneMenus.clickWhat(Kind.WILL, true));
    }

    private static List<String> keys(List<Help> rows) {
        return rows.stream().map(h -> h.key() + "/" + h.what()).toList();
    }

    @Test
    void everyExtraActionHasAHelpRowAndAKeyboardRow() {
        assertEquals(List.of(), StoneMenus.help(Kind.NONE, false), "a plain power keeps the closing row");
        assertEquals(List.of("dual_key/dual_what"), keys(StoneMenus.help(Kind.NONE, true)), "the second option alone is the row it always was");
        assertEquals(List.of("menu_key/menu_destination", "keys_key/keys_menu"), keys(StoneMenus.help(Kind.DESTINATION, false)));
        assertEquals(List.of("dual_key/dual_what", "menu_key/menu_destination_target", "keys_key/keys_both"), keys(StoneMenus.help(Kind.DESTINATION_TARGET, true)));
        assertEquals(List.of("dual_key/second_construct", "menu_key/menu_quick", "keys_key/keys_both"), keys(StoneMenus.help(Kind.WILL, true)));
        assertEquals(List.of("dual_key/second_at_once", "menu_key/menu_choice", "keys_key/keys_both"), keys(StoneMenus.help(Kind.CHOICE, true)));
    }

    @Test
    void theNarratorGetsOneLinePerInputThatDoesSomethingExtra() {
        List<String> narrate = StoneMenus.help(Kind.WILL, true).stream().map(Help::narrate).filter(s -> !s.isEmpty()).toList();
        assertEquals(List.of("narrate_second_construct", "narrate_menu_quick"), narrate, "the keys row repeats what the other two say, so it has no line");
        assertTrue(StoneMenus.help(Kind.DESTINATION, false).stream().map(Help::narrate).anyMatch("narrate_menu_destination"::equals));
        assertEquals(List.of("dual"), StoneMenus.help(Kind.NONE, true).stream().map(Help::narrate).toList(), "a dual power keeps its old narration line");
    }
}
