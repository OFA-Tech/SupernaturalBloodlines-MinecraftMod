package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import org.junit.jupiter.api.Test;

class BrotherhoodCardsTest {
    private static final String PAGES = "src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/";

    @Test
    void cardsAreForAnExistingBrotherhoodOnly() {
        for (GroupType type : GroupType.values()) {
            assertEquals(type == GroupType.BROTHERHOOD, BrotherhoodCards.applies(new GroupView(type, true, GroupView.MEMBER, "", 0, List.of())));
            assertFalse(BrotherhoodCards.applies(GroupView.none(type)));
        }
    }

    @Test
    void everyThemeUsesTheSameCards() {
        for (String page : List.of("PackPage.java", "BookPackPage.java")) {
            assertTrue(SourceTree.read(PAGES + page).contains("BrotherhoodCards.render(f, r, y, gv)"), page);
        }
        String book = SourceTree.read(PAGES + "BookPackPage.java");
        for (String style : List.of("BOOK", "MOON", "TENSURA")) {
            assertTrue(book.contains("Style." + style), style);
        }
    }

    @Test
    void theBrotherhoodPageHasNoButtonOfItsOwn() {
        String cards = SourceTree.read(PAGES + "BrotherhoodCards.java");
        assertFalse(cards.contains("runCommand") || cards.contains(".target(") || cards.contains("PageWidgets.described"));
    }
}
