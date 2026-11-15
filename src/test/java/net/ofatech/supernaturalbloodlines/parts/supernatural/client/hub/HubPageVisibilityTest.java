package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClientConfig.ThemeId;
import org.junit.jupiter.api.Test;

class HubPageVisibilityTest {
    private static HubPage.Context ctx(SupernaturalRace creature, boolean werewolves, boolean enchanted, boolean unused) {
        return new HubPage.Context(creature, Optional.empty(), werewolves, enchanted);
    }

    @Test
    void allModsWerewolfSeesEveryPage() {
        assertEquals(List.of(PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK), visiblePages(ctx(WEREWOLF, true, true, false)));
    }

    @Test
    void vampireWithNoPackHasNoPackPage() {
        assertEquals(List.of(PATH, SKILLS, QUESTS, RITES, BLOODLINE), visiblePages(ctx(VAMPIRE, true, true, false)));
    }

    @Test
    void onlyCurrentWolvesAndHybridTypesSeeThePack() {
        for (SupernaturalRace r : List.of(WEREWOLF, HYBRID, ORIGINAL_HYBRID, TRIBRID)) {
            assertTrue(PACK.visible(ctx(r, true, true, false)), r.id());
        }
        // a former werewolf: cured (human) or turned (vampire), whatever pack state they kept
        for (SupernaturalRace r : List.of(HUMAN, VAMPIRE, ORIGINAL, HERETIC, HUNTER, GHOUL)) {
            assertFalse(PACK.visible(ctx(r, true, true, true)), r.id());
        }
    }

    @Test
    void withoutWerewolvesThePackIsGoneEvenForWolfRaces() {
        for (SupernaturalRace r : List.of(WEREWOLF, HYBRID, ORIGINAL_HYBRID, TRIBRID)) {
            assertFalse(PACK.visible(ctx(r, false, true, true)), r.id());
        }
    }

    @Test
    void withoutEnchantedTheRitesAreGone() {
        assertEquals(List.of(PATH, SKILLS, QUESTS, BLOODLINE), visiblePages(ctx(VAMPIRE, false, false, false)));
    }

    @Test
    void ghoulSeesPathAndBloodlineAndTheRestIsLocked() {
        HubPage.Context c = ctx(GHOUL, true, true, false);
        for (HubPage p : visiblePages(c)) {
            assertEquals(p != PATH && p != BLOODLINE, p.locked(c), p.name());
        }
        assertFalse(PATH.locked(ctx(VAMPIRE, true, true, false)));
        assertFalse(QUESTS.locked(ctx(VAMPIRE, true, true, false)));
    }

    @Test
    void aPageThatWentAwayFallsBackToPath() {
        assertEquals(PATH, orFirst(PACK, ctx(VAMPIRE, true, true, false)));
        assertEquals(QUESTS, orFirst(QUESTS, ctx(VAMPIRE, true, true, false)));
    }

    @Test
    void focusWrapsByIdAndStartsAtEitherEnd() {
        List<String> ids = List.of("tab:PATH", "task:a", "stamp:a", "task:b");
        assertEquals("tab:PATH", HubFocus.move(ids, null, 1));
        assertEquals("task:b", HubFocus.move(ids, null, -1));
        assertEquals("tab:PATH", HubFocus.move(ids, "task:b", 1));
        assertEquals("task:b", HubFocus.move(ids, "tab:PATH", -1));
        assertEquals(null, HubFocus.move(List.of(), "task:a", 1));
        // the focus follows its target when the list re-sorts, and starts over when the target went away
        assertEquals("task:a", HubFocus.move(List.of("task:b", "stamp:b", "task:a"), "stamp:b", 1));
        assertEquals("tab:PATH", HubFocus.move(ids, "gone", 1));
    }

    @Test
    void focusScrollsItsTargetIntoView() {
        Rect clip = new Rect(0, 100, 50, 100);
        assertEquals(0, HubFocus.scrollToShow(0, clip, new Rect(0, 120, 10, 10)));          // already shown
        assertEquals(30, HubFocus.scrollToShow(0, clip, new Rect(0, 220, 10, 10)));         // below: just enough
        assertEquals(10, HubFocus.scrollToShow(40, clip, new Rect(0, 70, 10, 10)));         // above: back up to it
        assertEquals(0, HubFocus.scrollToShow(5, clip, new Rect(0, 50, 10, 10)));           // never negative
        assertEquals(90, HubFocus.pageScroll(0, 200, clip, 1));
        assertEquals(0, HubFocus.pageScroll(50, 200, clip, -1));
        assertEquals(60, HubFocus.pageScroll(0, 60, clip, 1));
    }

    @Test
    void arrowsWalkTheVisiblePagesOnly() {
        List<HubPage> pages = visiblePages(ctx(VAMPIRE, false, false, false));
        assertEquals(BLOODLINE, HubFocus.page(pages, QUESTS, 1));
        assertEquals(PATH, HubFocus.page(pages, BLOODLINE, 1));
        assertEquals(BLOODLINE, HubFocus.page(pages, PATH, -1));
    }

    @Test
    void everyThemeFallsBackToTheGrimoireForNow() {
        assertEquals(ThemeId.GRIMOIRE, HubClientConfig.resolve(ThemeId.GRIMOIRE, true));
        assertEquals(ThemeId.GRIMOIRE, HubClientConfig.resolve(ThemeId.TENSURA, false));
        assertEquals(ThemeId.GRIMOIRE, HubClientConfig.resolve(ThemeId.BLOOD_MOON, false));
    }
}
