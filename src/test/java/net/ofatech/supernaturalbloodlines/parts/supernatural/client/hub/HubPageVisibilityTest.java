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
    private static HubPage.Context ctx(SupernaturalRace creature, boolean werewolves, boolean enchanted, boolean pack) {
        return new HubPage.Context(creature, Optional.empty(), werewolves, enchanted, pack);
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
    void anyRaceInAPackSeesThePack() {
        assertTrue(PACK.visible(ctx(HUMAN, true, true, true)));
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
    void focusWrapsAndStartsAtEitherEnd() {
        assertEquals(0, HubFocus.move(-1, 4, 1));
        assertEquals(3, HubFocus.move(-1, 4, -1));
        assertEquals(0, HubFocus.move(3, 4, 1));
        assertEquals(3, HubFocus.move(0, 4, -1));
        assertEquals(-1, HubFocus.move(2, 0, 1));
        assertEquals(0, HubFocus.move(9, 4, 1)); // a stale focus after the targets shrank
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
