package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Kind;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.junit.jupiter.api.Test;

/**
 * The help rows and narration lines of stone powers, as the screen builds them (from the entry's power, against a fake of his mod) and as the lang file says them: every
 * line exists, none runs out of the 126 px panel, and the two screens that need the mouse say so.
 */
class StoneHelpTextTest {
    private static final String P = "supernaturalbloodlines.wheel.";
    private static final int PANEL = 126;
    /** A fixed measure: 5 px a letter, 6 bold. A row that fits this fits the real font, whose narrow letters (i, l, t) are narrower and capitals about as wide. */
    private static final int LETTER = 5;
    private static final int BOLD = 6;

    private static JsonObject lang() throws Exception {
        try (var in = StoneHelpTextTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            assertNotNull(in);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String say(JsonObject lang, String suffix, String ctrl) {
        assertTrue(lang.has(P + suffix), "no lang key " + P + suffix);
        return lang.get(P + suffix).getAsString().replace("%s", ctrl);
    }

    private static List<StoneView> views() {
        List<StoneView> out = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (boolean onLeft : new boolean[] {false, true}) {
                for (boolean dual : new boolean[] {false, true}) {
                    FakeStoneApi api = new FakeStoneApi().menu(2, "p", false, dual, kind, onLeft);
                    WheelEntry e = StoneEntries.build(api, Map.of(), 1_000).get(0);
                    out.add(StoneActions.view(api, e));
                }
            }
        }
        return out;
    }

    @Test
    void everyKeyAndNarrationLineOfEveryPowerIsInTheLangFile() throws Exception {
        JsonObject lang = lang();
        for (StoneView v : views()) {
            say(lang, "help." + v.clickWhat(), "Ctrl");
            for (StoneMenus.Help h : v.help()) {
                say(lang, "help." + h.key(), "Ctrl");
                say(lang, "help." + h.what(), "Ctrl");
            }
            for (String line : StoneMenus.narration(v.clickWhat(), v.help())) {
                say(lang, "help." + line, "Control");
            }
        }
    }

    @Test
    void noHelpRowRunsOutOfThePanelWithCtrlOrCmd() throws Exception {
        JsonObject lang = lang();
        List<String[]> base = List.of(new String[] {"help.click", "help.click_use"}, new String[] {"help.click", "help.click_destination"}, new String[] {"help.right", "help.right_slot"},
                new String[] {"help.alt", "help.alt_mode"}, new String[] {"help.scroll", "help.scroll_page"}, new String[] {"help.close_keys", "help.close"});
        List<String[]> rows = new ArrayList<>(base);
        for (StoneView v : views()) {
            for (StoneMenus.Help h : v.help()) {
                rows.add(new String[] {"help." + h.key(), "help." + h.what()});
            }
        }
        for (String ctrl : new String[] {"Ctrl", "Cmd"}) {
            for (String[] r : rows) {
                String key = say(lang, r[0], ctrl);
                String what = say(lang, r[1], ctrl);
                int width = key.length() * BOLD + 5 + what.length() * LETTER;
                assertTrue(width <= PANEL, key + " / " + what + " is " + width + " px, the panel holds " + PANEL);
            }
        }
    }

    @Test
    void theTwoScreensThatNeedTheMouseSaySo() throws Exception {
        JsonObject lang = lang();
        assertTrue(say(lang, "help.narrate_second_construct", "Control").contains("needs the mouse"));
        assertTrue(say(lang, "help.narrate_menu_quick", "Control").contains("need the mouse"));
        assertTrue(!say(lang, "help.narrate_menu_destination", "Control").contains("mouse"), "his destination screen works from the keyboard");
    }

    @Test
    void theNarrationNamesTheMacKeyAsCommand() throws Exception {
        JsonObject lang = lang();
        assertEquals("Middle-click or Command Enter: choose a destination", say(lang, "help.narrate_menu_destination", StoneMenus.ctrl(true, true)));
        assertEquals("Middle-click or Control Enter: choose a destination", say(lang, "help.narrate_menu_destination", StoneMenus.ctrl(false, true)));
    }

    @Test
    void thePrayAndSoulEntriesHaveANameAndADescription() throws Exception {
        JsonObject lang = lang();
        for (String key : new String[] {"infinity.pray", "infinity.pray.desc", "infinity.soul_control", "infinity.soul_control.desc", "notice.no_menu", "notice.unavailable"}) {
            say(lang, key, "");
        }
    }
}
