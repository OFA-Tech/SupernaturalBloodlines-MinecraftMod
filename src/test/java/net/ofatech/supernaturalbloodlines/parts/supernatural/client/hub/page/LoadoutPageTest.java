package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.core.hud.LoadoutTabModel;
import org.junit.jupiter.api.Test;

/** The hub's Loadout tab (plan 23 Task 5): a grey row says why on the page, and the list before the wheel's catalogue never offers Bite to a non-biter. */
class LoadoutPageTest {
    private static final String TAB = "src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/LoadoutTab.java";
    private static final String CLIENT = "src/main/java/net/ofatech/supernaturalbloodlines/parts/hud/client/ClientLoadout.java";
    /** The vanilla font's line height. */
    private static final int FONT = 9;

    @Test
    void aGreyRowHasAReasonLine() {
        String src = SourceTree.read(TAB);
        assertTrue(src.contains("Component.translatable(LoadoutTabModel.CANT_USE, Component.translatable(e.greyedReason()))"), "the grey row draws no reason line");
        assertTrue(src.contains("LoadoutTabModel.rowHeight(grey)"), "the row is not made tall enough for its reason");
        String lang = SourceTree.read("src/main/resources/assets/supernaturalbloodlines/lang/en_us.json");
        assertTrue(JsonParser.parseString(lang).getAsJsonObject().get(LoadoutTabModel.CANT_USE).getAsString().contains("%s"), "the reason line has no place for the reason");
    }

    @Test
    void aNormalRowHasNone() {
        assertEquals(LoadoutTabModel.ROW, LoadoutTabModel.rowHeight(false));
    }

    /** The name sits 4 px down, the reason one line below it: both fit the grey row, at any gui scale (the GUI is laid out in GUI pixels). */
    @Test
    void theReasonLineFitsTheRow() {
        int reasonBottom = 4 + LoadoutTabModel.REASON_LINE + FONT;
        assertTrue(reasonBottom <= LoadoutTabModel.rowHeight(true), "the reason line runs into the next row: " + reasonBottom);
        assertTrue(LoadoutTabModel.REASON_LINE >= FONT, "the reason line overlaps the name");
    }

    @Test
    void thePreCatalogueFallbackNeverListsBiteForAHuman() {
        String src = SourceTree.read(CLIENT);
        int bite = src.indexOf("new LoadoutAccess.Entry(Loadout.BITE");
        int guard = src.indexOf("if (LoadoutRules.biteListed(net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.canBite(player)))");
        assertTrue(guard >= 0 && bite > guard && src.substring(guard, bite).indexOf('}') < 0, "the fallback lists Bite without asking whether the player can bite");
    }
}
