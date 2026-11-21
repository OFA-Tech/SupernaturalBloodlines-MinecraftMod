package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static net.ofatech.supernaturalbloodlines.shared.hub.HubPage.*;
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
        assertEquals(List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, PACK, GUIDE, SETTINGS), visiblePages(ctx(WEREWOLF, true, true, false)));
    }

    @Test
    void vampireWithNoPackHasNoPackPage() {
        assertEquals(List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, GUIDE, SETTINGS), visiblePages(ctx(VAMPIRE, true, true, false)));
    }

    @Test
    void currentWolvesAndHybridTypesSeeThePackAndAnyoneInAPackDoes() {
        for (SupernaturalRace r : List.of(WEREWOLF, HYBRID, ORIGINAL_HYBRID, TRIBRID)) {
            assertTrue(PACK.visible(ctx(r, true, true, false)), r.id());
        }
        // a former werewolf with no pack (cured, or turned a vampire) has none; a witch Emissary or a Packmate who is in one does
        for (SupernaturalRace r : List.of(HUMAN, VAMPIRE, ORIGINAL, HERETIC, HUNTER, GHOUL)) {
            assertFalse(PACK.visible(ctx(r, true, true, true)), r.id());
            assertTrue(PACK.visible(ctx(r, true, true, true).withPack(true)), r.id() + " in a pack");
        }
    }

    @Test
    void withoutWerewolvesAWolfRaceWithNoPackHasNoPackPage() {
        for (SupernaturalRace r : List.of(WEREWOLF, HYBRID, ORIGINAL_HYBRID, TRIBRID)) {
            assertFalse(PACK.visible(ctx(r, false, true, true)), r.id());
        }
    }

    @Test
    void withoutEnchantedTheRitesAreGone() {
        assertEquals(List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, GUIDE, SETTINGS), visiblePages(ctx(VAMPIRE, false, false, false)));
    }

    @Test
    void theLoadoutIsNeverAPageOfItsOwnAndOpensSkills() {
        HubPage.Context withHud = new HubPage.Context(VAMPIRE, Optional.empty(), true, true, true);
        assertFalse(LOADOUT.visible(withHud), "the Loadout is a Skills tab now");
        assertFalse(visiblePages(withHud).contains(LOADOUT));
        assertEquals(SKILLS, orFirst(LOADOUT, withHud), "the old page opens Skills");
    }

    @Test
    void adminShowsOnlyToAnOperator() {
        assertFalse(ADMIN.visible(ctx(VAMPIRE, true, true, false)));
        assertFalse(visiblePages(ctx(VAMPIRE, true, true, false)).contains(ADMIN));
        assertTrue(ADMIN.visible(ctx(VAMPIRE, true, true, false).withOp(true)));
        assertEquals(ADMIN, visiblePages(ctx(VAMPIRE, true, true, false).withOp(true)).get(visiblePages(ctx(VAMPIRE, true, true, false).withOp(true)).size() - 1));
        assertEquals(HOME, orFirst(ADMIN, ctx(VAMPIRE, true, true, false)), "losing operator rights while the hub is open drops the page");
    }

    @Test
    void aGhoulKeepsHomePathGuideAndSettingsAndTheRestIsLocked() {
        HubPage.Context ghoul = new HubPage.Context(GHOUL, Optional.empty(), true, true, true);
        for (HubPage p : visiblePages(ghoul)) {
            assertEquals(!List.of(HOME, PATH, GUIDE, SETTINGS).contains(p), p.locked(ghoul), p.name());
        }
        assertTrue(BLOODLINE.locked(ghoul));
        assertTrue(SKILLS.locked(ghoul));
        assertFalse(PATH.locked(ctx(VAMPIRE, true, true, false)));
        assertFalse(QUESTS.locked(ctx(VAMPIRE, true, true, false)));
    }

    @Test
    void everyPageButHomeAndTheLoadoutAliasHasAGroup() {
        assertEquals(HubPage.Group.NONE, HOME.group());
        for (HubPage p : HubPage.values()) {
            if (p != HOME) {
                assertTrue(p.group() != HubPage.Group.NONE, p.name());
            }
        }
        assertEquals(List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, PACK, GUIDE, SETTINGS, ADMIN, LOADOUT), List.of(HubPage.values()));
    }

    @Test
    void aPageThatWentAwayFallsBackToHome() {
        assertEquals(HOME, orFirst(PACK, ctx(VAMPIRE, true, true, false)));
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
        assertEquals(GUIDE, HubFocus.page(pages, BLOODLINE, 1));
        assertEquals(SETTINGS, HubFocus.page(pages, GUIDE, 1));
        assertEquals(HOME, HubFocus.page(pages, SETTINGS, 1));
        assertEquals(SETTINGS, HubFocus.page(pages, HOME, -1));
    }

    @Test
    void aThemeNoPartRegisteredIsTheGrimoire() {
        assertEquals(ThemeId.GRIMOIRE, HubClientConfig.resolve(ThemeId.GRIMOIRE, id -> id.equals("grimoire")));
        assertEquals(ThemeId.GRIMOIRE, HubClientConfig.resolve(ThemeId.TENSURA, id -> id.equals("grimoire"))); // no Tensura, or 12f not in
        assertEquals(ThemeId.TENSURA, HubClientConfig.resolve(ThemeId.TENSURA, id -> true));
        assertEquals("blood_moon", ThemeId.BLOOD_MOON.id());
    }
}
