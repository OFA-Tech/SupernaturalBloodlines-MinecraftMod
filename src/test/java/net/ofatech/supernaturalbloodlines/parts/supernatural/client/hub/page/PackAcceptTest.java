package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import org.junit.jupiter.api.Test;

/** "Accept an invitation" on the pack page (plan 23): only with an invitation waiting, and never the main button of a leader. */
class PackAcceptTest {
    @Test
    void nothingToAcceptNoButton() {
        assertFalse(BookPackPage.mayAccept(false, false, GroupView.NOT_IN));
        assertFalse(BookPackPage.mayAccept(false, true, GroupView.LEADER));
    }

    @Test
    void aLeadersAcceptIsSecondary() {
        assertFalse(BookPackPage.acceptIsPrimary(true));
        assertTrue(BookPackPage.acceptIsPrimary(false));
        String src = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/BookPackPage.java");
        assertTrue(src.contains("d.action(\"accept\", t(KEY + \"accept\", \"accept\"), acceptIsPrimary(leads(f, pack)), true,"), "the Tensura detail draws Accept as the main button for a leader");
    }
}
